import { createDotCMSClient } from './lib/client/client';
import { CLIENT_ACTIONS, postMessageToEditor } from './lib/deprecated/editor/models/client.model';
import {
    CustomClientParams,
    DotCMSPageEditorConfig,
    EditorConfig
} from './lib/deprecated/editor/models/editor.model';
import {
    InlineEditorData,
    INLINE_EDITING_EVENT_KEY,
    InlineEditEventData
} from './lib/deprecated/editor/models/inline-event.model';
import { NOTIFY_CLIENT } from './lib/deprecated/editor/models/listeners.model';
import {
    destroyEditor,
    editContentlet,
    reorderMenu,
    initEditor,
    isInsideEditor,
    updateNavigation,
    initInlineEditing
} from './lib/deprecated/editor/sdk-editor';
import { ClientConfig, DotCmsClient } from './lib/deprecated/sdk-js-client';
import { getPageRequestParams, graphqlToPageEntity } from './lib/utils';

export {
    // Functions
    destroyEditor,
    editContentlet,
    getPageRequestParams,
    graphqlToPageEntity,
    initEditor,
    initInlineEditing,
    isInsideEditor,
    postMessageToEditor,
    reorderMenu,
    updateNavigation,

    // Classes
    DotCmsClient,

    // Interfaces & Types
    ClientConfig,
    CustomClientParams,
    DotCMSPageEditorConfig,
    EditorConfig,
    InlineEditEventData,
    InlineEditorData,

    // Constants
    CLIENT_ACTIONS,
    INLINE_EDITING_EVENT_KEY,
    NOTIFY_CLIENT,
    createDotCMSClient
};
