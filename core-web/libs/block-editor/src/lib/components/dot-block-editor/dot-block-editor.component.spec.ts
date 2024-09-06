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
        const expected = {
            type: 'doc',
            content: [
                {
                    type: 'paragraph',
                    attrs: { textAlign: 'left' },
                    content: [
                        {
                            type: 'text',
                            text: '{"attrs":{"charCount":9,"readingTime":1,"wordCount":2},"content":[{"attrs":{"level":1,"textAlign":"left"},"content":[{"text":"A title!!","type":"text"}],"type":"heading"}],"type":"doc"}'
                        }
                    ]
                }
            ]
        };
        expect(formValue).toEqual(JSON.stringify(expected));
    });
});
