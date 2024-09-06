import { of as observableOf, Observable } from 'rxjs';

import { DotPersona } from '@dotcms/dotcms-models';

import { mockDotPersona } from './dot-persona.mock';

export class DotPersonasServiceMock {
    get(): Observable<DotPersona[]> {
        return observableOf([mockDotPersona]);
    }
}
