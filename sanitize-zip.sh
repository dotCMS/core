#!/bin/sh
# Script to sanitize ZIP files by removing absolute paths
# Usage: sanitize-zip.sh input.zip output.zip

set -e

INPUT_FILE="$1"
OUTPUT_FILE="$2"

echo "==== SANITIZE ZIP DEBUG INFO ===="
echo "Current directory: $(pwd)"
echo "Input file path: $INPUT_FILE"
echo "Output file path: $OUTPUT_FILE"

# Make sure input file exists
if [ ! -f "$INPUT_FILE" ]; then
  echo "ERROR: Input file does not exist: $INPUT_FILE"
  exit 1
fi

# Create the output directory if it doesn't exist
OUTPUT_DIR=$(dirname "$OUTPUT_FILE")
mkdir -p "$OUTPUT_DIR"
echo "Created output directory: $OUTPUT_DIR"

# Get absolute paths
INPUT_FILE=$(realpath "$INPUT_FILE")
OUTPUT_DIR=$(realpath "$OUTPUT_DIR")
OUTPUT_FILE="$OUTPUT_DIR/$(basename "$OUTPUT_FILE")"

echo "Sanitizing ZIP file"
echo "Absolute input path:  $INPUT_FILE"
echo "Absolute output path: $OUTPUT_FILE"

# Create temp directory
TEMP_DIR=$(mktemp -d)
echo "Working in temp directory: $TEMP_DIR"

# Extract files to temp directory 
echo "Extracting ZIP file..."
unzip -q "$INPUT_FILE" -d "$TEMP_DIR" || true

# Create sanitized ZIP file
echo "Creating sanitized ZIP file..."
(cd "$TEMP_DIR" && zip -qr "$OUTPUT_FILE" .)

# Verify the output file exists
if [ ! -f "$OUTPUT_FILE" ]; then
  echo "ERROR: Failed to create output file: $OUTPUT_FILE"
  rm -rf "$TEMP_DIR"
  exit 1
fi

# Show some information about the created file
echo "Successfully created sanitized ZIP file:"
ls -la "$OUTPUT_FILE"

# Clean up
rm -rf "$TEMP_DIR"
echo "ZIP sanitization complete."
echo "==== END SANITIZE ZIP DEBUG INFO ====" 