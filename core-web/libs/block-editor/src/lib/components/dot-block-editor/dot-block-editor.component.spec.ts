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
    standalone: true,
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
});
