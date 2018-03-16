import { Observable } from 'rxjs/Observable';
import { DotDevice } from '../shared/models/dot-device/dot-device.model';
import { mockDotDevice } from './dot-device.mock';

export class DotDevicesServiceMock {
    get(): Observable<DotDevice[]> {
        return Observable.of([mockDotDevice]);
    }
}
