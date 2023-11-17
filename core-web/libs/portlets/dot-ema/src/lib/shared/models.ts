import { InjectionToken } from '@angular/core';

export enum MESSAGE_ACTIONS {
    EDIT_CONTENTLET = 'edit-contentlet'
}

export enum CUSTOM_EVENTS {
    EDIT_CONTENTLET_LOADED = 'edit-contentlet-loaded'
}

export const WINDOW = new InjectionToken<Window>('WindowToken');
