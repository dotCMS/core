import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { ConfirmationService } from 'primeng/api';

import { DotPersonalizeService } from '@dotcms/data-access';
import { DotPersona } from '@dotcms/dotcms-models';
import { DotPersonalizeServiceMock } from '@dotcms/utils-testing';

import { EditEmaPersonaSelectorComponent } from './edit-ema-persona-selector.component';

import { DotPageApiService } from '../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../shared/consts';

export const CUSTOM_PERSONA: DotPersona = {
    hostFolder: 'CUSTOM_HOST',
    inode: 'unique-inode-id',
    host: 'CUSTOM_HOST',
    locked: true,
    stInode: 'a1b2c3d4-e5f6-7890-gh12-34i5j6kl7m8n',
    contentType: 'customPersona',
    identifier: 'modes.customPersona.uniqueIdentifier',
    folder: 'CUSTOM_FOLDER',
    hasTitleImage: true,
    owner: 'CUSTOM_USER',
    url: 'example.customsite.com',
    sortOrder: 1,
    name: 'Advanced User',
    hostName: 'Custom Host',
    modDate: '2023-12-06',
    title: 'Advanced User Profile',
    personalized: true,
    baseType: 'ADVANCED_PERSONA',
    archived: true,
    working: true,
    live: true,
    keyTag: 'custom:advancedPersona',
    languageId: 2,
    titleImage: 'path/to/title/image.jpg',
    modUserName: 'custom admin user',
    hasLiveVersion: true,
    modUser: 'customAdmin'
};

describe('EditEmaPersonaSelectorComponent', () => {
    let spectator: Spectator<EditEmaPersonaSelectorComponent>;
    let component: EditEmaPersonaSelectorComponent;
    let button: Element;
    let selectedSpy: jest.SpyInstance;

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
                            data: [DEFAULT_PERSONA, CUSTOM_PERSONA],
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
                    ...DEFAULT_PERSONA
                },
                pageID: '123'
            }
        });

        component = spectator.component;
        button = spectator.query(byTestId('persona-button'));
        selectedSpy = jest.spyOn(component.selected, 'emit');
    });

    describe('dom', () => {
        describe('button', () => {
            it('should not have selected class', () => {
                expect(button.classList.contains('selected')).toBe(false);
            });

            it('should have selected class', () => {
                component.value = CUSTOM_PERSONA;
                spectator.detectComponentChanges();

                expect(button.classList.contains('selected')).toBe(true);
            });
        });

        it('should have a p-overlay', () => {
            expect(spectator.query(byTestId('persona-op'))).not.toBeNull();
        });

        it('should have p-listbox hidden', () => {
            expect(spectator.query(byTestId('persona-listbox'))).toBeNull();
        });

        it('should show p-listbox on button click', () => {
            spectator.click(button);
            expect(spectator.query(byTestId('persona-listbox'))).not.toBeNull();
        });

        it('should set the value to the listbox', () => {
            expect(component.listbox.value).toEqual(DEFAULT_PERSONA);
        });

        it('should add the check icon to the personalized persona', () => {
            spectator.click(button);
            expect(spectator.queryAll('.pi').length).toBe(1);
            expect(spectator.query('.pi').outerHTML).toBe(
                `<i class="pi pi-tag ng-star-inserted"></i>`
            );
        });
    });

    describe('events', () => {
        it('should emit selected persona', () => {
            spectator.click(button);
            spectator.triggerEventHandler('p-listbox', 'onChange', {
                value: {
                    identifier: 'modes.customPersona.uniqueIdentifier'
                }
            });

            expect(selectedSpy).toHaveBeenCalledWith({
                ...{
                    identifier: 'modes.customPersona.uniqueIdentifier'
                },
                pageID: '123'
            });
        });

        it('should not emit persona when it currently selected', () => {
            spectator.click(button);

            spectator.triggerEventHandler('p-listbox', 'onChange', {
                value: {
                    identifier: 'modes.persona.no.persona'
                }
            });
            expect(selectedSpy).not.toHaveBeenCalled();
        });
    });
});
