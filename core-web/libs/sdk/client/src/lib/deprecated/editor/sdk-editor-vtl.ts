import {
    listenEditorMessages,
    listenHoveredContentlet,
    preserveScrollOnIframe,
    scrollHandler
} from './listeners/listeners';
import { addClassToEmptyContentlets, initDotUVE, isInsideEditor } from './sdk-editor';
import { listenBlockEditorInlineEvent } from './utils/traditional-vtl.utils';

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
    initDotUVE();
    listenEditorMessages();
    scrollHandler();
    preserveScrollOnIframe();
    listenHoveredContentlet();
    addClassToEmptyContentlets();
    listenBlockEditorInlineEvent();
}
