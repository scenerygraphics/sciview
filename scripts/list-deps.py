#!/usr/bin/env python3
"""List all dependencies from gradle-dep-tree JSON output (deduplicated)."""

import json
import sys
from pathlib import Path

def get_scopes(actual_configs):
    """
    Extract scopes, omitting redundant ones based on subset relationships.
    
    Since test-compile ⊃ compile and test-runtime ⊃ runtime,
    we only show the "minimal" scopes needed to describe availability.
    """
    scopes = []
    
    # Check what's present
    has_compile = "compileClasspath" in actual_configs
    has_runtime = "runtimeClasspath" in actual_configs
    has_test_compile = "testCompileClasspath" in actual_configs
    has_test_runtime = "testRuntimeClasspath" in actual_configs
    
    # Declaration types (api, implementation, etc) imply compile and runtime scopes
    is_api = "api" in actual_configs
    is_impl = "implementation" in actual_configs
    is_compile_only = "compileOnly" in actual_configs
    is_runtime_only = "runtimeOnly" in actual_configs
    
    # If declared as api or implementation, it's in compile and runtime scope
    if is_api or is_impl:
        has_compile = True
        has_runtime = True
    elif is_compile_only:
        has_compile = True
    elif is_runtime_only:
        has_runtime = True
    
    # If in test-compile but not compile, it's test-only for compilation
    if has_test_compile and not has_compile:
        scopes.append("test-compile")
    elif has_compile:
        scopes.append("compile")
    
    # If in test-runtime but not runtime, it's test-only for runtime
    if has_test_runtime and not has_runtime:
        scopes.append("test-runtime")
    elif has_runtime:
        scopes.append("runtime")
    
    return scopes

def get_configs_for_ga(ga, nodes, ga_to_resolved):
    """Get configs for a G:A.
    
    Tries to use the resolved version's configs. If the resolved version has no actual configs
    (only metadata or nothing), looks for another version with actual configs.
    """
    resolved_version = ga_to_resolved.get(ga)
    
    if resolved_version:
        resolved_key = f"{ga}:{resolved_version}"
        resolved_node = nodes.get(resolved_key, {})
        configs = resolved_node.get("configurations", [])
        
        # Filter to actual configs (non-metadata)
        actual_configs = [c for c in configs if not c.endswith("DependenciesMetadata")]
        if actual_configs:
            return actual_configs
    
    # Fallback: look for any other version with actual configs
    for key in nodes:
        if key.startswith(ga + ":"):
            configs = nodes[key].get("configurations", [])
            # Filter to actual configs (non-metadata)
            actual_configs = [c for c in configs if not c.endswith("DependenciesMetadata")]
            if actual_configs:
                # Prefer non-test-only configs if available
                has_compile = "compileClasspath" in actual_configs
                has_runtime = "runtimeClasspath" in actual_configs
                if has_compile or has_runtime:
                    return actual_configs
    
    # Final fallback: return any actual configs we found
    for key in nodes:
        if key.startswith(ga + ":"):
            configs = nodes[key].get("configurations", [])
            actual_configs = [c for c in configs if not c.endswith("DependenciesMetadata")]
            if actual_configs:
                return actual_configs
    
    return []

