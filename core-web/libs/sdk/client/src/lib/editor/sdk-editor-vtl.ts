import {
    listenEditorMessages,
    listenHoveredContentlet,
    preserveScrollOnIframe,
    scrollHandler
} from './listeners/listeners';
import { DotSDK } from './models/client.model';
import { isInsideEditor, addClassToEmptyContentlets, initDotSDK } from './sdk-editor';

declare global {
    interface Window {
        dotSDK: DotSDK;
    }
}

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
    initDotSDK();
    listenEditorMessages();
    scrollHandler();
    preserveScrollOnIframe();
    listenHoveredContentlet();
    addClassToEmptyContentlets();
}
