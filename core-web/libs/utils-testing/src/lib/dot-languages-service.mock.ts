import { of, Observable } from 'rxjs';

import { DotLanguage } from '@dotcms/dotcms-models';

import { mockLanguageArray } from './dot-language.mock';

export class DotLanguagesServiceMock {
    get(): Observable<DotLanguage[]> {
        return of(mockLanguageArray);
    }
}
