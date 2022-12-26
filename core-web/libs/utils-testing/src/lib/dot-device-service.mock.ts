import { DotDevice } from '@dotcms/dotcms-models';
import { of as observableOf, Observable } from 'rxjs';
import { mockDotDevices } from './dot-device.mock';

export class DotDevicesServiceMock {
    get(): Observable<DotDevice[]> {
        return observableOf(mockDotDevices);
    }
}
