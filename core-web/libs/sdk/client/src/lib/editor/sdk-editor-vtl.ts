import {
    listenContentChange,
    listenEditorMessages,
    listenHoveredContentlet,
    pingEditor
} from './listeners/listeners';
import { isInsideEditor } from './sdk-editor';

/**
 * This is the main entry point for the SDK VTL.
 * This is added to VTL Script in the EditPage
 *
 * @remarks
 * This module sets up the necessary listeners and functionality for the SDK VTL.
 * It checks if the script is running inside the editor and then initializes the client by pinging the editor,
 * listening for editor messages, hovered contentlet changes, and content changes.
 *
 */
if (isInsideEditor()) {
    pingEditor();
    listenEditorMessages();
    listenHoveredContentlet();
    listenContentChange();
}
