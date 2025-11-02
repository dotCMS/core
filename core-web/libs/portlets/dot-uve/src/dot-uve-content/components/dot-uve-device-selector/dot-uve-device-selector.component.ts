import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { DotDevicesService } from '@dotcms/data-access';
import {
    DotDevice,
    SEARCH_ENGINE_TILES,
    SOCIAL_MEDIA_TILES,
    SocialMediaOption
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_DEVICES } from './utils';

import { UVEStore } from '../../../store/dot-uve.store';

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
    #deviceService = inject(DotDevicesService);

    readonly systemDevices = DEFAULT_DEVICES;
    readonly $currentDeviceInode = this.#store.configuration.device;
    readonly $menuItems = signal<MenuItem[]>([]);
    readonly $allowRotation = signal<boolean>(false);

    readonly #socialMediaItems = this.#parseTilesToMenuItems(SOCIAL_MEDIA_TILES);
    readonly #searchEngineItems = this.#parseTilesToMenuItems(SEARCH_ENGINE_TILES);

    ngOnInit(): void {
        this.#deviceService.get().subscribe((devices) => {
            this.$menuItems.set(this.#getMenuItems(devices));
        });
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

    #getMenuItems(userDevices: DotDevice[]): MenuItem[] {
        const menuItems: MenuItem[] = [
            {
                id: 'search-engine',
                label: 'uve.preview.mode.search.engine.subheader',
                items: this.#socialMediaItems
            },
            {
                id: 'social-media',
                label: 'uve.preview.mode.social.media.subheader',
                items: this.#searchEngineItems
            }
        ];

        if (userDevices.length) {
            menuItems.unshift({
                id: 'device',
                label: 'uve.preview.mode.device.subheader',
                items: this.#getDeviceMenuItems(userDevices)
            });
        }

        return menuItems;
    }

    /**
     * Get the menu items for devices
     *
     * @param {DotDeviceListItem[]} devices
     * @return {*}  {MenuItem[]}
     * @memberof DotUveDeviceSelectorComponent
     */
    #getDeviceMenuItems(userDevices: DotDevice[]): MenuItem[] {
        return userDevices.map((item) => ({
            label: item.name,
            id: item.inode,
            value: item.inode,
            command: () => this.onDeviceSelect(item)
        }));
    }

    /**
     * Get the menu items for social media
     *
     * @param {Record<string, SocialMediaOption>} options
     * @return {*}  {MenuItem[]}
     * @memberof DotUveDeviceSelectorComponent
     */
    #parseTilesToMenuItems(options: Record<string, SocialMediaOption>): MenuItem[] {
        return Object.values(options).map((item) => ({
            label: item.label,
            id: item.value,
            value: item.value,
            command: () => this.onSocialMediaSelect(item.value)
        }));
    }
}
