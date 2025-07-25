# Heimdall Documentation Directory

This directory is monitored by Heimdall's file monitoring service for automatic memory updates.

## Usage

- Place documentation files directly in this directory
- Or create symlinks to your actual documentation:
  ```bash
  ln -s ../docs ./.heimdall/docs/project-docs
  ln -s ../README.md ./.heimdall/docs/README.md
  ```

## Supported Formats

- Markdown (.md, .markdown, .mdown, .mkd)
- Text files (.txt)
- More formats coming soon

## Monitoring

Files in this directory are automatically:
- Parsed and stored as cognitive memories
- Updated when modified
- Indexed for semantic search
- Connected to related concepts
