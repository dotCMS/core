import { of as observableOf, Observable } from 'rxjs';
import { DotLanguage } from '@models/dot-language/dot-language.model';
import { mockDotLanguage } from './dot-language.mock';

export class DotLanguagesServiceMock {
    get(): Observable<DotLanguage[]> {
        return observableOf([mockDotLanguage]);
    }
}
