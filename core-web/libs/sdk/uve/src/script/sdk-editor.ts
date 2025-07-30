import { UVE_MODE } from '@dotcms/types';

import {
    addClassToEmptyContentlets,
    listenBlockEditorInlineEvent,
    registerUVEEvents,
    scrollHandler,
    setClientIsReady
} from './utils';

import { createUVESubscription, getUVEState } from '../lib/core/core.utils';
import { editContentlet, reorderMenu, updateNavigation } from '../lib/editor/public';

declare global {
    interface Window {
        dotUVE: unknown;
    }
}

const dotUVE = {
    createSubscription: createUVESubscription,
    editContentlet,
    reorderMenu,
    updateNavigation
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
