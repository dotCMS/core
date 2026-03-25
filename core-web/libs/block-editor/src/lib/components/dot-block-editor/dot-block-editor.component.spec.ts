import { Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';

import { Component } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';

import { DotBlockEditorComponent } from './dot-block-editor.component';

import { BlockEditorModule } from '../../block-editor.module';

const BLOCK_EDITOR_FIELD = {
    attrs: {
        charCount: 9,
        readingTime: 1,
        wordCount: 2
    },
    content: [
        {
            attrs: {
                level: 1,
                textAlign: 'left'
            },
            content: [
                {
                    text: 'A title!!',
                    type: 'text'
                }
            ],
            type: 'heading'
        }
    ],
    type: 'doc'
};

/**
 * TODO: Remove it and use `FormGroupMockDirective` when movving this component to `libs/edit-content` if needed.
 *
 * @class MockFormComponent
 */
@Component({
    selector: 'dot-app-mock-form',
    imports: [BlockEditorModule],
    template: `
        <form [formGroup]="form">
            <dot-block-editor formControlName="block"></dot-block-editor>
        </form>
    `
})
class MockFormComponent {
    field = BLOCK_EDITOR_FIELD;
    form = new FormGroup({
        block: new FormControl(JSON.stringify(BLOCK_EDITOR_FIELD))
    });
}

describe('DotBlockEditorComponent - ControlValueAccessor', () => {
    let spectator: Spectator<MockFormComponent>;
    const createComponent = createComponentFactory({
        component: MockFormComponent
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should update form value when onBlockEditorChange is called', () => {
        const blockEditorComponent = spectator.query(DotBlockEditorComponent);
        const control = spectator.component.form.get('block');

        // Reset to null first so the assertion proves onChange actually propagated
        control.setValue(null);

        blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

        expect(control.value).toEqual(JSON.stringify(BLOCK_EDITOR_FIELD));
    });

    describe('Disabled State', () => {
        it('should set disabled state via setDisabledState method', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);

            // Initially not disabled
            expect(blockEditorComponent.disabled).toBe(false);

            // Set disabled state
            blockEditorComponent.setDisabledState(true);

            expect(blockEditorComponent.disabled).toBe(true);
        });

        it('should apply disabled CSS classes when disabled', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);

            // Check that when editor exists, setEditable is called properly
            const mockEditor = {
                setEditable: jest.fn()
            } as Partial<typeof blockEditorComponent.editor>;
            blockEditorComponent.editor = mockEditor as typeof blockEditorComponent.editor;

            blockEditorComponent.setDisabledState(false);
            spectator.detectChanges();
            expect(mockEditor.setEditable).toHaveBeenCalledWith(true);

            blockEditorComponent.setDisabledState(true);
            spectator.detectChanges();
            expect(mockEditor.setEditable).toHaveBeenCalledWith(false);
        });

        it('should not emit changes when disabled', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const emitSpy = jest.spyOn(blockEditorComponent.valueChange, 'emit');

            // Use the CVA method to set disabled state
            blockEditorComponent.setDisabledState(true);

            // Try to trigger change
            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(emitSpy).not.toHaveBeenCalled();
        });

        it('should emit changes when not disabled', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const emitSpy = jest.spyOn(blockEditorComponent.valueChange, 'emit');

            // Use the CVA method to set disabled state
            blockEditorComponent.setDisabledState(false);

            // Trigger change
            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(emitSpy).toHaveBeenCalledWith(BLOCK_EDITOR_FIELD);
        });
    });

    describe('hasFieldError input', () => {
        it('should default to false', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            expect(blockEditorComponent.hasFieldError).toBe(false);
        });

        it('should accept a true value', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            blockEditorComponent.hasFieldError = true;
            expect(blockEditorComponent.hasFieldError).toBe(true);
        });
    });

    describe('hasError getter', () => {
        it('should return false when no errors exist', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            expect(blockEditorComponent.hasError).toBe(false);
        });

        it('should return true when hasFieldError is true', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            blockEditorComponent.hasFieldError = true;
            expect(blockEditorComponent.hasError).toBe(true);
        });

        it('should return true when charLimitExceeded error exists on the form control', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');
            control.setErrors({ charLimitExceeded: { max: 100, actual: 150 } });

            expect(blockEditorComponent.hasError).toBe(true);
        });

        it('should return true when both hasFieldError and charLimitError are present', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');
            blockEditorComponent.hasFieldError = true;
            control.setErrors({ charLimitExceeded: { max: 100, actual: 150 } });

            expect(blockEditorComponent.hasError).toBe(true);
        });
    });

    describe('charLimitError getter', () => {
        it('should return null when no charLimitExceeded error exists on the control', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            expect(blockEditorComponent.charLimitError).toBeNull();
        });

        it('should return the error object when charLimitExceeded error is set on the control', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');
            control.setErrors({ charLimitExceeded: { max: 200, actual: 250 } });

            expect(blockEditorComponent.charLimitError).toEqual({ max: 200, actual: 250 });
        });

        it('should return null when control has other errors but not charLimitExceeded', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');
            control.setErrors({ required: true });

            expect(blockEditorComponent.charLimitError).toBeNull();
        });
    });

    describe('requiredError getter', () => {
        it('should return false when the control has no errors', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            expect(blockEditorComponent.requiredError).toBe(false);
        });

        it('should return false when the control has a required error but is not touched', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');
            control.setErrors({ required: true });

            // Control is untouched by default
            expect(blockEditorComponent.requiredError).toBe(false);
        });

        it('should return true when the control has a required error and is touched', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');
            control.setErrors({ required: true });
            control.markAsTouched();

            expect(blockEditorComponent.requiredError).toBe(true);
        });

        it('should return false when the control is touched but has no required error', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');
            control.markAsTouched();

            expect(blockEditorComponent.requiredError).toBe(false);
        });

        it('should return false when the control is touched and has other errors but not required', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');
            control.setErrors({ charLimitExceeded: { max: 100, actual: 150 } });
            control.markAsTouched();

            expect(blockEditorComponent.requiredError).toBe(false);
        });
    });

    describe('onBlockEditorChange and char limit validation', () => {
        /**
         * Helper to create a minimal mock editor with the given character/word counts.
         */
        function createMockEditor(characters: number, words = 10) {
            return {
                storage: {
                    characterCount: {
                        characters: () => characters,
                        words: () => words
                    }
                }
            } as unknown as DotBlockEditorComponent['editor'];
        }

        describe('doc attrs (charCount / wordCount / readingTime)', () => {
            it('should include charCount, wordCount and readingTime in the emitted value when content is not empty', () => {
                const blockEditorComponent = spectator.query(DotBlockEditorComponent);
                const emitSpy = jest.spyOn(blockEditorComponent.valueChange, 'emit');

                // 265 words at 265 words-per-minute (Medium formula) → readingTime = Math.ceil(265/265) = 1
                blockEditorComponent.editor = createMockEditor(100, 265);
                blockEditorComponent.setDisabledState(false);

                const valueWithoutAttrs: typeof BLOCK_EDITOR_FIELD = {
                    ...BLOCK_EDITOR_FIELD,
                    attrs: {}
                };

                blockEditorComponent.onBlockEditorChange(valueWithoutAttrs);

                expect(emitSpy).toHaveBeenCalledWith(
                    expect.objectContaining({
                        attrs: expect.objectContaining({
                            charCount: 100,
                            wordCount: 265,
                            readingTime: 1
                        })
                    })
                );
            });

            it('should not override existing attrs when patching doc attrs', () => {
                const blockEditorComponent = spectator.query(DotBlockEditorComponent);
                const emitSpy = jest.spyOn(blockEditorComponent.valueChange, 'emit');

                blockEditorComponent.editor = createMockEditor(50, 10);
                blockEditorComponent.setDisabledState(false);

                blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

                // charCount/wordCount are overwritten by the live values from the mock editor;
                // any other attrs that were already on the doc should still be present.
                expect(emitSpy).toHaveBeenCalledWith(
                    expect.objectContaining({
                        type: BLOCK_EDITOR_FIELD.type,
                        content: BLOCK_EDITOR_FIELD.content,
                        attrs: expect.objectContaining({
                            charCount: 50,
                            wordCount: 10
                        })
                    })
                );
            });

            it('should emit value unchanged when the editor has no content (charCount = 0)', () => {
                const blockEditorComponent = spectator.query(DotBlockEditorComponent);
                const emitSpy = jest.spyOn(blockEditorComponent.valueChange, 'emit');

                blockEditorComponent.editor = createMockEditor(0, 0);
                blockEditorComponent.setDisabledState(false);

                blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

                expect(emitSpy).toHaveBeenCalledWith(BLOCK_EDITOR_FIELD);
            });

            it('should emit value unchanged when the editor is not yet initialized', () => {
                const blockEditorComponent = spectator.query(DotBlockEditorComponent);
                const emitSpy = jest.spyOn(blockEditorComponent.valueChange, 'emit');

                // Force the editor to be null to simulate the case where the editor is not yet
                // initialized (e.g., writeValue called before the async ngOnInit completes).
                blockEditorComponent.editor = null;
                blockEditorComponent.setDisabledState(false);

                blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

                expect(emitSpy).toHaveBeenCalledWith(BLOCK_EDITOR_FIELD);
            });
        });

        it('should set charLimitExceeded error when character count exceeds charLimit', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');

            blockEditorComponent.editor = createMockEditor(150);
            blockEditorComponent.charLimit = 100;
            blockEditorComponent.setDisabledState(false);

            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(control.errors).toEqual(
                expect.objectContaining({
                    charLimitExceeded: { max: 100, actual: 150 }
                })
            );
        });

        it('should mark the control as touched when charLimitExceeded is set', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');

            blockEditorComponent.editor = createMockEditor(150);
            blockEditorComponent.charLimit = 100;
            blockEditorComponent.setDisabledState(false);

            // Ensure untouched initially
            expect(control.touched).toBe(false);

            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(control.touched).toBe(true);
        });

        it('should clear charLimitExceeded error when character count is within limit', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');

            // Pre-set the error as if it was previously over limit
            control.setErrors({ charLimitExceeded: { max: 100, actual: 150 } });

            blockEditorComponent.editor = createMockEditor(50);
            blockEditorComponent.charLimit = 100;
            blockEditorComponent.setDisabledState(false);

            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(control.errors).toBeNull();
        });

        it('should preserve other errors when clearing charLimitExceeded', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');

            // Set multiple errors including charLimitExceeded
            control.setErrors({
                required: true,
                charLimitExceeded: { max: 100, actual: 150 }
            });

            blockEditorComponent.editor = createMockEditor(50);
            blockEditorComponent.charLimit = 100;
            blockEditorComponent.setDisabledState(false);

            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            // charLimitExceeded removed; required preserved
            expect(control.errors).toEqual({ required: true });
        });

        it('should not set charLimitExceeded error when charLimit is not defined', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');

            blockEditorComponent.editor = createMockEditor(150);
            // charLimit remains NaN (its default when field variable is undefined)
            blockEditorComponent.setDisabledState(false);

            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(control.errors).toBeNull();
        });

        it('should not set charLimitExceeded error when charLimit is zero', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');

            blockEditorComponent.editor = createMockEditor(150);
            blockEditorComponent.charLimit = 0;
            blockEditorComponent.setDisabledState(false);

            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(control.errors).toBeNull();
        });

        it('should not set charLimitExceeded error when character count equals the limit', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');

            blockEditorComponent.editor = createMockEditor(100);
            blockEditorComponent.charLimit = 100;
            blockEditorComponent.setDisabledState(false);

            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(control.errors).toBeNull();
        });
    });
});
