import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { DotDevicesService, DotMessageService } from '@dotcms/data-access';
import {
    DotDevice,
    DotDeviceListItem,
    SEARCH_ENGINE_TILES,
    SOCIAL_MEDIA_TILES,
    SocialMediaOption
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../store/dot-uve.store';

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

@Component({
    selector: 'dot-uve-device-selector',
    providers: [DotDevicesService],
    imports: [ButtonModule, MenuModule, DotMessagePipe, NgClass],
    templateUrl: './dot-uve-device-selector.component.html',
    styleUrl: './dot-uve-device-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUVEDeviceSelectorComponent {
    #store = inject(UVEStore);
    #messageService = inject(DotMessageService);
    #deviceService = inject(DotDevicesService);

    readonly systemDevices = DEFAULT_DEVICES;
    readonly $currentDeviceInode = this.#store.configuration.device;
    readonly $userDevice = signal<DotDeviceListItem[]>([]);
    readonly $allowRotation = signal<boolean>(false);

    ngOnInit(): void {
        this.#deviceService.get().subscribe((devices) => {
            this.$userDevice.set(devices);
        });
        // const { device: deviceInode, orientation, seo: socialMedia } = this.#store.viewParams();
        // const device = this.$devices().find((d) => d.inode === deviceInode);
        // if (!socialMedia) {
        //     this.#store.setDevice(device || DEFAULT_DEVICE, orientation);
        //     return;
        // }
        // this.#store.setSEO(socialMedia);
    }

    /**
     * Select a social media
     *
     * @param {string} socialMedia
     * @memberof DotUveDeviceSelectorComponent
     */
    onSocialMediaSelect(_socialMedia: string): void {
        /** */
    }

    /**
     * Select a device
     *
     * @param {DotDevice} device
     * @memberof DotUveDeviceSelectorComponent
     */
    onDeviceSelect(_device: DotDevice): void {
        /** */
    }

    /**
     * Toggle orientation
     *
     * @memberof DotUveDeviceSelectorComponent
     */
    onOrientationChange(): void {
        /** */
    }

    readonly $menuItems = computed<MenuItem[]>(() => {
        const SOCIAL_MEDIA_MENU: MenuItem = {
            label: this.#messageService.get('uve.preview.mode.social.media.subheader'),
            id: 'social-media',
            items: this.#getSocialMediaMenuItems(SOCIAL_MEDIA_TILES)
        };

        const SEARCH_ENGINE_MENU: MenuItem = {
            label: this.#messageService.get('uve.preview.mode.search.engine.subheader'),
            id: 'search-engine',
            items: this.#getSocialMediaMenuItems(SEARCH_ENGINE_TILES)
        };
        const menuItems = [SOCIAL_MEDIA_MENU, SEARCH_ENGINE_MENU];

        if (this.$userDevice().length) {
            const DEVICE_MENU: MenuItem = {
                label: this.#messageService.get('uve.preview.mode.device.subheader'),
                id: 'device',
                items: this.#getDeviceMenuItems(this.$userDevice())
            };

            menuItems.unshift(DEVICE_MENU);
        }

        return menuItems;
    });

    /**
     * Get the menu items for social media
     *
     * @param {Record<string, SocialMediaOption>} options
     * @return {*}  {MenuItem[]}
     * @memberof DotUveDeviceSelectorComponent
     */
    #getSocialMediaMenuItems(options: Record<string, SocialMediaOption>) {
        return Object.values(options).map((item) => ({
            label: item.label,
            id: item.value,
            value: item.value,
            command: () => {
                this.onSocialMediaSelect(item.value);
            }
        }));
    }

    /**
     * Get the menu items for devices
     *
     * @param {DotDeviceListItem[]} devices
     * @return {*}  {MenuItem[]}
     * @memberof DotUveDeviceSelectorComponent
     */
    #getDeviceMenuItems(userDevices: DotDeviceListItem[]) {
        return userDevices.map((item) => ({
            label: item.name,
            id: item.inode,
            value: item.inode,
            command: () => this.onDeviceSelect(item)
        }));
    }
}
