import { ClientConfig, DotCmsClient } from './lib/client/sdk-js-client';
import { CUSTOMER_ACTIONS, postMessageToEditor } from './lib/editor/models/client.model';
import {
    CustomClientParams,
    DotCMSPageEditorConfig,
    EditorConfig
} from './lib/editor/models/editor.model';
import {
    destroyEditor,
    initEditor,
    isInsideEditor,
    updateNavigation
} from './lib/editor/sdk-editor';
import { getPageRequestParams, graphqlToPageEntity } from './lib/utils';

export {
    graphqlToPageEntity,
    getPageRequestParams,
    isInsideEditor,
    DotCmsClient,
    DotCMSPageEditorConfig,
    CUSTOMER_ACTIONS,
    CustomClientParams,
    postMessageToEditor,
    EditorConfig,
    initEditor,
    updateNavigation,
    destroyEditor,
    ClientConfig
};
