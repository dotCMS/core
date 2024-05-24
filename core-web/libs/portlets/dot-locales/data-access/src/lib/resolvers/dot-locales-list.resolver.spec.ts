import { Observable, of } from 'rxjs';

import { EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotLanguagesService } from '@dotcms/data-access';
import { mockLanguagesISO, mockLocales } from '@dotcms/utils-testing';

import { DotLocalesListResolver, DotLocalesListResolverData } from './dot-locales-list.resolver';

describe('DotLocalesListResolver', () => {
    const locales = [...mockLocales];
    const languagesISO = { ...mockLanguagesISO };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: DotLanguagesService,
                    useValue: {
                        get: () => of(locales),
                        getISO: () => of(languagesISO)
                    }
                }
            ]
        });
    });

    it('should resolve language list and ISO countries and languages', (done) => {
        const result = runInInjectionContext(TestBed.inject(EnvironmentInjector), () =>
            DotLocalesListResolver(null, null)
        );

        (result as Observable<DotLocalesListResolverData>).subscribe((resolverData) => {
            expect(resolverData.locales).toEqual(locales);
            expect(resolverData.countries).toEqual(languagesISO.countries);
            expect(resolverData.languages).toEqual(languagesISO.languages);
            done();
        });
    });
});
