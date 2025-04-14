import {
    addClassToEmptyContentlets,
    listenBlockEditorInlineEvent,
    registerUVEEvents,
    scrollHandler,
    setClientIsReady
} from './utils';

import { createUVESubscription, getUVEState } from '../lib/core/core.utils';
import {
    editContentlet,
    initInlineEditing,
    initUVE,
    reorderMenu,
    sendMessageToUVE
} from '../lib/editor/public';
import { UVE_MODE } from '../lib/types/editor/public';

declare global {
    interface Window {
        dotUVE: unknown;
    }
}

const dotUVE = {
    createUVESubscription,
    editContentlet,
    getUVEState,
    initInlineEditing,
    initUVE,
    reorderMenu,
    sendMessageToUVE
};

window.dotUVE = dotUVE;

const uveState = getUVEState();

if (uveState?.mode === UVE_MODE.EDIT) {
    registerUVEEvents();
    scrollHandler();
    addClassToEmptyContentlets();
    setClientIsReady();
    listenBlockEditorInlineEvent();
}
