import { ClientConfig, DotCmsClient } from './lib/client/sdk-js-client';
import { CLIENT_ACTIONS, postMessageToEditor } from './lib/editor/models/client.model';
import {
    CustomClientParams,
    DotCMSPageEditorConfig,
    EditorConfig
} from './lib/editor/models/editor.model';
import { NOTIFY_CLIENT } from './lib/editor/models/listeners.model';
import {
    destroyEditor,
    editContentlet,
    reorderMenu,
    initEditor,
    isInsideEditor,
    updateNavigation
} from './lib/editor/sdk-editor';
import { getPageRequestParams, graphqlToPageEntity } from './lib/utils';

export {
    graphqlToPageEntity,
    getPageRequestParams,
    isInsideEditor,
    editContentlet,
    reorderMenu,
    DotCmsClient,
    DotCMSPageEditorConfig,
    CLIENT_ACTIONS,
    NOTIFY_CLIENT,
    CustomClientParams,
    postMessageToEditor,
    EditorConfig,
    initEditor,
    updateNavigation,
    destroyEditor,
    ClientConfig
};
