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

describe('DotBlockEditorComponent - ControlValueAccesor', () => {
    let spectator: Spectator<MockFormComponent>;
    const createComponent = createComponentFactory({
        component: MockFormComponent
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should set form value when binary file changes', () => {
        const blockEditorComponent = spectator.query(DotBlockEditorComponent);
        blockEditorComponent.value = BLOCK_EDITOR_FIELD;

        const formValue = spectator.component.form.get('block').value;

        expect(formValue).toEqual(JSON.stringify(BLOCK_EDITOR_FIELD));
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
            expect(mockEditor.setEditable).toHaveBeenCalledWith(true);

            blockEditorComponent.setDisabledState(true);
            expect(mockEditor.setEditable).toHaveBeenCalledWith(false);
        });

        it('should not emit changes when disabled', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const emitSpy = jest.spyOn(blockEditorComponent.valueChange, 'emit');

            // Set disabled state
            blockEditorComponent.disabled = true;

            // Try to trigger change
            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(emitSpy).not.toHaveBeenCalled();
        });

        it('should emit changes when not disabled', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const emitSpy = jest.spyOn(blockEditorComponent.valueChange, 'emit');

            // Ensure not disabled
            blockEditorComponent.disabled = false;

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

        it('should set charLimitExceeded error when character count exceeds charLimit', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');

            blockEditorComponent.editor = createMockEditor(150);
            blockEditorComponent.charLimit = 100;
            blockEditorComponent.disabled = false;

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
            blockEditorComponent.disabled = false;

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
            blockEditorComponent.disabled = false;

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
            blockEditorComponent.disabled = false;

            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            // charLimitExceeded removed; required preserved
            expect(control.errors).toEqual({ required: true });
        });

        it('should not set charLimitExceeded error when charLimit is not defined', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');

            blockEditorComponent.editor = createMockEditor(150);
            // charLimit remains NaN (its default when field variable is undefined)
            blockEditorComponent.disabled = false;

            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(control.errors).toBeNull();
        });

        it('should not set charLimitExceeded error when charLimit is zero', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');

            blockEditorComponent.editor = createMockEditor(150);
            blockEditorComponent.charLimit = 0;
            blockEditorComponent.disabled = false;

            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(control.errors).toBeNull();
        });

        it('should not set charLimitExceeded error when character count equals the limit', () => {
            const blockEditorComponent = spectator.query(DotBlockEditorComponent);
            const control = spectator.component.form.get('block');

            blockEditorComponent.editor = createMockEditor(100);
            blockEditorComponent.charLimit = 100;
            blockEditorComponent.disabled = false;

            blockEditorComponent.onBlockEditorChange(BLOCK_EDITOR_FIELD);

            expect(control.errors).toBeNull();
        });
    });
});
