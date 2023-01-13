import { DotLanguage } from '@dotcms/dotcms-models';
import { of, Observable } from 'rxjs';
import { mockDotLanguage } from './dot-language.mock';

export class DotLanguagesServiceMock {
    get(): Observable<DotLanguage[]> {
        return of([mockDotLanguage]);
    }
}
