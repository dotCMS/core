import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotDevice,
    DotDeviceListItem,
    SEARCH_ENGINE_TILES,
    SocialMediaOption,
    SOCIAL_MEDIA_TILES
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_DEVICE, DEFAULT_DEVICES } from '../../../../../shared/consts';
import { UVEStore } from '../../../../../store/dot-uve.store';
import { Orientation } from '../../../../../store/models';

@Component({
    selector: 'dot-uve-device-selector',
    imports: [ButtonModule, TooltipModule, DotMessagePipe, NgClass, MenuModule],
    templateUrl: './dot-uve-device-selector.component.html',
    styleUrl: './dot-uve-device-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex items-center gap-1 p-0'
    }
})
export class DotUveDeviceSelectorComponent implements OnInit {
    #store = inject(UVEStore);
    #messageService = inject(DotMessageService);
    $devices = input<DotDeviceListItem[]>([], {
        alias: 'devices'
    });

    readonly defaultDevices = DEFAULT_DEVICES;
    readonly $currentDevice = this.#store.device;
    readonly $currentSocialMedia = this.#store.socialMedia;
    readonly $currentOrientation = this.#store.orientation;
    readonly socialMediaMenu = {
        label: this.#messageService.get('uve.preview.mode.social.media.subheader'),
        id: 'social-media',
        items: this.#getSocialMediaMenuItems(SOCIAL_MEDIA_TILES)
    };
    readonly searchEngineMenu = {
        label: this.#messageService.get('uve.preview.mode.search.engine.subheader'),
        id: 'search-engine',
        items: this.#getSocialMediaMenuItems(SEARCH_ENGINE_TILES)
    };
    readonly $disableOrientation = computed(
        () => this.#store.device()?.inode === 'default' || this.#store.socialMedia()
    );

    readonly $menuItems = computed(() => {
        const isTraditionalPage = this.#store.isTraditionalPage();
        const menu = [];

        const extraDevices = this.$devices().filter((device) => !device._isDefault);

        if (extraDevices.length) {
            const customDevices = {
                label: this.#messageService.get('uve.preview.mode.device.subheader'),
                id: 'custom-devices',
                items: this.#getDeviceMenuItems(extraDevices)
            };

            menu.push(customDevices);
        }

        if (isTraditionalPage) {
            menu.push(this.socialMediaMenu);
            menu.push(this.searchEngineMenu);
        }

        return menu;
    });

    readonly $moreButtonLabel = computed(() => {
        const DEFAULT_LABEL = 'more';

        const customDevice = this.$devices().find(
            (device) => !device._isDefault && device.inode === this.$currentDevice()?.inode
        );

        const label = customDevice?.name || this.$currentSocialMedia();

        return label || DEFAULT_LABEL;
    });

    readonly activeMenuItemId = computed(() => {
        const deviceInode = this.$currentDevice()?.inode;
        const socialMedia = this.$currentSocialMedia();

        return socialMedia || deviceInode;
    });

    readonly $isMoreButtonActive = computed(() => !this.$currentDevice()?._isDefault);

    ngOnInit(): void {
        const { device: deviceInode, orientation, seo: socialMedia } = this.#store.viewParams();
        const device = this.$devices().find((d) => d.inode === deviceInode);

        if (!socialMedia) {
            this.#store.setDevice(device || DEFAULT_DEVICE, orientation);

            return;
        }

        this.#store.setSEO(socialMedia);
    }

    /**
     * Select a social media
     *
     * @param {string} socialMedia
     * @memberof DotUveDeviceSelectorComponent
     */
    onSocialMediaSelect(socialMedia: string): void {
        const isSameSocialMedia = this.$currentSocialMedia() === socialMedia;

        if (isSameSocialMedia) {
            this.#store.setDevice(DEFAULT_DEVICE);

            return;
        }

        this.#store.setSEO(socialMedia);
    }

    /**
     * Select a device
     *
     * @param {DotDevice} device
     * @memberof DotUveDeviceSelectorComponent
     */
    onDeviceSelect(device: DotDevice): void {
        const currentDevice = this.$currentDevice();
        const isSameDevice = currentDevice?.inode === device.inode;
        this.#store.setDevice(isSameDevice ? DEFAULT_DEVICE : device);
    }

    /**
     * Toggle orientation
     *
     * @memberof DotUveDeviceSelectorComponent
     */
    onOrientationChange(): void {
        this.#store.setOrientation(
            this.$currentOrientation() === Orientation.LANDSCAPE
                ? Orientation.PORTRAIT
                : Orientation.LANDSCAPE
        );
    }

    /**
     * Menu items are actions (MenuItem.command), not navigations, so we render them as buttons.
     * We also close the menu after executing the command.
     */
    onMoreMenuItemClick(event: MouseEvent, item: MenuItem, menu: Menu): void {
        if (item.disabled || item.separator) return;

        event.preventDefault();
        event.stopPropagation();

        item.command?.({ originalEvent: event, item });
        menu.hide();
    }

    /**
     * Get the menu items for social media
     *
     * @param {Record<string, SocialMediaOption>} options
     * @return {*}  {MenuItem[]}
     * @memberof DotUveDeviceSelectorComponent
     */
    #getSocialMediaMenuItems(options: Record<string, SocialMediaOption>): MenuItem[] {
        return Object.values(options).map((item) => ({
            label: item.label,
            id: item.value,
            value: item.value,
            command: () => this.onSocialMediaSelect(item.value)
        }));
    }

    /**
     * Get the menu items for devices
     *
     * @param {DotDeviceListItem[]} devices
     * @return {*}  {MenuItem[]}
     * @memberof DotUveDeviceSelectorComponent
     */
    #getDeviceMenuItems(devices: DotDeviceListItem[]): MenuItem[] {
        return devices.map((device) => ({
            label: `${this.#messageService.get(device.name)} (${device.cssWidth}x${device.cssHeight})`,
            id: device.inode,
            command: () => this.onDeviceSelect(device)
        }));
    }
}
