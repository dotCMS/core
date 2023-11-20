import { InjectionToken } from '@angular/core';

export enum CUSTOMER_ACTIONS {
    EDIT_CONTENTLET = 'edit-contentlet', // The customer hit edit button
    NOOP = 'noop'
}

export enum NOTIFY_CUSTOMER {
    EMA_RELOAD_PAGE = 'ema-reload-page' // We need to reload the ema page
}

// All the custom events that come from the JSP Iframe
export enum NG_CUSTOM_EVENTS {
    EDIT_CONTENTLET_LOADED = 'edit-contentlet-loaded'
}

export const WINDOW = new InjectionToken<Window>('WindowToken');
