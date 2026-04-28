import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';

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

import { DeviceSelectorChange, DeviceSelectorState } from './dot-uve-device-selector.models';

import { DEFAULT_DEVICE } from '../../../../../shared/consts';

@Component({
    selector: 'dot-uve-device-selector',
    imports: [ButtonModule, TooltipModule, DotMessagePipe, MenuModule],
    templateUrl: './dot-uve-device-selector.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex items-center gap-1 p-0'
    }
})
export class DotUveDeviceSelectorComponent {
    #messageService = inject(DotMessageService);

    $state = input.required<DeviceSelectorState>({ alias: 'state' });

    $devices = input<DotDeviceListItem[]>([], { alias: 'devices' });
    $isTraditionalPage = input<boolean>(true, { alias: 'isTraditionalPage' });

    stateChange = output<DeviceSelectorChange>();

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

    readonly $menuItems = computed(() => {
        const isTraditionalPage = this.$isTraditionalPage();
        const menu = [];

        const extraDevices = this.$devices().filter((device) => !device._isDefault);

        if (extraDevices.length) {
            menu.push({
                label: this.#messageService.get('uve.preview.mode.device.subheader'),
                id: 'custom-devices',
                items: this.#getDeviceMenuItems(extraDevices)
            });
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
            (device) => !device._isDefault && device.inode === this.$state().device?.inode
        );

        const label = customDevice?.name || this.$state().socialMedia;

        return label || DEFAULT_LABEL;
    });

    readonly activeMenuItemId = computed(() => {
        const deviceInode = this.$state().device?.inode;
        const socialMedia = this.$state().socialMedia;

        return socialMedia || deviceInode;
    });

    readonly $isMoreButtonActive = computed(() => !this.$state().device?._isDefault);

    onSocialMediaSelect(socialMedia: string): void {
        const isSameSocialMedia = this.$state().socialMedia === socialMedia;

        if (isSameSocialMedia) {
            this.stateChange.emit({ type: 'device', device: DEFAULT_DEVICE });

            return;
        }

        this.stateChange.emit({ type: 'socialMedia', socialMedia });
    }

    onDeviceSelect(device: DotDevice): void {
        const isSameDevice = this.$state().device?.inode === device.inode;

        this.stateChange.emit({
            type: 'device',
            device: isSameDevice ? DEFAULT_DEVICE : device
        });
    }

    onMoreMenuItemClick(event: MouseEvent, item: MenuItem, menu: Menu): void {
        if (item.disabled || item.separator) return;

        event.preventDefault();
        event.stopPropagation();

        item.command?.({ originalEvent: event, item });
        menu.hide();
    }

    #getSocialMediaMenuItems(options: Record<string, SocialMediaOption>): MenuItem[] {
        return Object.values(options).map((item) => ({
            label: item.label,
            id: item.value,
            value: item.value,
            command: () => this.onSocialMediaSelect(item.value)
        }));
    }

    #getDeviceMenuItems(devices: DotDeviceListItem[]): MenuItem[] {
        return devices.map((device) => ({
            label: `${this.#messageService.get(device.name)} (${device.cssWidth}x${device.cssHeight})`,
            id: device.inode,
            command: () => this.onDeviceSelect(device)
        }));
    }
}
