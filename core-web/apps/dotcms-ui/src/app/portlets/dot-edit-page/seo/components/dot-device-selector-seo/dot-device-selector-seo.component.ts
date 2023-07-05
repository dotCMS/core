import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    HostBinding,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges
} from '@angular/core';

import { filter, flatMap, map, take, toArray } from 'rxjs/operators';

import { DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { DotDevice } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-device-selector-seo',
    templateUrl: './dot-device-selector-seo.component.html',
    styleUrls: ['./dot-device-selector-seo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotDeviceSelectorSeoComponent implements OnInit, OnChanges {
    @Input() value: DotDevice;
    @Output() selected = new EventEmitter<DotDevice>();
    @HostBinding('class.disabled') disabled: boolean;

    options: DotDevice[] = [];
    placeholder = '';
    defaultOptions = [
        { name: 'Mobile Portrait', icon: 'pi pi-mobile', cssHeight: '390', cssWidth: '844' },
        { name: 'Mobile Landscape', icon: 'pi pi-mobile', cssHeight: '844', cssWidth: '390' },
        { name: 'HD Monitor', icon: 'pi pi-desktop', cssHeight: '1920', cssWidth: '1080' },
        { name: '4K Monitor', icon: 'pi pi-desktop', cssHeight: '3840', cssWidth: '2160' },
        { name: 'Table Portrait', icon: 'pi pi-tablet', cssHeight: '820', cssWidth: '1180' },
        { name: 'Table Landscape', icon: 'pi pi-tablet', cssHeight: '1180', cssWidth: '820' }
    ];

    constructor(
        private dotDevicesService: DotDevicesService,
        private dotMessageService: DotMessageService,
        private readonly cd: ChangeDetectorRef
    ) {}

    ngOnInit() {
        this.loadOptions();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.value && !changes.value.firstChange) {
            this.loadOptions();
        }
    }

    /**
     * Track changes in the dropwdown
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
                flatMap((devices: DotDevice[]) => devices),
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
                    this.disabled = this.options.length < 2;

                    if (this.disabled) {
                        this.placeholder = 'No devices';
                    }
                },
                () => {
                    this.disabled = true;
                    this.placeholder = 'No devices';
                },
                () => {
                    this.cd.detectChanges();
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
