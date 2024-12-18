import { DotDeviceListItem } from '@dotcms/dotcms-models';

export const DEFAULT_DEVICES: DotDeviceListItem[] = [
    {
        icon: 'pi pi-desktop',
        identifier: 'default-id',
        name: 'DOTCMS_KEY_FOR_DEFAULT',
        cssHeight: '100',
        inode: 'default',
        cssWidth: '100'
    },
    {
        cssWidth: '820',
        name: 'DOTCMS_KEY_FOR_TABLET',
        icon: 'pi pi-tablet',
        cssHeight: '1180',
        inode: 'tablet',
        identifier: 'tablet-id'
    },
    {
        inode: 'mobile',
        icon: 'pi pi-mobile',
        name: 'DOTCMS_KEY_FOR_MOBILE',
        cssHeight: '844',
        identifier: 'mobile-id',
        cssWidth: '390'
    }
];
