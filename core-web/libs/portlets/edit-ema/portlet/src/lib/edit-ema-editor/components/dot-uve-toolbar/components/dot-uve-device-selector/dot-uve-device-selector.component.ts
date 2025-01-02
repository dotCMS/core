import { DeepSignal } from '@ngrx/signals';

import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';

import {
    DotMessageService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService
} from '@dotcms/data-access';
import {
    DotDevice,
    DotDeviceListItem,
    SEO_MEDIA_TYPES,
    socialMediaTiles
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_DEVICE, DEFAULT_DEVICES } from '../../../../../shared/consts';
import { UVEStore } from '../../../../../store/dot-uve.store';
import { Orientation } from '../../../../../store/models';

@Component({
    selector: 'dot-uve-device-selector',
    standalone: true,
    imports: [ButtonModule, TooltipModule, DotMessagePipe, NgClass, MenuModule],
    templateUrl: './dot-uve-device-selector.component.html',
    styleUrl: './dot-uve-device-selector.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveDeviceSelectorComponent implements OnInit {
    defaultDevices = DEFAULT_DEVICES;

    #store = inject(UVEStore);
    #messageService = inject(DotMessageService);
    private readonly dotSeoMetaTagsService = inject(DotSeoMetaTagsService);
    private readonly dotSeoMetaTagsUtilService = inject(DotSeoMetaTagsUtilService);

    $devices = input<DotDeviceListItem[]>([], {
        alias: 'devices'
    });

    readonly $currentDevice = this.#store.device as DeepSignal<DotDeviceListItem>;

    readonly $currentSocialMedia = this.#store.socialMedia;

    readonly $disableOrientation = computed(
        () => this.#store.device()?.inode === 'default' || this.#store.socialMedia()
    );

    readonly $currentOrientation = this.#store.orientation;

    readonly $menuItems = computed(() => {
        const extraDevices = this.$devices().filter((device) => !device._isDefault);

        const customDevices = {
            label: 'Custom Devices',
            items: extraDevices.map((device) => ({
                label: `${this.#messageService.get(device.name)} (${device.cssWidth}x${device.cssHeight})`,
                command: () => this.onDeviceSelect(device),
                styleClass: this.$currentDevice()?.inode === device.inode ? 'active' : ''
            }))
        };

        const socialMediaMenu = {
            label: 'Social Media Tiles',
            items: Object.values(socialMediaTiles)
                .filter(
                    (item) =>
                        item.value === SEO_MEDIA_TYPES.FACEBOOK ||
                        item.value === SEO_MEDIA_TYPES.TWITTER ||
                        item.value === SEO_MEDIA_TYPES.LINKEDIN
                )
                .map((item) => ({
                    label: item.label,
                    command: () => this.onSocialMediaSelect(item.value),
                    styleClass: this.$currentSocialMedia() === item.value ? 'active' : ''
                }))
        };

        const searchEngineMenu = {
            label: 'Search Engine',
            items: Object.values(socialMediaTiles)
                .filter((item) => item.value === SEO_MEDIA_TYPES.GOOGLE)
                .map((item) => ({
                    label: item.label,
                    command: () => this.onSocialMediaSelect(item.value),
                    styleClass: this.$currentSocialMedia() === item.value ? 'active' : ''
                }))
        };

        const menu = [];
        if (this.#store.isTraditionalPage()) {
            menu.push(socialMediaMenu);
            menu.push(searchEngineMenu);
        }

        if (extraDevices.length) {
            menu.push(customDevices);
        }

        return menu;
    });

    readonly $isADefaultDeviceActive = computed(() => {
        return !!this.$currentDevice()?._isDefault;
    });

    ngOnInit(): void {
        const { device: deviceInode, orientation, seo: socialMedia } = this.#store.viewParams();
        const device = this.$devices().find((d) => d.inode === deviceInode);

        this.loadOGTags();

        if (!socialMedia) {
            this.#store.setDevice(device || DEFAULT_DEVICE, orientation);

            return;
        }

        this.#store.setDevice(DEFAULT_DEVICE);
        this.#store.setSEO(socialMedia);
    }

    onSocialMediaSelect(socialMedia: string): void {
        const isSameSocialMedia = this.$currentSocialMedia() === socialMedia;

        this.#store.setSEO(isSameSocialMedia ? null : socialMedia);
    }

    onDeviceSelect(device: DotDevice): void {
        if (this.#store.socialMedia()) {
            this.#store.setSEO(null);
            this.#store.reloadCurrentPage();
        }

        const currentDevice = this.$currentDevice();
        const isSameDevice = currentDevice?.inode === device.inode;
        this.#store.setDevice(isSameDevice ? DEFAULT_DEVICE : device);
    }

    onOrientationChange(): void {
        this.#store.setOrientation(
            this.$currentOrientation() === Orientation.LANDSCAPE
                ? Orientation.PORTRAIT
                : Orientation.LANDSCAPE
        );
    }

    loadOGTags() {
        const parser = new DOMParser();
        const content = this.#store.pageAPIResponse()?.page.rendered;
        const doc = parser.parseFromString(content, 'text/html');
        const ogTags = this.dotSeoMetaTagsUtilService.getMetaTags(doc);

        this.dotSeoMetaTagsService.getMetaTagsResults(doc).subscribe((results) => {
            this.#store.setOgTags(ogTags);
            this.#store.setOGTagResults(results);
        });
    }
}
