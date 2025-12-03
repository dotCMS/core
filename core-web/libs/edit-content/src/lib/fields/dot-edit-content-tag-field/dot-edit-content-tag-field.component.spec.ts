/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import {
    byTestId,
    createHostFactory,
    mockProvider,
    SpectatorHost,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { AutoComplete } from 'primeng/autocomplete';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeTagField } from '@dotcms/utils-testing';

import {
    AUTO_COMPLETE_DELAY,
    AUTO_COMPLETE_MIN_LENGTH,
    AUTO_COMPLETE_UNIQUE,
    DotTagFieldComponent
} from './components/tag-field/tag-field.component';
import { DotEditContentTagFieldComponent } from './dot-edit-content-tag-field.component';

import { DotEditContentService } from '../../services/dot-edit-content.service';

const TAG_FIELD_MOCK = createFakeTagField({
    variable: 'tagField'
});

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    contentlet: DotCMSContentlet;
    field: DotCMSContentTypeField;
}

describe('DotEditContentTagFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentTagFieldComponent, MockFormComponent>;
    let autocomplete: AutoComplete;
    let service: SpyObject<DotEditContentService>;

    const createHost = createHostFactory({
        component: DotEditContentTagFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        providers: [mockProvider(DotEditContentService)]
    });

    describe('Component Configuration', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-tag-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TAG_FIELD_MOCK.variable]: new FormControl([])
                        }),
                        field: TAG_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [TAG_FIELD_MOCK.variable]: []
                        })
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
            const control = spectator.hostComponent.formGroup.get(TAG_FIELD_MOCK.variable);
            control.setValue([]);

            const autocomplete = spectator.query(AutoComplete);
            expect(autocomplete.value).toEqual([]);
        });
    });

    describe('User Interactions', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-tag-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TAG_FIELD_MOCK.variable]: new FormControl([])
                        }),
                        field: TAG_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [TAG_FIELD_MOCK.variable]: []
                        })
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
            spectator.triggerEventHandler(AutoComplete, 'completeMethod', {
                query: 'type',
                originalEvent: new Event('input')
            });

            // Wait for the async operation to complete
            await spectator.fixture.whenStable();

            expect(service.getTags).toHaveBeenCalledWith('type');
            expect(autocomplete.suggestions).toEqual(expectedTags);
        });

        describe('Enter key behavior', () => {
            let autocompleteInput: HTMLInputElement;

            beforeEach(() => {
                autocompleteInput = spectator.query('input[role="combobox"]');
            });

            it('should add new tag when Enter is pressed with non-empty value', () => {
                const newTag = 'newTag';
                autocompleteInput.value = newTag;

                const tagField = spectator.query(DotTagFieldComponent);
                tagField.onEnterKey({
                    preventDefault: () => {},
                    target: autocompleteInput
                } as any);
                spectator.detectChanges();

                expect(tagField.$values()).toEqual([newTag]);
                expect(autocompleteInput.value).toBe('');
            });

            it('should not add empty tag when Enter is pressed with empty value', () => {
                autocompleteInput.value = '   ';

                const tagField = spectator.query(DotTagFieldComponent);
                tagField.onEnterKey({
                    preventDefault: () => {},
                    target: autocompleteInput
                } as any);
                spectator.detectChanges();

                expect(tagField.$values()).toEqual([]);
            });

            it('should append new tag to existing tags', () => {
                const existingTags = ['tag1', 'tag2'];

                const tagField = spectator.query(DotTagFieldComponent);
                tagField.writeValue(existingTags);
                spectator.detectChanges();

                const newTag = 'tag3';
                autocompleteInput.value = newTag;

                tagField.onEnterKey({
                    preventDefault: () => {},
                    target: autocompleteInput
                } as any);
                spectator.detectChanges();

                expect(tagField.$values()).toEqual([...existingTags, newTag]);
                expect(autocompleteInput.value).toBe('');
            });

            it('should not add duplicate tag due to AutoComplete unique property', () => {
                const existingTag = 'existingTag';
                const tagField = spectator.query(DotTagFieldComponent);
                tagField.writeValue([existingTag]);
                spectator.detectChanges();
                autocompleteInput.value = existingTag;

                tagField.onEnterKey({
                    preventDefault: () => {},
                    target: autocompleteInput
                } as any);
                spectator.detectChanges();

                expect(tagField.$values()).toEqual([existingTag]);
                expect(autocompleteInput.value).toBe(existingTag);
                expect(autocomplete.unique).toBe(true);
            });
        });
    });

    describe('Form Integration', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-tag-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [TAG_FIELD_MOCK.variable]: new FormControl([])
                        }),
                        field: TAG_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [TAG_FIELD_MOCK.variable]: []
                        })
                    }
                }
            );
            spectator.detectChanges();
            service = spectator.inject(DotEditContentService);
        });

        it('should update form value when user selects a tag', () => {
            const selectedTag = 'angular';
            service.getTags.mockReturnValue(of([selectedTag]));

            // Simulate user selecting a tag
            const tagField = spectator.query(DotTagFieldComponent);
            tagField.onTagsChange([selectedTag]);
            spectator.detectChanges();

            const control = spectator.hostComponent.formGroup.get(TAG_FIELD_MOCK.variable);
            expect(control.value).toEqual(selectedTag);
        });

        it('should allow multiple tag selection', async () => {
            const tags = ['angular', 'typescript'];
            service.getTags.mockReturnValue(of(tags));

            const tagField = spectator.query(DotTagFieldComponent);
            tagField.onTagsChange(tags);

            spectator.detectChanges();

            const control = spectator.hostComponent.formGroup.get(TAG_FIELD_MOCK.variable);
            expect(control.value).toEqual('angular,typescript');
        });
    });
});
