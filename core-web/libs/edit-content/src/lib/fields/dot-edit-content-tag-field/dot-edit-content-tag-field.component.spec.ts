/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteCompleteEvent } from 'primeng/autocomplete';

import {
    AUTO_COMPLETE_DELAY,
    AUTO_COMPLETE_MIN_LENGTH,
    AUTO_COMPLETE_UNIQUE,
    DotEditContentTagFieldComponent
} from './dot-edit-content-tag-field.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { TAG_FIELD_MOCK } from '../../utils/mocks';

describe('DotEditContentTagFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentTagFieldComponent>;
    let autocomplete: AutoComplete;

    const createHost = createHostFactory({
        component: DotEditContentTagFieldComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        providers: [mockProvider(DotEditContentService)]
    });

    describe('Component Configuration', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-tag-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TAG_FIELD_MOCK.variable]: new FormControl([])
                        }),
                        field: TAG_FIELD_MOCK
                    }
                }
            );
            spectator.detectChanges();
            autocomplete = spectator.query(AutoComplete);
        });

        it('should render autocomplete with correct attributes', () => {
            const container = spectator.query(
                byTestId(`tag-field-container-${TAG_FIELD_MOCK.variable}`)
            );

            expect(container).toBeTruthy();
            expect(autocomplete).toBeTruthy();
            expect(autocomplete.id).toBe(`tag-id-${TAG_FIELD_MOCK.variable}`);
            expect(autocomplete.inputId).toBe(TAG_FIELD_MOCK.variable);
            expect(autocomplete.multiple).toBe(true);
            expect(autocomplete.forceSelection).toBe(false);
            expect(autocomplete.unique).toBe(AUTO_COMPLETE_UNIQUE);
            expect(autocomplete.minLength).toBe(AUTO_COMPLETE_MIN_LENGTH);
            expect(autocomplete.delay).toBe(AUTO_COMPLETE_DELAY);
        });

        it('should be connected to form control', () => {
            spectator.component.writeValue([]);
            expect(spectator.component.$values()).toEqual([]);
        });
    });

    describe('User Interactions', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-tag-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TAG_FIELD_MOCK.variable]: new FormControl([])
                        }),
                        field: TAG_FIELD_MOCK
                    }
                }
            );
            spectator.detectChanges();
            autocomplete = spectator.query(AutoComplete);
        });

        it('should show suggestions when user types valid search term', async () => {
            const expectedTags = ['angular', 'typescript'];
            const service = spectator.inject(DotEditContentService) as any;
            service.getTags.mockReturnValue(of(expectedTags));

            // Simulate user typing
            spectator.component.onSearch({
                query: 'type',
                originalEvent: new Event('input')
            } as AutoCompleteCompleteEvent);

            // Wait for the async operation to complete
            await spectator.fixture.whenStable();

            expect(service.getTags).toHaveBeenCalledWith('type');
            expect(spectator.component.$suggestions()).toEqual(expectedTags);
        });

        describe('Enter key behavior', () => {
            let autocompleteInput: HTMLInputElement;

            beforeEach(() => {
                autocompleteInput = spectator.query('input[role="combobox"]');
            });

            it('should add new tag when Enter is pressed with non-empty value', () => {
                const newTag = 'newTag';
                autocompleteInput.value = newTag;

                spectator.component.onEnterKey({
                    preventDefault: () => {
                        // do nothing
                    },
                    target: autocompleteInput
                } as any);
                spectator.detectChanges();

                expect(spectator.component.$values()).toEqual([newTag]);
                expect(autocompleteInput.value).toBe('');
            });

            it('should not add empty tag when Enter is pressed with empty value', () => {
                autocompleteInput.value = '   ';

                spectator.component.onEnterKey({
                    preventDefault: () => {},
                    target: autocompleteInput
                } as any);
                spectator.detectChanges();

                expect(spectator.component.$values()).toEqual([]);
            });

            it('should append new tag to existing tags', () => {
                const existingTags = ['tag1', 'tag2'];
                const newTag = 'tag3';
                spectator.component.writeValue(existingTags);
                autocompleteInput.value = newTag;

                spectator.component.onEnterKey({
                    preventDefault: () => {},
                    target: autocompleteInput
                } as any);
                spectator.detectChanges();

                expect(spectator.component.$values()).toEqual([...existingTags, newTag]);
                expect(autocompleteInput.value).toBe('');
            });

            it('should not add duplicate tag due to AutoComplete unique property', () => {
                const existingTag = 'existingTag';
                spectator.component.writeValue([existingTag]);
                autocompleteInput.value = existingTag;

                spectator.component.onEnterKey({
                    preventDefault: () => {},
                    target: autocompleteInput
                } as any);
                spectator.detectChanges();

                expect(spectator.component.$values()).toEqual([existingTag]);
                expect(autocompleteInput.value).toBe(existingTag);
                expect(autocomplete.unique).toBe(true);
            });
        });
    });

    describe('Form Integration', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-tag-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TAG_FIELD_MOCK.variable]: new FormControl([])
                        }),
                        field: TAG_FIELD_MOCK
                    }
                }
            );
            spectator.detectChanges();
        });

        it('should update form value when user selects a tag', async () => {
            const selectedTag = 'angular';
            const service = spectator.inject(DotEditContentService) as any;
            service.getTags.mockReturnValue(of([selectedTag]));

            // Simulate user selecting a tag
            spectator.component.onTagsChange([selectedTag]);
            await spectator.fixture.whenStable();

            expect(spectator.component.$values()).toEqual([selectedTag]);
        });

        it('should allow multiple tag selection', async () => {
            const tags = ['angular', 'typescript'];
            const service = spectator.inject(DotEditContentService) as any;
            service.getTags.mockReturnValue(of(tags));

            // Simulate user selecting multiple tags
            spectator.component.onTagsChange(tags);

            expect(spectator.component.$values()).toEqual(tags);
        });
    });
});