def main():
    filter_scopes = None
    json_file = None
    
    for arg in sys.argv[1:]:
        if arg.startswith("--scopes="):
            filter_scopes = set(arg.split("=", 1)[1].split(","))
        elif not arg.startswith("-"):
            json_file = arg
    
    if not json_file:
        print("Usage: list-deps.py [--scopes=scope1,scope2,...] <json-file>", file=sys.stderr)
        sys.exit(1)
    
    json_file = Path(json_file)
    if not json_file.exists():
        print(f"Error: File not found: {json_file}", file=sys.stderr)
        sys.exit(1)
    
    with open(json_file) as f:
        data = json.load(f)
    
    root = data["root"]
    nodes = data["nodes"]
    
    # Get only reachable nodes and determine resolved versions
    reachable = set()
    ga_to_resolved = {}
    ga_to_version_info = {}  # Track version info for better resolution
    
    def traverse(node_key):
        if node_key in reachable:
            return
        reachable.add(node_key)
        
        parts = node_key.split(":")
        if len(parts) >= 3:
            ga = ":".join(parts[:-1])
            version = parts[-1]
            
            if ga not in ga_to_version_info:
                ga_to_version_info[ga] = []
            
            configs = nodes.get(node_key, {}).get("configurations", [])
            actual_configs = [c for c in configs if not c.endswith("DependenciesMetadata")]
            has_compile = "compileClasspath" in actual_configs
            has_runtime = "runtimeClasspath" in actual_configs
            
            ga_to_version_info[ga].append({
                "version": version,
                "has_scope": has_compile or has_runtime,
                "configs": actual_configs
            })
            
            # Initially set resolved version (will be refined after traverse)
            if ga not in ga_to_resolved:
                ga_to_resolved[ga] = version
        
        node = nodes.get(node_key, {})
        for child in node.get("children", []):
            traverse(child)
    
    traverse(root)
    
    # Determine which versions are "active" (have actual scope configs, not just build/doc configs)
    # Some GAs may have multiple versions for different scopes (e.g., compile vs runtime)
    active_versions = set()
    for ga, versions in ga_to_version_info.items():
        for v in versions:
            if v["has_scope"]:  # Has compile or runtime scope
                active_versions.add(f"{ga}:{v['version']}")
    
    # Filter to only active versions (those with actual scope configs)
    resolved_deps = []
    for dep in reachable:
        if dep in active_versions:
            resolved_deps.append(dep)
    
    print(f"Dependencies for {root}")
    print("=" * (len(f"Dependencies for {root}") + 1))
    
    if filter_scopes:
        scope_list = ", ".join(sorted(filter_scopes))
        print(f"(filtered to: {scope_list})")
    print()
    
    for dep in sorted(resolved_deps):
        # Get configs directly from this specific version
        node = nodes.get(dep, {})
        configs = node.get("configurations", [])
        
        # Extract primary configs (exclude metadata variants)
        primary_configs = [c for c in configs if not c.endswith("DependenciesMetadata")]
        
        # Check if matches scope filter
        if filter_scopes is not None:
            scopes = []
            if primary_configs:
                actual_configs = [c for c in primary_configs if c in [
                    "compileClasspath", "runtimeClasspath", 
                    "testCompileClasspath", "testRuntimeClasspath",
                    "api", "implementation",
                    "compileOnly", "runtimeOnly",
                    "testImplementation", "testCompileOnly",
                    "testRuntimeOnly"
                ]]
                
                if actual_configs:
                    scopes = get_scopes(actual_configs)
            
            # Skip if no scopes match the filter
            if not any(s in filter_scopes for s in scopes):
                continue
        
        output = dep
        
        if primary_configs:
            # Filter to actual runtime/compile scopes
            actual_configs = [c for c in primary_configs if c in [
                "compileClasspath", "runtimeClasspath", 
                "testCompileClasspath", "testRuntimeClasspath",
                "api", "implementation",
                "compileOnly", "runtimeOnly",
                "testImplementation", "testCompileOnly",
                "testRuntimeOnly"
            ]]
            
            if actual_configs:
                # Identify declaration type
                declaration = None
                if "api" in actual_configs:
                    declaration = "api"
                elif "implementation" in actual_configs:
                    declaration = "impl"
                elif "compileOnly" in actual_configs:
                    declaration = "compile-only"
                elif "runtimeOnly" in actual_configs:
                    declaration = "runtime-only"
                
                # Get scopes
                scopes = get_scopes(actual_configs)
                
                # Format output
                parts = []
                if declaration:
                    parts.append(declaration)
                parts.extend(scopes)
                
                if parts:
                    output += f"  [{', '.join(parts)}]"
        
        print(output)

if __name__ == "__main__":
    main()
