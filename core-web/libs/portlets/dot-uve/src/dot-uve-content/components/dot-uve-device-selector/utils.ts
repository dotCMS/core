import { MenuItem } from 'primeng/api';

import { DotDeviceListItem } from '@dotcms/dotcms-models';

export const DEFAULT_DEVICE: DotDeviceListItem = {
    icon: 'pi pi-desktop',
    identifier: 'default-id',
    name: 'uve.device.selector.default',
    cssHeight: '100', // This will be used in %
    inode: 'desktop',
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

export const getSocialMediaSubmenu = (items: MenuItem[]) => ({
    id: 'search-engine',
    label: 'uve.preview.mode.search.engine.subheader',
    items
});

export const getSearchEngineSubmenu = (items: MenuItem[]) => ({
    id: 'social-media',
    label: 'uve.preview.mode.social.media.subheader',
    items
});

export const getDeviceSubmenu = (items: MenuItem[]) => ({
    id: 'device',
    label: 'uve.preview.mode.device.subheader',
    items
});
