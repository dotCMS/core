import {
    Component,
    EventEmitter,
    Input,
    Output,
    HostBinding,
    OnInit,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { DotDevicesService } from '@services/dot-devices/dot-devices.service';
import { DotDevice } from '@models/dot-device/dot-device.model';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { map, take, flatMap, filter, toArray } from 'rxjs/operators';

@Component({
    selector: 'dot-device-selector',
    templateUrl: './dot-device-selector.component.html',
    styleUrls: ['./dot-device-selector.component.scss']
})
export class DotDeviceSelectorComponent implements OnInit, OnChanges {
    @Input() value: DotDevice;
    @Output() selected = new EventEmitter<DotDevice>();
    @HostBinding('class.disabled') disabled: boolean;

    options: DotDevice[] = [];

    constructor(
        private dotDevicesService: DotDevicesService,
        private dotMessageService: DotMessageService
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
                },
                () => {
                    this.disabled = true;
                }
            );
    }

    private setOptions(message: string, devices: DotDevice[]): DotDevice[] {
        return [
            {
                name: message,
                cssHeight: '',
                cssWidth: '',
                inode: '0'
            },
            ...devices
        ];
    }
}
