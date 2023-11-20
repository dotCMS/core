import { InjectionToken } from '@angular/core';

export enum POST_MESSAGE_ACTIONS {
    EDIT_CONTENTLET = 'edit-contentlet',
    EMA_RELOAD_PAGE = 'ema-reload-page',
    NOOP = 'noop'
}

export enum NG_CUSTOM_EVENTS {
    EDIT_CONTENTLET_LOADED = 'edit-contentlet-loaded'
}

export const WINDOW = new InjectionToken<Window>('WindowToken');
