import { InjectionToken } from '@angular/core';

iimport { UVE_MODE } from '@dotcms/uve';
import { DotDeviceListItem, DotPersona } from '@dotcms/dotcms-models';

import { CommonErrors, PAGE_MODE } from './enums';
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

// Add the Feature flags we want to fetch for UVE
export const UVE_FEATURE_FLAGS = [];

export const DEFAULT_DEVICE: DotDeviceListItem = {
    icon: 'pi pi-desktop',
    identifier: 'default-id',
    name: 'uve.device.selector.default',
    cssHeight: '100', // This will be used in %
    inode: 'default',
    cssWidth: '100', // This will be used in %
    _isDefault: true
};

export const DEFAULT_DEVICES: DotDeviceListItem[] = [
    DEFAULT_DEVICE,
    {
        cssWidth: '820',
        name: 'uve.device.selector.tablet',
        icon: 'pi pi-tablet',
        cssHeight: '1180',
        inode: 'tablet',
        identifier: 'tablet-id',
        _isDefault: true
    },
    {
        inode: 'mobile',
        icon: 'pi pi-mobile',
        name: 'uve.device.selector.mobile',
        cssHeight: '844',
        identifier: 'mobile-id',
        cssWidth: '390',
        _isDefault: true
    }
];

export const UVE_MODE_TO_PAGE_MODE: Record<UVE_MODE, PAGE_MODE> = {
    [UVE_MODE.PREVIEW]: PAGE_MODE.PREVIEW,
    [UVE_MODE.LIVE]: PAGE_MODE.LIVE,
    [UVE_MODE.EDIT]: PAGE_MODE.EDIT,
    [UVE_MODE.UNKNOWN]: PAGE_MODE.PREVIEW // Not possible in the current architecture, but required for the type
};
