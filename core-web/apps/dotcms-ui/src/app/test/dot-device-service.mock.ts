import { of as observableOf, Observable } from 'rxjs';
import { DotDevice } from '@models/dot-device/dot-device.model';
import { mockDotDevices } from './dot-device.mock';

export class DotDevicesServiceMock {
    get(): Observable<DotDevice[]> {
        return observableOf(mockDotDevices);
    }
}
