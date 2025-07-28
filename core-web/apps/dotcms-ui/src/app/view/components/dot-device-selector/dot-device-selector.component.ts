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
    SimpleChanges,
    inject
} from '@angular/core';

import { filter, map, flatMap, take, toArray } from 'rxjs/operators';

import { DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { DotDevice } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-device-selector',
    templateUrl: './dot-device-selector.component.html',
    styleUrls: ['./dot-device-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DotDeviceSelectorComponent implements OnInit, OnChanges {
    private dotDevicesService = inject(DotDevicesService);
    private dotMessageService = inject(DotMessageService);
    private readonly cd = inject(ChangeDetectorRef);

    @Input() value: DotDevice;
    @Output() selected = new EventEmitter<DotDevice>();
    @HostBinding('class.disabled') disabled: boolean;

    options: DotDevice[] = [];
    placeholder = '';

    ngOnInit() {
        this.loadOptions();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.value && !changes.value.firstChange) {
            this.loadOptions();
        }
    }

    /**
     * Track changes in the dropwdow
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
