import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DotDevicesService } from '../../../api/services/dot-devices/dot-devices.service';
import { DotDevice } from '../../../shared/models/dot-device/dot-device.model';
import { Observable } from 'rxjs/Observable';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import {DotPersona} from '../../../shared/models/dot-persona/dot-persona.model';
import {concat} from 'rxjs/observable/concat';

@Component({
    selector: 'dot-device-selector',
    templateUrl: './dot-device-selector.component.html',
    styleUrls: ['./dot-device-selector.component.scss']
})
export class DotDeviceSelectorComponent implements OnInit {
    @Input() value: DotDevice;
    @Output() selected = new EventEmitter<DotDevice>();

    devicesOptions: Observable<DotDevice[]>;

    constructor(private dotDevicesService: DotDevicesService, private dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.devicesOptions = this.dotMessageService.getMessages(['editpage.viewas.default.device'])
            .mergeMap((messages: string[]) =>
                this.dotDevicesService.get()
                    .map((devices: DotDevice[]) => [
                        {
                            name: messages['editpage.viewas.default.device'],
                            cssHeight: '',
                            cssWidth: '',
                            inode: '0'
                        },
                        ...devices
                    ])
            );
    }

    /**
     * Track changes in the dropwdow
     * @param {DotDevice} device
     */
    change(device: DotDevice) {
        this.selected.emit(device);
    }
}
