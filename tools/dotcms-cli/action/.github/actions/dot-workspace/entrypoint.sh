#!/bin/sh
workspace_updated=false

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

echo "Base path: $BASE_PATH"
ls -la "$BASE_PATH"

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
      workspace_updated=true
fi

## We need an empty file to create the folders cause git does not track empty folders
placeholder_file_content='#placeholder file
'

FILES_PATH=$BASE_PATH/$FILES_NAME_SPACE
FILES_PATH=$(normalize "$FILES_PATH")

echo "Files path: $FILES_PATH"
if [ ! -d "$FILES_PATH" ]; then
      echo "Creating files path: $FILES_PATH";
      mkdir -p "$FILES_PATH";

      WORKING_EN=$(normalize "$FILES_PATH"/working/en-us/ )
      echo "Working en-us path: $WORKING_EN";
      mkdir -p "$WORKING_EN";
      echo "$placeholder_file_content" >> "$WORKING_EN".placeholder

      LIVE_EN=$(normalize "$FILES_PATH"/live/en-us/ )
      echo "Live en-us path: $LIVE_EN";
      mkdir -p "$LIVE_EN";
      echo "$placeholder_file_content" >> "$LIVE_EN".placeholder

      workspace_updated=true
fi

CONTENT_TYPES_PATH=$BASE_PATH/$CONTENT_TYPES_NAME_SPACE
CONTENT_TYPES_PATH=$(normalize "$CONTENT_TYPES_PATH")

echo "Content types path: $CONTENT_TYPES_PATH"
if [ ! -d "$CONTENT_TYPES_PATH" ]; then
      echo "Creating content types path: $CONTENT_TYPES_PATH";
      mkdir -p "$CONTENT_TYPES_PATH";
      echo "$placeholder_file_content" >> "$CONTENT_TYPES_PATH".placeholder
      workspace_updated=true
fi

LANGUAGE_PATH=$BASE_PATH/$LANGS_NAME_SPACE
LANGUAGE_PATH=$(normalize "$LANGUAGE_PATH")
echo "Languages path: $LANGUAGE_PATH"
if [ ! -d "$LANGUAGE_PATH" ]; then
      echo "Creating languages path: $LANGUAGE_PATH";
      mkdir -p "$LANGUAGE_PATH";
      echo "$placeholder_file_content" >> "$LANGUAGE_PATH".placeholder
      workspace_updated=true
fi

SITES_PATH=$BASE_PATH/$SITES_NAME_SPACE
SITES_PATH=$(normalize "$SITES_PATH")
echo "Sites path: $SITES_PATH"
if [ ! -d "$SITES_PATH" ]; then
      echo "Creating sites path: $SITES_PATH";
      mkdir -p "$SITES_PATH";
      echo "$placeholder_file_content" >> "$SITES_PATH".placeholder
      workspace_updated=true
fi

ls -la "$BASE_PATH"

echo "workspace-updated=$workspace_updated" >> "$GITHUB_OUTPUT"
