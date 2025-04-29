#!/bin/bash

# This script validates GitHub workflow files for common issues

WORKFLOW_DIR=".github/workflows"
ERRORS_FOUND=0

echo "Validating GitHub workflows..."

# Check for references to workflows that don't exist
for file in "$WORKFLOW_DIR"/*.yml "$WORKFLOW_DIR"/*.yaml; do
  if [ -f "$file" ]; then
    # Look for 'uses: ./.github/workflows/' lines
    while IFS= read -r line; do
      if [[ "$line" =~ uses:\ \.\/\.github\/workflows\/([a-zA-Z0-9_-]+\.(yml|yaml)) ]]; then
        referenced_workflow="${BASH_REMATCH[1]}"
        if [ ! -f "$WORKFLOW_DIR/$referenced_workflow" ]; then
          echo "ERROR: $file references non-existent workflow: $referenced_workflow"
          ERRORS_FOUND=1
        fi
      fi
    done < "$file"
  fi
done

# Check for references to actions that don't exist
for file in "$WORKFLOW_DIR"/*.yml "$WORKFLOW_DIR"/*.yaml; do
  if [ -f "$file" ]; then
    # Look for 'uses: ./.github/actions/' lines
    while IFS= read -r line; do
      if [[ "$line" =~ uses:\ \.\/\.github\/actions\/([a-zA-Z0-9_/-]+) ]]; then
        referenced_action="${BASH_REMATCH[1]}"
        # Handle both composite actions and Docker actions
        if [ ! -f ".github/actions/$referenced_action/action.yml" ] && [ ! -f ".github/actions/$referenced_action/action.yaml" ]; then
          echo "ERROR: $file references non-existent action: $referenced_action"
          ERRORS_FOUND=1
        fi
      fi
    done < "$file"
  fi
done

echo "Validation complete."

if [ $ERRORS_FOUND -ne 0 ]; then
  echo "Errors were found in the workflow files."
  exit 1
else
  echo "All workflows validated successfully."
  exit 0
fi 