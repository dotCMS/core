import { Spectator, createComponentFactory, byTestId } from '@ngneat/spectator/jest';
import { provideComponentStore } from '@ngrx/component-store';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';

import { DotActionMenuButtonComponent } from '@dotcms/ui';

import { DotLanguagesListComponent } from './dot-languages-list.component';
import { DotLanguageRow, DotLanguagesListStore } from './store/dot-languages-list.store';

describe('DotLanguagesListComponent', () => {
    let spectator: Spectator<DotLanguagesListComponent>;

    const createComponent = createComponentFactory({
        component: DotLanguagesListComponent,
        componentProviders: [provideComponentStore(DotLanguagesListStore)],
        declarations: [
            MockComponent(DotActionMenuButtonComponent),
            TableModule,
            ButtonModule,
            InputTextModule
        ]
    });

    const mockLanguages: DotLanguageRow[] = [
        {
            locale: 'English (en-US)',
            language: 'English - en',
            country: 'United States - US',
            defaultLanguage: true,
            variables: 'TBD',
            actions: [
                {
                    menuItem: {
                        label: 'Edit',
                        command: () => {
                            //TODO: Implement
                        }
                    },
                    shouldShow: () => true
                },
                {
                    menuItem: {
                        label: 'Delete',
                        command: () => {
                            //TODO: Implement
                        }
                    },
                    shouldShow: () => true
                }
            ]
        }
    ];

    beforeEach(() => (spectator = createComponent()));

    it('should display languages when vm$ emits', () => {
        spectator.component.vm$ = of({ languages: mockLanguages });

        spectator.detectChanges();

        expect(spectator.query(byTestId('locale-cell'))).toHaveText('English (en-US)');
    });

    it('should display default language tag for default languages', () => {
        spectator.component.vm$ = of({ languages: mockLanguages });

        spectator.detectChanges();

        expect(spectator.query('.p-tag-success')).toHaveText('Default');
    });

    it('should display action menu for each language', () => {
        spectator.component.vm$ = of({ languages: mockLanguages });

        spectator.detectChanges();

        expect(spectator.query(DotActionMenuButtonComponent)?.actions?.length).toEqual(2);
    });
});
