#!/usr/bin/env python3
"""Convert gradle-dep-tree JSON to pruned tree with configuration (scope) info."""

import json
import sys
from pathlib import Path
from collections import deque

def get_scopes(actual_configs):
    """Extract scopes, omitting redundant ones based on subset relationships."""
    scopes = []
    
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
    
    if has_test_compile and not has_compile:
        scopes.append("test-compile")
    elif has_compile:
        scopes.append("compile")
    
    if has_test_runtime and not has_runtime:
        scopes.append("test-runtime")
    elif has_runtime:
        scopes.append("runtime")
    
    return scopes

def get_min_depths(root, nodes, reachable, active_versions, filter_scopes=None):
    """
    BFS to find the minimum depth at which each G:A:V appears.
    Also filter to only nodes that match the scope filter.
    """
    min_depth = {}
    queue = deque([(root, 0)])
    visited = set()
    
    while queue:
        node_key, depth = queue.popleft()
        
        # Skip if not reachable
        if node_key not in reachable:
            continue
        
        # Skip if not an active version
        if node_key not in active_versions:
            continue
        
        # Get configs for this node
        node = nodes.get(node_key, {})
        configs = node.get("configurations", [])
        primary_configs = [c for c in configs if not c.endswith("DependenciesMetadata")]
        
        should_include = True
        if primary_configs:
            actual_configs = [c for c in primary_configs if c in [
                "compileClasspath", "runtimeClasspath", 
                "testCompileClasspath", "testRuntimeClasspath",
                "api", "implementation",
                "compileOnly", "runtimeOnly",
                "testImplementation", "testCompileOnly",
                "testRuntimeOnly"
            ]]
            
            if not actual_configs:
                should_include = False  # No actual scopes - skip this node
            
            # If filtering, also check if scopes match
            elif filter_scopes is not None:
                scopes = get_scopes(actual_configs)
                if not any(s in filter_scopes for s in scopes):
                    should_include = False  # Doesn't match filter
        else:
            should_include = False  # No configs
        
        # Add to tree if it should be shown
        if should_include:
            if node_key not in min_depth:
                min_depth[node_key] = depth
        
        # Only recurse into children if parent is included
        # (pruned tree: if parent is filtered out, entire subtree is filtered out)
        if should_include and node_key not in visited:
            visited.add(node_key)
            
            children = node.get("children", [])
            for child in children:
                queue.append((child, depth + 1))
    
    return min_depth

def print_pruned_tree(node_key, nodes, reachable, active_versions, min_depth, depth, indent=0, show_scopes=False):
    """DFS to print tree, showing each G:A:V only at its minimum depth."""
    
    # Skip if not reachable or not an active version
    if node_key not in reachable or node_key not in active_versions:
        return
    
    # Only print if we're at the minimum depth for this node
    if node_key not in min_depth or min_depth[node_key] != depth:
        return
    
    # Build output with scopes if requested
    output = "  " * indent + node_key
    
    if show_scopes:
        node = nodes.get(node_key, {})
        configs = node.get("configurations", [])
        primary_configs = [c for c in configs if not c.endswith("DependenciesMetadata")]
        
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
                # Get declaration type
                declaration = None
                if "api" in actual_configs:
                    declaration = "api"
                elif "implementation" in actual_configs:
                    declaration = "impl"
                elif "compileOnly" in actual_configs:
                    declaration = "compile-only"
                elif "runtimeOnly" in actual_configs:
                    declaration = "runtime-only"
                
                scopes = get_scopes(actual_configs)
                
                # Format output
                parts = []
                if declaration:
                    parts.append(declaration)
                parts.extend(scopes)
                
                if parts:
                    output += f"  [{', '.join(parts)}]"
    
    print(output)
    
    # Recurse into children
    node = nodes.get(node_key, {})
    children = node.get("children", [])
    for child in children:
        print_pruned_tree(child, nodes, reachable, active_versions, min_depth, depth + 1, 
                         indent + 1, show_scopes=show_scopes)

def get_reachable_nodes(root, nodes):
    """Find all nodes reachable from root."""
    reachable = set()
    active_versions = set()  # Versions with actual scope configs
    
    def traverse(node_key):
        if node_key in reachable:
            return
        reachable.add(node_key)
        
        configs = nodes.get(node_key, {}).get("configurations", [])
        actual_configs = [c for c in configs if not c.endswith("DependenciesMetadata")]
        has_compile = "compileClasspath" in actual_configs
        has_runtime = "runtimeClasspath" in actual_configs
        
        # Mark as active if it has actual scope configs
        if has_compile or has_runtime:
            active_versions.add(node_key)
        
        node = nodes.get(node_key, {})
        for child in node.get("children", []):
            traverse(child)
    
    traverse(root)
    
    return reachable, active_versions

def main():
    show_all_scopes = False
    filter_scopes = None
    json_file = None
    
    for arg in sys.argv[1:]:
        if arg == "--all-scopes":
            show_all_scopes = True
        elif arg.startswith("--scopes="):
            filter_scopes = set(arg.split("=", 1)[1].split(","))
        elif not arg.startswith("-"):
            json_file = arg
    
    if not json_file:
        print("Usage: pruned-tree.py [--all-scopes|--scopes=scope1,scope2,...] <json-file>", file=sys.stderr)
        sys.exit(1)
    
    json_file = Path(json_file)
    if not json_file.exists():
        print(f"Error: File not found: {json_file}", file=sys.stderr)
        sys.exit(1)
    
    with open(json_file) as f:
        data = json.load(f)
    
    root = data["root"]
    nodes = data["nodes"]
    
    # Find reachable nodes and active versions
    reachable, active_versions = get_reachable_nodes(root, nodes)
    
    # Two-pass calculation: first find min depths, then print
    scopes_to_filter = filter_scopes if filter_scopes or not show_all_scopes else None
    min_depth = get_min_depths(root, nodes, reachable, active_versions, scopes_to_filter)
    
    print(f"Pruned Dependency Tree for {root}")
    print("=" * (len(f"Pruned Dependency Tree for {root}") + 1))
    
    if show_all_scopes:
        print("(showing all scopes: compile, runtime, test-compile, test-runtime)")
    elif filter_scopes:
        scope_list = ", ".join(sorted(filter_scopes))
        print(f"(filtered to: {scope_list})")
    print()
    
    print_pruned_tree(root, nodes, reachable, active_versions, min_depth, 0, 
                     show_scopes=show_all_scopes or filter_scopes is not None)

if __name__ == "__main__":
    main()
