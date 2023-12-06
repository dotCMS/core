import { InjectionToken } from '@angular/core';

export const EDIT_CONTENTLET_URL =
    '/c/portal/layout?p_p_id=content&p_p_action=1&p_p_state=maximized&p_p_mode=view&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_content_cmd=edit&inode=';

export const ADD_CONTENTLET_URL = `/html/ng-contentlet-selector.jsp?ng=true&container_id=*CONTAINER_ID*&add=*BASE_TYPES*`;

export const WINDOW = new InjectionToken<Window>('WindowToken');

export const DEFAULT_LANGUAGE_ID = 1;

export const DEFAULT_URL = 'index';
