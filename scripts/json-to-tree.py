#!/usr/bin/env python3
"""Convert gradle-dep-tree JSON output to plaintext tree format."""

import json
import sys
from pathlib import Path
from collections import defaultdict

def get_reachable_nodes(root, nodes):
    """Find all nodes reachable from root, selecting only one version per GA."""
    reachable = set()
    ga_to_resolved = {}  # Maps group:artifact -> resolved version
    
    def traverse(node_key):
        if node_key in reachable:
            return
        reachable.add(node_key)
        
        # Track which version of each GA is used
        parts = node_key.split(":")
        if len(parts) >= 3:
            ga = ":".join(parts[:-1])
            version = parts[-1]
            if ga not in ga_to_resolved:
                ga_to_resolved[ga] = version
        
        node = nodes.get(node_key, {})
        for child in node.get("children", []):
            traverse(child)
    
    traverse(root)
    return reachable, ga_to_resolved

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

def print_tree(node_key, nodes, reachable, ga_to_resolved, seen, indent=0, filter_scopes=None):
    """Recursively print dependency tree, marking repeated nodes with (*)."""
    # Skip if this version isn't reachable or isn't the resolved one
    if node_key not in reachable:
        return
    
    parts = node_key.split(":")
    if len(parts) >= 3:
        ga = ":".join(parts[:-1])
        version = parts[-1]
        if ga in ga_to_resolved and ga_to_resolved[ga] != version:
            return
    
    # Get scope information
    node = nodes.get(node_key, {})
    configs = node.get("configurations", [])
    
    # Extract primary configs (exclude metadata variants)
    primary_configs = [c for c in configs if not c.endswith("DependenciesMetadata")]
    
    # Check if this dep matches the scope filter
    has_matching_scope = True
    if primary_configs and filter_scopes is not None:
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
            has_matching_scope = any(s in filter_scopes for s in scopes)
    
    if not has_matching_scope:
        return
    
    # Check if already visited - if so, mark with (*) and show scopes
    if node_key in seen:
        output = "  " * indent + node_key + " (*)"
        
        # Get scope information for repeated deps too
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
                scope_parts = []
                if declaration:
                    scope_parts.append(declaration)
                scope_parts.extend(scopes)
                
                if scope_parts:
                    output += f"  [{', '.join(scope_parts)}]"
        
        print(output)
        return
    
    seen.add(node_key)
    
    output = "  " * indent + node_key
    
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
            scope_parts = []
            if declaration:
                scope_parts.append(declaration)
            scope_parts.extend(scopes)
            
            if scope_parts:
                output += f"  [{', '.join(scope_parts)}]"
    
    print(output)
    
    children = node.get("children", [])
    for child in children:
        print_tree(child, nodes, reachable, ga_to_resolved, seen, indent + 1, filter_scopes, single_scope)

def main():
    filter_scopes = None
    single_scope = False
    json_file = None
    
    for arg in sys.argv[1:]:
        if arg.startswith("--scopes="):
            filter_scopes = set(arg.split("=", 1)[1].split(","))
            single_scope = len(filter_scopes) == 1
        elif not arg.startswith("-"):
            json_file = arg
    
    if not json_file:
        print("Usage: json-to-tree.py [--scopes=scope1,scope2,...] <json-file>", file=sys.stderr)
        sys.exit(1)
    
    json_file = Path(json_file)
    if not json_file.exists():
        print(f"Error: File not found: {json_file}", file=sys.stderr)
        sys.exit(1)
    
    with open(json_file) as f:
        data = json.load(f)
    
    root = data["root"]
    nodes = data["nodes"]
    
    # Find reachable nodes and resolved versions
    reachable, ga_to_resolved = get_reachable_nodes(root, nodes)
    
    print(f"Dependency Tree for {root}")
    print("=" * (len(f"Dependency Tree for {root}") + 1))
    
    if filter_scopes:
        scope_list = ", ".join(sorted(filter_scopes))
        print(f"(filtered to: {scope_list})")
    print()
    
    seen = set()
    print_tree(root, nodes, reachable, ga_to_resolved, seen, filter_scopes=filter_scopes)

if __name__ == "__main__":
    main()
