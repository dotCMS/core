import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DotDevicesService } from '../../../api/services/dot-devices/dot-devices.service';
import { DotDevice } from '../../../shared/models/dot-device/dot-device.model';
import { Observable } from 'rxjs/Observable';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { map } from 'rxjs/operators/map';

@Component({
    selector: 'dot-device-selector',
    templateUrl: './dot-device-selector.component.html',
    styleUrls: ['./dot-device-selector.component.scss']
})
export class DotDeviceSelectorComponent implements OnInit {
    @Input() value: DotDevice;
    @Output() selected = new EventEmitter<DotDevice>();

    options: Observable<DotDevice[]>;

    constructor(private dotDevicesService: DotDevicesService, private dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.options = this.dotMessageService
            .getMessages(['editpage.viewas.default.device'])
            .mergeMap(() =>
                this.dotDevicesService
                    .get()
                    .pipe(
                        map((devices: DotDevice[]) =>
                            this.setOptions(this.dotMessageService.get('editpage.viewas.default.device'), devices)
                        )
                    )
            );
    }

    /**
     * Track changes in the dropwdow
     * @param {DotDevice} device
     */
    change(device: DotDevice) {
        this.selected.emit(device);
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
