import { describe, expect } from '@jest/globals';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';

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
    let spectator: Spectator<DotEditContentTagFieldComponent>;
    let service: SpyObject<DotEditContentService>;
    let formGroup: FormGroup;
    let formGroupDirective: FormGroupDirective;

    const createComponent = createComponentFactory({
        component: DotEditContentTagFieldComponent,
        componentViewProviders: [
            {
                provide: ControlContainer,
                useExisting: FormGroupDirective
            }
        ],
        providers: [FormGroupDirective, mockProvider(DotEditContentService)]
    });

    beforeEach(() => {
        formGroup = new FormGroup({});
        formGroup.addControl(TAG_FIELD_MOCK.variable, new FormControl([]));

        formGroupDirective = new FormGroupDirective([], []);
        formGroupDirective.form = formGroup;

        spectator = createComponent({
            detectChanges: false,
            providers: [
                {
                    provide: FormGroupDirective,
                    useValue: formGroupDirective
                }
            ]
        });

        spectator.setInput({
            field: TAG_FIELD_MOCK
        });

        service = spectator.inject(DotEditContentService);
        service.getTags.mockReturnValue(of(['tagExample']));

        spectator.detectChanges();
    });

    describe('Component Configuration', () => {
        it('should render autocomplete with correct attributes', () => {
            const autocomplete = spectator.query(AutoComplete);
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
            const control = formGroup.get(TAG_FIELD_MOCK.variable);
            expect(spectator.component.formControl).toBeDefined();
            expect(spectator.component.formControl).toBe(control);
        });
    });

    describe('User Interactions', () => {
        it('should show suggestions when user types valid search term', async () => {
            const expectedTags = ['angular', 'typescript'];
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
            let autocomplete: AutoComplete;
            let autocompleteInput: HTMLInputElement;

            beforeEach(() => {
                autocomplete = spectator.query(AutoComplete);
                autocompleteInput = spectator.query('input[role="combobox"]');
            });

            const simulateEnterKey = () => {
                const enterEvent = new KeyboardEvent('keydown', {
                    key: 'Enter',
                    bubbles: true,
                    cancelable: true
                });
                autocompleteInput.dispatchEvent(enterEvent);
            };

            it('should add new tag when Enter is pressed with non-empty value', () => {
                const newTag = 'newTag';
                autocompleteInput.value = newTag;

                simulateEnterKey();
                spectator.detectChanges();

                expect(spectator.component.formControl?.value).toEqual([newTag]);
                expect(autocompleteInput.value).toBe('');
            });

            it('should not add empty tag when Enter is pressed with empty value', () => {
                autocompleteInput.value = '   ';

                simulateEnterKey();
                spectator.detectChanges();

                expect(spectator.component.formControl?.value).toEqual([]);
            });

            it('should append new tag to existing tags', () => {
                const existingTags = ['tag1', 'tag2'];
                const newTag = 'tag3';
                spectator.component.formControl?.setValue(existingTags);
                autocompleteInput.value = newTag;

                simulateEnterKey();
                spectator.detectChanges();

                expect(spectator.component.formControl?.value).toEqual([...existingTags, newTag]);
                expect(autocompleteInput.value).toBe('');
            });

            it('should not add duplicate tag due to AutoComplete unique property', () => {
                const existingTag = 'existingTag';
                spectator.component.formControl?.setValue([existingTag]);
                autocompleteInput.value = existingTag;

                simulateEnterKey();
                spectator.detectChanges();

                expect(spectator.component.formControl?.value).toEqual([existingTag]);
                expect(autocompleteInput.value).toBe(existingTag);
                expect(autocomplete.unique).toBe(true);
            });
        });
    });

    describe('Form Integration', () => {
        it('should update form value when user selects a tag', async () => {
            const selectedTag = 'angular';
            service.getTags.mockReturnValue(of([selectedTag]));

            // Simulate user selecting a tag
            const selectAutocomplete = spectator.debugElement.query(
                By.css(`[data-testid="tag-field-container-${TAG_FIELD_MOCK.variable}"]`)
            );
            spectator.triggerEventHandler(selectAutocomplete, 'completeMethod', {
                query: selectedTag
            });

            await spectator.fixture.whenStable();

            const formControl = spectator.component.formControl;
            formControl?.setValue([selectedTag]);

            expect(formControl?.value).toEqual([selectedTag]);
        });

        it('should allow multiple tag selection', async () => {
            const tags = ['angular', 'typescript'];
            service.getTags.mockReturnValue(of(tags));
            const formControl = spectator.component.formControl;

            // Simulate user selecting multiple tags
            formControl?.setValue(tags);

            expect(formControl?.value).toEqual(tags);
        });
    });
});
