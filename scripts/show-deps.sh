#!/bin/bash
# Helper script to display dependency tree using gradle-dep-tree plugin

OUTPUT_FILE="/tmp/sciview-deps.json"
OUTPUT_DIR="build/gradle-dep-tree"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FORMAT=""
SCOPES=""

# Parse arguments
for arg in "$@"; do
    if [[ "$arg" == --scopes=* ]]; then
        SCOPES="${arg#--scopes=}"
    elif [[ "$arg" != --* ]]; then
        FORMAT="$arg"
    fi
done

# Show help if no format specified
if [ -z "$FORMAT" ]; then
    cat << 'EOF'
Usage: show-dep-tree.sh <format> [--scopes=scope1,scope2,...]

FORMATS:
  pruned        - Maven-style tree (each G:A:V shown once at first occurrence)
  fulltree      - Full tree with all branches, marks repeated deps with (*)
  list          - Flat deduplicated list of all resolved dependencies
  json          - Raw JSON format for programmatic use

SCOPE FILTERING:
  --scopes=compile,runtime,test-compile,test-runtime
    Filter to show only dependencies in specified scopes.
    Available scopes: compile, runtime, test-compile, test-runtime
    Works with all non-JSON formats: pruned, fulltree, list

ANNOTATION FORMAT [declaration, scope1, scope2, ...]:
  Examples:
    [api, compile, runtime]      - Available in main code (compile & runtime)
    [impl, test-compile]         - Test-only compilation dependency
    [compile, test-runtime]      - Main compile + test runtime (edge case)
  
  Scope notes:
    - "compile" = in compileClasspath (implies also in testCompileClasspath)
    - "runtime" = in runtimeClasspath (implies also in testRuntimeClasspath)
    - "test-compile" = only in testCompileClasspath (not in main compile)
    - "test-runtime" = only in testRuntimeClasspath (not in main runtime)
  
  You can filter by scope only (compile/runtime/test-compile/test-runtime), 
  not by declaration type.

NOTE: All output formats include scope annotations.
      You can filter them with sed if you don't want them:
      ./show-dep-tree.sh list | sed 's/  \[.*\]//g'

EXAMPLES:
  # Pruned tree with all scopes
  ./show-dep-tree.sh pruned

  # Pruned tree, only compile scope (no scope brackets for single scope)
  ./show-dep-tree.sh pruned --scopes=compile

  # Pruned tree, compile and runtime only (main code)
  ./show-dep-tree.sh pruned --scopes=compile,runtime

  # Flat list of all dependencies
  ./show-dep-tree.sh list

  # List only main compile dependencies
  ./show-dep-tree.sh list --scopes=compile

  # List test-only dependencies
  ./show-dep-tree.sh list --scopes=test-compile,test-runtime

  # Full tree with all scopes
  ./show-dep-tree.sh fulltree

  # Full tree, only main runtime scope
  ./show-dep-tree.sh fulltree --scopes=runtime

  # JSON output
  ./show-dep-tree.sh json
EOF
    exit 0
fi

echo "Generating dependency tree..."
./gradlew generateDepTrees -I "$SCRIPT_DIR/gradle-dep-tree-init.gradle.kts" -q -Dcom.jfrog.depsTreeOutputFile="$OUTPUT_FILE" 2>&1 | grep -v "^Creating Maven"

# The plugin outputs a path, so read that path
if [ -f "$OUTPUT_FILE" ]; then
    TREE_FILE=$(cat "$OUTPUT_FILE" 2>/dev/null || echo "")
else
    TREE_FILE=$(ls -t "$OUTPUT_DIR"/* 2>/dev/null | head -1)
fi

if [ -z "$TREE_FILE" ] && [ -f "$OUTPUT_DIR/"* ]; then
    TREE_FILE=$(ls -t "$OUTPUT_DIR"/* 2>/dev/null | head -1)
fi

if [ -f "$TREE_FILE" ]; then
    echo ""
    case "$FORMAT" in
        json)
            echo "Dependency tree (JSON format):"
            echo "==============================="
            python3 -m json.tool "$TREE_FILE" 2>/dev/null || cat "$TREE_FILE"
            ;;
        fulltree)
            if [ -n "$SCOPES" ]; then
                "$SCRIPT_DIR/json-to-tree.py" --scopes="$SCOPES" "$TREE_FILE"
            else
                "$SCRIPT_DIR/json-to-tree.py" "$TREE_FILE"
            fi
            ;;
        pruned)
            if [ -n "$SCOPES" ]; then
                "$SCRIPT_DIR/pruned-tree.py" --scopes="$SCOPES" "$TREE_FILE"
            else
                "$SCRIPT_DIR/pruned-tree.py" --all-scopes "$TREE_FILE"
            fi
            ;;
        list|flat)
            if [ -n "$SCOPES" ]; then
                "$SCRIPT_DIR/list-deps.py" --scopes="$SCOPES" "$TREE_FILE"
            else
                "$SCRIPT_DIR/list-deps.py" "$TREE_FILE"
            fi
            ;;
        *)
            echo "Unknown format: $FORMAT"
            echo "Run 'show-dep-tree.sh' with no arguments for help"
            exit 1
            ;;
    esac
else
    echo "Error: Could not find dependency tree file at $TREE_FILE"
    exit 1
fi
