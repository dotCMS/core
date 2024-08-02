import { InjectionToken } from '@angular/core';

import { DotPersona } from '@dotcms/dotcms-models';

import { CommonErrors } from './enums';
import { CommonErrorsInfo } from './models';

export const LAYOUT_URL = '/c/portal/layout';

export const CONTENTLET_SELECTOR_URL = `/html/ng-contentlet-selector.jsp`;

export const HOST = 'http://localhost:3000';

export const WINDOW = new InjectionToken<Window>('WindowToken');

export const EDIT_CONTENT_CALLBACK_FUNCTION = 'saveAssignCallBackAngular';

export const VIEW_CONTENT_CALLBACK_FUNCTION = 'angularWorkflowEventCallback';

export const IFRAME_SCROLL_ZONE = 100;

export const BASE_IFRAME_MEASURE_UNIT = 'px';

export const COMMON_ERRORS: CommonErrorsInfo = {
    [CommonErrors.NOT_FOUND]: {
        icon: 'compass',
        title: 'editema.infopage.notfound.title',
        description: 'editema.infopage.notfound.description',
        buttonPath: '/pages',
        buttonText: 'editema.infopage.button.gotopages'
    },
    [CommonErrors.ACCESS_DENIED]: {
        icon: 'ban',
        title: 'editema.infopage.accessdenied.title',
        description: 'editema.infopage.accessdenied.description',
        buttonPath: '/pages',
        buttonText: 'editema.infopage.button.gotopages'
    }
};

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
