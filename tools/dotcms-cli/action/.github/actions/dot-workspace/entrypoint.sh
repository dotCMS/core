#!/bin/sh

if [ ! "$CREATE_WORKSPACE" = "true" ]; then
     echo "Skipping workspace creation";
     exit 0;
fi

normalize() {
    in=$1
    normalized=$(echo "$in" | sed -E 's#/+#/#g')
    echo "$normalized"
}

BASE_PATH=$(normalize "$BASE_PATH")

WORKSPACE_FILE=$BASE_PATH/$DOT_WORKSPACE_YML
WORKSPACE_FILE=$(normalize "$WORKSPACE_FILE")
echo "Workspace file: $WORKSPACE_FILE"

workspace_file_content='name: default
version: 1.0.0
description: "DO NOT ERASE ME !!! I am a marker file required by dotCMS CLI."'

if [ ! -f "$WORKSPACE_FILE" ]; then
      echo "Creating workspace file: $WORKSPACE_FILE";
      echo "$workspace_file_content" >> "$WORKSPACE_FILE";
      cat "$WORKSPACE_FILE";
fi

FILES_PATH=$BASE_PATH/$FILES_NAME_SPACE
FILES_PATH=$(normalize "$FILES_PATH")

echo "Files path: $FILES_PATH"
if [ ! -f "$FILES_PATH" ]; then
      echo "Creating files path: $FILES_PATH";
      mkdir -p "$FILES_PATH";
fi

CONTENT_TYPES_PATH=$BASE_PATH/$CONTENT_TYPES_NAME_SPACE
CONTENT_TYPES_PATH=$(normalize "$CONTENT_TYPES_PATH")

echo "Content types path: $CONTENT_TYPES_PATH"
if [ ! -f "$CONTENT_TYPES_PATH" ]; then
      echo "Creating content types path: $CONTENT_TYPES_PATH";
      mkdir -p "$CONTENT_TYPES_PATH";
fi

LANGUAGE_PATH=$BASE_PATH/$LANGS_NAME_SPACE
LANGUAGE_PATH=$(normalize "$LANGS_PATH")
echo "Languages path: $LANGUAGE_PATH"
if [ ! -f "$LANGUAGE_PATH" ]; then
      echo "Creating languages path: $LANGUAGE_PATH";
      mkdir -p "$LANGUAGE_PATH";
fi

SITES_PATH=$BASE_PATH/$SITES_NAME_SPACE
SITES_PATH=$(normalize "$SITES_PATH")
echo "Sites path: $SITES_PATH"
if [ ! -f "$SITES_PATH" ]; then
      echo "Creating sites path: $SITES_PATH";
      mkdir -p "$SITES_PATH";
fi

ls -la "$BASE_PATH"