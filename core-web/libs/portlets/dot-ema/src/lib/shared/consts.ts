import { InjectionToken } from '@angular/core';

import { DotPersona } from '@dotcms/dotcms-models';

export const EDIT_CONTENTLET_URL =
    '/c/portal/layout?p_p_id=content&p_p_action=1&p_p_state=maximized&p_p_mode=view&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_content_cmd=edit&inode=';

export const ADD_CONTENTLET_URL = `/html/ng-contentlet-selector.jsp?ng=true&container_id=*CONTAINER_ID*&add=*BASE_TYPES*`;

export const WINDOW = new InjectionToken<Window>('WindowToken');

export const DEFAULT_LANGUAGE_ID = 1;

export const DEFAULT_URL = 'index';

export const DEFAULT_PERSONA_ID = 'modes.persona.no.persona';

export const DEFAULT_PERSONA: DotPersona = {
    hostFolder: 'SYSTEM_HOST',
    inode: '',
    host: 'SYSTEM_HOST',
    locked: false,
    stInode: 'c938b15f-bcb6-49ef-8651-14d455a97045',
    contentType: 'persona',
    identifier: 'modes.persona.no.persona',
    folder: 'SYSTEM_FOLDER',
    hasTitleImage: false,
    owner: 'SYSTEM_USER',
    url: 'demo.dotcms.com',
    sortOrder: 0,
    name: 'Default Visitor',
    hostName: 'System Host',
    modDate: '0',
    title: 'Default Visitor',
    personalized: false,
    baseType: 'PERSONA',
    archived: false,
    working: false,
    live: false,
    keyTag: 'dot:persona',
    languageId: 1,
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    modUserName: 'system user system user',
    hasLiveVersion: false,
    modUser: 'system'
};
