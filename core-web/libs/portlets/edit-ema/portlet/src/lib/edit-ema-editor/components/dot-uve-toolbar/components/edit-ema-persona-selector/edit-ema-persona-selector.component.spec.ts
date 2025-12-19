import { expect, describe, it } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { ConfirmationService } from 'primeng/api';
import { Paginator } from 'primeng/paginator';

import { DotCMSViewAsPersona } from '@dotcms/types';

import { EditEmaPersonaSelectorComponent } from './edit-ema-persona-selector.component';

import { DotPageApiService } from '../../../../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../../../../shared/consts';

export const CUSTOM_PERSONA: DotCMSViewAsPersona = {
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

const TEST_DEFAULT_PERSONA: DotCMSViewAsPersona = {
    ...DEFAULT_PERSONA,
    photo: { versionPath: '/dA/198-23423-234' },
    url: 'example.defaultsite.com'
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
            {
                provide: DotPageApiService,
                useValue: {
                    getPersonas() {
                        return of({
                            data: [TEST_DEFAULT_PERSONA, CUSTOM_PERSONA],
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
                value: TEST_DEFAULT_PERSONA,
                pageId: '123'
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
            expect(component.listbox.value).toEqual(TEST_DEFAULT_PERSONA);
        });

        it('should add the chip to the personalized persona', () => {
            spectator.click(button);

            const chip = spectator.query(byTestId('persona-chip'));

            expect(chip).not.toBeNull();
        });

        it('should show a paginator when there are more than 10 personas', () => {
            component.$personas.set({
                items: Array(11).fill(CUSTOM_PERSONA),
                totalRecords: 11,
                itemsPerPage: 10
            });

            spectator.click(button);

            expect(spectator.query(byTestId('persona-paginator'))).not.toBeNull();
        });

        it('should not show a paginator when there are less than 10 personas', () => {
            component.$personas.set({
                items: Array(9).fill(CUSTOM_PERSONA),
                totalRecords: 9,
                itemsPerPage: 10
            });

            spectator.click(button);

            expect(spectator.query(byTestId('persona-paginator'))).toBeNull();
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
                pageId: '123'
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

        it("should call onRemove when remove icon it's clicked", () => {
            spectator.click(button);

            const onRemoveSpy = jest.spyOn(component, 'onRemove');

            const removeIcon = spectator.query('.p-chip-remove-icon');
            spectator.click(removeIcon);

            expect(onRemoveSpy).toHaveBeenCalledWith(
                expect.anything(), // This is a mouse event, not relevant for this test
                {
                    ...CUSTOM_PERSONA
                },
                false
            );
        });

        it("should pass selected as true when remove icon it's clicked on a selected value", () => {
            component.value = CUSTOM_PERSONA;
            spectator.click(button);

            const onRemoveSpy = jest.spyOn(component, 'onRemove');

            const removeIcon = spectator.query('.p-chip-remove-icon');
            spectator.click(removeIcon);

            expect(onRemoveSpy).toHaveBeenCalledWith(
                expect.anything(), // This is a mouse event, not relevant for this test
                {
                    ...CUSTOM_PERSONA
                },
                true
            );
        });

        it('should call fetchPersonas with incremented page when clicked in paginator', () => {
            const fetchPersonasSpy = jest.spyOn(component, 'fetchPersonas');

            component.$personas.set({
                items: Array(11).fill(CUSTOM_PERSONA),
                totalRecords: 11,
                itemsPerPage: 10
            });

            spectator.click(button);
            // PrimeNG paginator starts at 0, so the second page is 1
            spectator.triggerEventHandler(Paginator, 'onPageChange', { page: 1 });

            // but the API starts at 1, so we need to add 1
            expect(fetchPersonasSpy).toHaveBeenCalledWith(2);
        });

        it('should call fetchPersonas when pageId changes', () => {
            const fetchPersonasSpy = jest.spyOn(component, 'fetchPersonas');

            spectator.setInput('pageId', '456');
            spectator.detectChanges();

            expect(fetchPersonasSpy).toHaveBeenCalled();
        });
    });
});
