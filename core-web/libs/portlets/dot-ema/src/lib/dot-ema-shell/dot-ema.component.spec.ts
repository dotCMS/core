import { describe, expect } from '@jest/globals';
import { SpectatorRouting, createRoutingFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService, MessageService } from 'primeng/api';

import { DotLanguagesService, DotMessageService, DotPersonalizeService } from '@dotcms/data-access';
import { DotLanguagesServiceMock, DotPersonalizeServiceMock } from '@dotcms/utils-testing';

import { DotEmaComponent } from './dot-ema.component';
import { EditEmaStore } from './store/dot-ema.store';

import { DotPageApiService } from '../services/dot-page-api.service';
import { DEFAULT_PERSONA, WINDOW } from '../shared/consts';

describe('DotEmaComponent', () => {
    let spectator: SpectatorRouting<DotEmaComponent>;
    let store: EditEmaStore;

    const createComponent = createRoutingFactory({
        component: DotEmaComponent,
        imports: [RouterTestingModule, HttpClientTestingModule],
        detectChanges: false,
        componentProviders: [
            MessageService,
            EditEmaStore,
            ConfirmationService,
            DotMessageService,
            { provide: DotLanguagesService, useValue: new DotLanguagesServiceMock() },
            {
                provide: DotPageApiService,
                useValue: {
                    get() {
                        return of({
                            page: {
                                title: 'hello world',
                                identifier: '123'
                            },
                            viewAs: {
                                language: {
                                    id: 1,
                                    language: 'English',
                                    countryCode: 'US',
                                    languageCode: 'EN',
                                    country: 'United States'
                                },
                                persona: DEFAULT_PERSONA
                            }
                        });
                    },
                    save() {
                        return of({});
                    },
                    getPersonas() {
                        return of({
                            entity: [DEFAULT_PERSONA],
                            pagination: {
                                totalEntries: 1,
                                perPage: 10,
                                page: 1
                            }
                        });
                    }
                }
            },

            {
                provide: WINDOW,
                useValue: window
            },
            {
                provide: DotPersonalizeService,
                useValue: new DotPersonalizeServiceMock()
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(EditEmaStore, true);
        jest.spyOn(store, 'load');
    });

    describe('DOM', () => {
        it('should have a navigation bar', () => {
            expect(spectator.query('dot-edit-ema-navigation-bar')).not.toBeNull();
        });
    });

    describe('router', () => {
        it('should trigger an store load with default values', () => {
            spectator.detectChanges();

            expect(store.load).toHaveBeenCalledWith({
                language_id: 1,
                url: 'index',
                persona_id: DEFAULT_PERSONA.identifier
            });
        });

        it('should trigger a load when changing the queryParams', () => {
            spectator.triggerNavigation({
                url: [],
                queryParams: {
                    language_id: 2,
                    url: 'my-awesome-page',
                    'com.dotmarketing.persona.id': 'SomeCoolDude'
                }
            });

            spectator.detectChanges();
            expect(store.load).toHaveBeenCalledWith({
                language_id: 2,
                url: 'my-awesome-page',
                persona_id: 'SomeCoolDude'
            });
        });
    });
});
