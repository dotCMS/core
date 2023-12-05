import { Spectator, createComponentFactory } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { ConfirmationService } from 'primeng/api';

import { DotPersonalizeService } from '@dotcms/data-access';
import { DotPersonalizeServiceMock } from '@dotcms/utils-testing';

import { EditEmaPersonaSelectorComponent } from './edit-ema-persona-selector.component';

import { DotPageApiService } from '../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../shared/consts';

describe('EditEmaPersonaSelectorComponent', () => {
    let spectator: Spectator<EditEmaPersonaSelectorComponent>;
    let component: EditEmaPersonaSelectorComponent;

    const createComponent = createComponentFactory({
        component: EditEmaPersonaSelectorComponent,
        imports: [HttpClientTestingModule],
        providers: [
            ConfirmationService,
            { provide: DotPersonalizeService, useValue: new DotPersonalizeServiceMock() },
            {
                provide: DotPageApiService,
                useValue: {
                    getPersonas() {
                        return of({
                            entity: [DEFAULT_PERSONA],
                            pagination: {
                                totalEntries: 1,
                                page: 1,
                                perPage: 10
                            }
                        });
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                value: {
                    ...DEFAULT_PERSONA,
                    identifier: 'some test'
                },
                pageID: '123'
            }
        });

        component = spectator.component;
    });

    describe('events', () => {
        it('should emit the default persona when the selected emits the default persona', () => {
            const selectedSpy = jest.spyOn(component.selected, 'emit');
            spectator.triggerEventHandler('p-autocomplete', 'onSelect', DEFAULT_PERSONA);
            expect(selectedSpy).toHaveBeenCalledWith({ ...DEFAULT_PERSONA, pageID: '123' });
        });

        it('should not emit the selected persona when it is already selected', () => {
            const selectedSpy = jest.spyOn(component.selected, 'emit');
            spectator.triggerEventHandler('p-autocomplete', 'onSelect', {
                identifier: 'some test'
            });
            expect(selectedSpy).not.toHaveBeenCalled();
        });

        it('should emit the selected persona when it is personalized', () => {
            const selectedSpy = jest.spyOn(component.selected, 'emit');

            spectator.triggerEventHandler('p-autocomplete', 'onSelect', {
                identifier: 'some test 2',
                personalized: true,
                pageID: '123'
            });

            expect(selectedSpy).toHaveBeenCalledWith({
                identifier: 'some test 2',
                personalized: true,
                pageID: '123'
            });
        });
    });
});
