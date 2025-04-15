import {
    addClassToEmptyContentlets,
    listenBlockEditorInlineEvent,
    registerUVEEvents,
    scrollHandler,
    setClientIsReady
} from './utils';

import * as publicUVEExports from '../index';
import { getUVEState } from '../lib/core/core.utils';
import { UVE_MODE } from '../lib/types/editor/public';

declare global {
    interface Window {
        dotUVE: unknown;
    }
}

const dotUVE = {
    ...publicUVEExports
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
