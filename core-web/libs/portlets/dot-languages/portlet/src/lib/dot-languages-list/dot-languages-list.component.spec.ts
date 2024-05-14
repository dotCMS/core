import { Spectator, createComponentFactory, byTestId, mockProvider } from '@ngneat/spectator/jest';
import { provideComponentStore } from '@ngrx/component-store';

import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotActionMenuButtonComponent, DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotLanguagesListComponent } from './dot-languages-list.component';
import { DotLanguagesListStore } from './store/dot-languages-list.store';

const messageServiceMock = new MockDotMessageService({
    'languages.locale': 'Locale'
});

describe('DotLanguagesListComponent', () => {
    let spectator: Spectator<DotLanguagesListComponent>;

    const endPointLanguages: DotLanguage[] = [
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

    const createComponent = createComponentFactory({
        component: DotLanguagesListComponent,
        componentProviders: [provideComponentStore(DotLanguagesListStore)],
        imports: [
            CommonModule,
            InputTextModule,
            ButtonModule,
            DotMessagePipe,
            TableModule,
            DotActionMenuButtonComponent,
            TagModule
        ],
        providers: [
            mockProvider(MessageService),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: {
                        data: {
                            languages: endPointLanguages
                        }
                    }
                }
            }
        ]
    });

    beforeEach(() => (spectator = createComponent()));

    it('should display languages when vm$ emits', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('locale-cell'))).toHaveText('English (en-US)');
    });

    it('should display default language tag for default languages', () => {
        //   spectator.component.vm$ = of({ languages: mockLanguages });

        spectator.detectChanges();

        expect(spectator.query('.p-tag-success')).toHaveText('Default');
    });

    it('should display action menu for each language', () => {
        //spectator.component.vm$ = of({ languages: mockLanguages });

        spectator.detectChanges();

        expect(spectator.query(DotActionMenuButtonComponent)?.actions?.length).toEqual(2);
    });
});
