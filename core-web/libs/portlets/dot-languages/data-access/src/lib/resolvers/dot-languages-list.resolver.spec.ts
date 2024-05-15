import { Observable, of } from 'rxjs';

import { EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';

import { DotLanguagesListResolver } from './dot-languages-list.resolver';

const mockLanguages: DotLanguage[] = [
    {
        id: 1,
        languageCode: 'en',
        countryCode: 'US',
        language: 'English',
        country: 'United States',
        isoCode: 'en-US',
        defaultLanguage: true
    },
    {
        id: 2,
        languageCode: 'es',
        countryCode: 'ES',
        language: 'Spanish',
        country: 'Spain',
        isoCode: 'es-ES',
        defaultLanguage: false
    }
];

describe('DotLanguagesListResolver', () => {
    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: DotLanguagesService,
                    useValue: {
                        get: () => of(mockLanguages)
                    }
                }
            ]
        });
    });

    it('should resolve languages list', (done) => {
        const result = runInInjectionContext(TestBed.inject(EnvironmentInjector), () =>
            DotLanguagesListResolver(null, null)
        );

        (result as Observable<DotLanguage[]>).subscribe((languages) => {
            expect(languages).toEqual(mockLanguages);
            done();
        });
    });
});
