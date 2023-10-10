import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';
import { PanelModule } from 'primeng/panel';

import { filter, map, mergeMap, take, toArray } from 'rxjs/operators';

import { DotCurrentUserService, DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { DotCurrentUser, DotDevice, DotDeviceListItem } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import {
    SEO_MEDIA_TYPES,
    SocialMediaOption,
    socialMediaTiles
} from '../../../content/services/dot-edit-content-html/models/meta-tags-model';

@Component({
    standalone: true,
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        DotPipesModule,
        ButtonModule,
        OverlayPanelModule,
        PanelModule,
        DividerModule,
        DotMessagePipe,
        RouterLink
    ],
    providers: [DotDevicesService],
    selector: 'dot-device-selector-seo',
    templateUrl: './dot-device-selector-seo.component.html',
    styleUrls: ['./dot-device-selector-seo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotDeviceSelectorSeoComponent implements OnInit {
    @Input() value: DotDevice;
    @Output() selected = new EventEmitter<DotDevice>();
    @Output() changeSeoMedia = new EventEmitter<string>();
    @Output() hideOverlayPanel = new EventEmitter<string>();
    @ViewChild('deviceSelector') overlayPanel: OverlayPanel;
    previewUrl: string;

    protected linkToAddDevice = '/c/content';
    protected linkToEditDeviceQueryParams = {
        devices: null
    };

    options$: Observable<DotDevice[]>;
    isCMSAdmin$: Observable<boolean>;
    socialMediaTiles: SocialMediaOption[];
    defaultOptions: DotDeviceListItem[] = [
        {
            name: this.dotMessageService.get('editpage.device.selector.mobile.portrait'),
            icon: 'pi pi-mobile',
            cssHeight: '844',
            cssWidth: '390',
            inode: '0',
            identifier: ''
        },
        {
            name: this.dotMessageService.get('editpage.device.selector.mobile.landscape'),
            icon: 'pi pi-mobile',
            cssHeight: '390',
            cssWidth: '844',
            inode: '0',
            identifier: ''
        },
        {
            name: this.dotMessageService.get('editpage.device.selector.hd.monitor'),
            icon: 'pi pi-desktop',
            cssHeight: '1080',
            cssWidth: '1920',
            inode: '0',
            identifier: ''
        },
        {
            name: this.dotMessageService.get('editpage.device.selector.4k.monitor'),
            icon: 'pi pi-desktop',
            cssHeight: '2160',
            cssWidth: '3840',
            inode: '0',
            identifier: ''
        },
        {
            name: this.dotMessageService.get('editpage.device.selector.tablet.portrait'),
            icon: 'pi pi-tablet',
            cssHeight: '1180',
            cssWidth: '820',
            inode: '0',
            identifier: ''
        },
        {
            name: this.dotMessageService.get('editpage.device.selector.tablet.landscape'),
            icon: 'pi pi-tablet',
            cssHeight: '820',
            cssWidth: '1180',
            inode: '0',
            identifier: ''
        }
    ];

    constructor(
        private dotDevicesService: DotDevicesService,
        private dotMessageService: DotMessageService,
        private dotCurrentUser: DotCurrentUserService
    ) {}

    ngOnInit() {
        this.options$ = this.getOptions();
        this.isCMSAdmin$ = this.checkIfCMSAdmin();
        this.socialMediaTiles = Object.values(socialMediaTiles).filter(
            (item) =>
                item.value === SEO_MEDIA_TYPES.FACEBOOK ||
                item.value === SEO_MEDIA_TYPES.TWITTER ||
                item.value === SEO_MEDIA_TYPES.LINKEDIN
        );
    }

    /**
     * Emit selected changes
     * @param DotDevice device
     */
    change(device: DotDevice) {
        this.selected.emit(device);
        this.overlayPanel.hide();
    }

    /**
     * Emit selected changes
     * @param DotDevice device
     */
    changeSeoMediaEvent(tile: string) {
        this.changeSeoMedia.emit(tile);
    }

    /**
     * Opens the device selector menu
     * @param event
     */
    openMenu(event: Event, target?: HTMLElement) {
        this.overlayPanel.toggle(event, target);
    }

    /**
     * Load the options for the select
     *
     * @returns Observable<DotDevice[]>
     */
    getOptions(): Observable<DotDevice[]> {
        return this.dotDevicesService.get().pipe(
            take(1),
            mergeMap((devices: DotDevice[]) => {
                this.linkToEditDeviceQueryParams.devices = devices[0]?.stInode;

                return devices;
            }),
            filter((device: DotDevice) => +device.cssHeight > 0 && +device.cssWidth > 0),
            toArray()
        );
    }

    /**
     * Check if current user is CMS Admin
     * @returns Observable<boolean>
     */
    checkIfCMSAdmin(): Observable<boolean> {
        return this.dotCurrentUser.getCurrentUser().pipe(
            map((user: DotCurrentUser) => {
                return user.admin;
            })
        );
    }

    /**
     * Hide the overlay panel
     */
    onHideDeviceSelector() {
        this.hideOverlayPanel.emit();
    }

    @Input()
    set apiLink(value: string) {
        if (value) {
            const frontEndUrl = `${value.replace('api/v1/page/render', '')}`;

            this.previewUrl = `${frontEndUrl}${
                frontEndUrl.indexOf('?') != -1 ? '&' : '?'
            }disabledNavigateMode=true`;
        }
    }
}
