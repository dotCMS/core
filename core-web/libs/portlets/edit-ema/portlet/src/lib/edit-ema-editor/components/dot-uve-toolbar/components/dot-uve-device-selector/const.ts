import { DotDeviceListItem } from '@dotcms/dotcms-models';

export const DEFAULT_DEVICES: DotDeviceListItem[] = [
    {
        inode: 'mobile',
        icon: 'pi pi-mobile',
        name: 'DOTCMS_KEY_FOR_MOBILE',
        cssHeight: '844',
        identifier: 'mobile-id',
        cssWidth: '390'
    },
    {
        icon: 'pi pi-desktop',
        identifier: '4k-monitor-id',
        name: 'DOTCMS_KEY_FOR_4K_MONITOR',
        cssHeight: '2160',
        inode: '4k-monitor',
        cssWidth: '3840'
    },
    {
        cssWidth: '820',
        name: 'DOTCMS_KEY_FOR_TABLET',
        icon: 'pi pi-tablet',
        cssHeight: '1180',
        inode: 'tablet',
        identifier: 'tablet-id'
    }
];
