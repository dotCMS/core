#!/usr/bin/env python3
"""Fetch workflow metadata with caching."""

import sys
import json
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / "utils"))
from github_api import get_run_metadata


def main():
    if len(sys.argv) < 3:
        print("Usage: python fetch-metadata.py <RUN_ID> <WORKSPACE>", file=sys.stderr)
        sys.exit(1)
    
    run_id = sys.argv[1]
    workspace = Path(sys.argv[2])
    
    if not workspace:
        print("ERROR: WORKSPACE parameter is required", file=sys.stderr)
        sys.exit(1)
    
    metadata_file = workspace / "run-metadata.json"
    
    # Fetch metadata if not cached
    if not metadata_file.exists():
        print("Fetching run metadata...")
        get_run_metadata(run_id, metadata_file)
        print(f"✓ Metadata saved to {metadata_file}")
    else:
        print(f"✓ Using cached metadata: {metadata_file}")
    
    # Display metadata
    metadata = json.loads(metadata_file.read_text(encoding='utf-8'))
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()


