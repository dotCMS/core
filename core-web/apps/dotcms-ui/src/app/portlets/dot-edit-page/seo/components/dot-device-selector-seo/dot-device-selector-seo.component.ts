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

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';
import { PanelModule } from 'primeng/panel';

import { filter, mergeMap, take, toArray } from 'rxjs/operators';

import { DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { DotDevice, DotDeviceListItem } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

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
        DotMessagePipe
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
    @ViewChild('op') overlayPanel: OverlayPanel;

    options$: Observable<DotDevice[]>;
    socialMediaTiles = [
        { label: 'Facebook', icon: 'pi pi-facebook' },
        { label: 'Twitter', icon: 'pi pi-twitter' },
        { label: 'LinkedIn', icon: 'pi pi-linkedin' }
    ];
    defaultOptions: DotDeviceListItem[] = [
        {
            name: this.dotMessageService.get('editpage.device.selector.mobile.portrait'),
            icon: 'pi pi-mobile',
            cssHeight: '390',
            cssWidth: '844',
            inode: '0',
            identifier: ''
        },
        {
            name: this.dotMessageService.get('editpage.device.selector.mobile.landscape'),
            icon: 'pi pi-mobile',
            cssHeight: '844',
            cssWidth: '390',
            inode: '0',
            identifier: ''
        },
        {
            name: this.dotMessageService.get('editpage.device.selector.hd.monitor'),
            icon: 'pi pi-desktop',
            cssHeight: '1920',
            cssWidth: '1080',
            inode: '0',
            identifier: ''
        },
        {
            name: this.dotMessageService.get('editpage.device.selector.4k.monitor'),
            icon: 'pi pi-desktop',
            cssHeight: '3840',
            cssWidth: '2160',
            inode: '0',
            identifier: ''
        },
        {
            name: this.dotMessageService.get('editpage.device.selector.tablet.portrait'),
            icon: 'pi pi-tablet',
            cssHeight: '820',
            cssWidth: '1180',
            inode: '0',
            identifier: ''
        },
        {
            name: this.dotMessageService.get('editpage.device.selector.tablet.landscape'),
            icon: 'pi pi-tablet',
            cssHeight: '1180',
            cssWidth: '820',
            inode: '0',
            identifier: ''
        }
    ];

    constructor(
        private dotDevicesService: DotDevicesService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.options$ = this.getOptions();
    }

    /**
     * Emit selected changes
     * @param DotDevice device
     */
    change(device: DotDevice) {
        this.selected.emit(device);
        this.overlayPanel.hide();
    }

    public openMenu(event: Event) {
        this.overlayPanel.toggle(event);
    }

    /**
     * Load the options for the select
     *
     * @returns Observable<DotDevice[]>
     */
    public getOptions(): Observable<DotDevice[]> {
        return this.dotDevicesService.get().pipe(
            take(1),
            mergeMap((devices: DotDevice[]) => devices),
            filter((device: DotDevice) => +device.cssHeight > 0 && +device.cssWidth > 0),
            toArray()
        );
    }
}
