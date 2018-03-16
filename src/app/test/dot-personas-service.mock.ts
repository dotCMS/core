import { Observable } from 'rxjs/Observable';
import { DotPersona } from '../shared/models/dot-persona/dot-persona.model';
import { mockDotPersona } from './dot-persona.mock';

export class DotPersonasServiceMock {
    get(): Observable<DotPersona[]> {
        return Observable.of([mockDotPersona]);
    }
}
