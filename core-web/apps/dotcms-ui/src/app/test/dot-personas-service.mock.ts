import { of as observableOf, Observable } from 'rxjs';
import { DotPersona } from '@models/dot-persona/dot-persona.model';
import { mockDotPersona } from './dot-persona.mock';

export class DotPersonasServiceMock {
    get(): Observable<DotPersona[]> {
        return observableOf([mockDotPersona]);
    }
}
