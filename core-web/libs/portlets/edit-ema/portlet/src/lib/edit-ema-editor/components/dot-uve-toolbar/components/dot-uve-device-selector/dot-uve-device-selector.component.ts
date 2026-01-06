import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
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
import { Orientation } from '../../../../../store/models';

/**
 * State - what the user has selected (changes frequently)
 */
export interface DeviceSelectorState {
    currentDevice: DotDevice | null;
    currentSocialMedia: string | null;
    currentOrientation: Orientation | null;
}

/**
 * Change events - discriminated union for type-safe event handling
 */
export type DeviceSelectorChange =
    | { type: 'device'; device: DotDevice }
    | { type: 'socialMedia'; socialMedia: string }
    | { type: 'orientation'; orientation: Orientation };

@Component({
    selector: 'dot-uve-device-selector',
    imports: [ButtonModule, TooltipModule, DotMessagePipe, NgClass, MenuModule],
    templateUrl: './dot-uve-device-selector.component.html',
    styleUrl: './dot-uve-device-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveDeviceSelectorComponent {
    #messageService = inject(DotMessageService);

    // State input - what's currently selected
    state = input.required<DeviceSelectorState>();

    // Config inputs - available options and settings
    devices = input<DotDeviceListItem[]>([]);
    isTraditionalPage = input<boolean>(true);

    // Single output - unified state change event
    stateChange = output<DeviceSelectorChange>();

    readonly defaultDevices = DEFAULT_DEVICES;
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
        () => this.state().currentDevice?.inode === 'default' || this.state().currentSocialMedia !== null
    );

    readonly $menuItems = computed(() => {
        const isTraditionalPage = this.isTraditionalPage();
        const menu = [];

        const extraDevices = this.devices().filter((device) => !device._isDefault);

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

        const customDevice = this.devices().find(
            (device) => !device._isDefault && device.inode === this.state().currentDevice?.inode
        );

        const label = customDevice?.name || this.state().currentSocialMedia;

        return label || DEFAULT_LABEL;
    });

    readonly activeMenuItemId = computed(() => {
        const deviceInode = this.state().currentDevice?.inode;
        const socialMedia = this.state().currentSocialMedia;

        return socialMedia || deviceInode;
    });

    readonly $isMoreButtonActive = computed(() => !this.state().currentDevice?._isDefault);

    /**
     * Select a social media
     * Emits unified state change event to parent container
     *
     * @param {string} socialMedia
     * @memberof DotUveDeviceSelectorComponent
     */
    onSocialMediaSelect(socialMedia: string): void {
        const isSameSocialMedia = this.state().currentSocialMedia === socialMedia;

        if (isSameSocialMedia) {
            // Emit default device to clear social media
            this.stateChange.emit({ type: 'device', device: DEFAULT_DEVICE });

            return;
        }

        // Emit social media selection
        this.stateChange.emit({ type: 'socialMedia', socialMedia });
    }

    /**
     * Select a device
     * Emits unified state change event to parent container
     *
     * @param {DotDevice} device
     * @memberof DotUveDeviceSelectorComponent
     */
    onDeviceSelect(device: DotDevice): void {
        const currentDevice = this.state().currentDevice;
        const isSameDevice = currentDevice?.inode === device.inode;

        // Emit device selection (or default to clear)
        this.stateChange.emit({
            type: 'device',
            device: isSameDevice ? DEFAULT_DEVICE : device
        });
    }

    /**
     * Toggle orientation
     * Emits unified state change event to parent container
     *
     * @memberof DotUveDeviceSelectorComponent
     */
    onOrientationChange(): void {
        const newOrientation =
            this.state().currentOrientation === Orientation.LANDSCAPE
                ? Orientation.PORTRAIT
                : Orientation.LANDSCAPE;

        // Emit orientation change
        this.stateChange.emit({ type: 'orientation', orientation: newOrientation });
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
