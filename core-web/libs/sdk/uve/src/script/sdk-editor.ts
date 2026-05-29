import { UVE_MODE } from '@dotcms/types';

import {
    addClassToEmptyContentlets,
    injectEmptyStateStyles,
    listenBlockEditorInlineEvent,
    registerUVEEvents,
    scrollHandler,
    setClientIsReady
} from './utils';

import { createUVESubscription, getUVEState } from '../lib/core/core.utils';
import {
    createContentlet,
    editContentlet,
    reorderMenu,
    updateNavigation
} from '../lib/editor/public';
import { registerStyleEditorSchemas } from '../lib/style-editor/internal';

declare global {
    interface Window {
        dotUVE: unknown;
    }
}

const dotUVE = {
    createSubscription: createUVESubscription,
    createContentlet,
    editContentlet,
    reorderMenu,
    updateNavigation,
    registerStyleEditorSchemas
};

window.dotUVE = dotUVE;

const uveState = getUVEState();

if (uveState?.mode === UVE_MODE.EDIT) {
    registerUVEEvents();
    scrollHandler();
    addClassToEmptyContentlets();
    setClientIsReady();
    listenBlockEditorInlineEvent();
    injectEmptyStateStyles();
}
