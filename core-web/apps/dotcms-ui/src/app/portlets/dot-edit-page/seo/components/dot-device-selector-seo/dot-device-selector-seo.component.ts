import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    HostBinding,
    Input,
    OnInit,
    Output
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PanelModule } from 'primeng/panel';

import { filter, mergeMap, map, take, toArray } from 'rxjs/operators';

import { DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { DotDevice, DotDeviceIcon } from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipeModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@Component({
    standalone: true,
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        DotIconModule,
        DotPipesModule,
        ButtonModule,
        OverlayPanelModule,
        PanelModule,
        DividerModule,
        DotMessagePipeModule
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
    @HostBinding('class.disabled') disabled: boolean;

    options: DotDevice[] = [];
    placeholder = '';
    socialMediaTiles = [
        { label: 'Facebook', icon: 'pi pi-facebook' },
        { label: 'Twitter', icon: 'pi pi-twitter' },
        { label: 'LinkedIn', icon: 'pi pi-linkedin' }
    ];
    defaultOptions: DotDeviceIcon[] = [
        {
            name: 'Mobile Portrait',
            icon: 'pi pi-mobile',
            cssHeight: '390',
            cssWidth: '844',
            inode: '0',
            identifier: ''
        },
        {
            name: 'Mobile Landscape',
            icon: 'pi pi-mobile',
            cssHeight: '844',
            cssWidth: '390',
            inode: '0',
            identifier: ''
        },
        {
            name: 'HD Monitor',
            icon: 'pi pi-desktop',
            cssHeight: '1920',
            cssWidth: '1080',
            inode: '0',
            identifier: ''
        },
        {
            name: '4K Monitor',
            icon: 'pi pi-desktop',
            cssHeight: '3840',
            cssWidth: '2160',
            inode: '0',
            identifier: ''
        },
        {
            name: 'Table Portrait',
            icon: 'pi pi-tablet',
            cssHeight: '820',
            cssWidth: '1180',
            inode: '0',
            identifier: ''
        },
        {
            name: 'Table Landscape',
            icon: 'pi pi-tablet',
            cssHeight: '1180',
            cssWidth: '820',
            inode: '0',
            identifier: ''
        }
    ];

    constructor(
        private dotDevicesService: DotDevicesService,
        private dotMessageService: DotMessageService,
        private readonly cd: ChangeDetectorRef
    ) {}

    ngOnInit() {
        this.loadOptions();
    }

    /**
     * Track changes in the select
     * @param DotDevice device
     */
    change(device: DotDevice) {
        this.selected.emit(device);
    }

    private loadOptions(): void {
        this.dotDevicesService
            .get()
            .pipe(
                take(1),
                mergeMap((devices: DotDevice[]) => devices),
                filter((device: DotDevice) => +device.cssHeight > 0 && +device.cssWidth > 0),
                toArray(),
                map((devices: DotDevice[]) =>
                    this.setOptions(
                        this.dotMessageService.get('editpage.viewas.default.device'),
                        devices
                    )
                )
            )
            .subscribe(
                (devices: DotDevice[]) => {
                    this.options = devices;
                },
                () => {
                    this.placeholder = 'No devices';
                }
            );
    }

    private setOptions(message: string, devices: DotDevice[]): DotDevice[] {
        return [
            {
                name: message,
                cssHeight: '',
                cssWidth: '',
                inode: '0',
                identifier: ''
            },
            ...devices
        ];
    }
}
