import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { BlockEditorModule, DotBlockEditorComponent } from '@dotcms/block-editor';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentBlockEditorComponent } from './dot-edit-content-block-editor.component';

import { DotEditContentStore } from '../../store/edit-content.store';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
    contentlet: DotCMSContentlet;
}

const BLOCK_EDITOR_FIELD_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
    contentTypeId: 'test-content-type',
    dataType: 'LONG_TEXT',
    defaultValue: '',
    fieldType: 'STORY_BLOCK',
    fieldTypeLabel: 'Story Block',
    fieldVariables: [],
    fixed: false,
    hint: '',
    iDate: Date.now(),
    id: 'test-field-id',
    indexed: false,
    listed: false,
    modDate: Date.now(),
    name: 'Story Block Field',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 1,
    unique: false,
    variable: 'storyBlock'
};

describe('DotEditContentBlockEditorComponent', () => {
    let spectator: SpectatorHost<DotEditContentBlockEditorComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: DotEditContentBlockEditorComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule, BlockEditorModule],
        providers: [
            {
                provide: DotEditContentStore,
                useValue: {
                    currentLocale: signal({ id: 2, language: 'Spanish', country: 'Spain' })
                }
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createHost(
            `<form [formGroup]="formGroup">
                <dot-edit-content-block-editor [field]="field" [contentlet]="contentlet" />
            </form>`,
            {
                hostProps: {
                    formGroup: new FormGroup({
                        [BLOCK_EDITOR_FIELD_MOCK.variable]: new FormControl('')
                    }),
                    field: BLOCK_EDITOR_FIELD_MOCK,
                    contentlet: createFakeContentlet({
                        [BLOCK_EDITOR_FIELD_MOCK.variable]: ''
                    })
                },
                providers: [
                    {
                        provide: DotEditContentStore,
                        useValue: {
                            currentLocale: signal({ id: 2, language: 'Spanish', country: 'Spain' })
                        }
                    }
                ]
            }
        );
    });

    it('should pass the correct languageId to dot-block-editor', () => {
        spectator.detectChanges();

        const blockEditorElement = spectator.query('dot-block-editor');
        expect(blockEditorElement).toBeTruthy();

        // Access the component instance to verify the languageId property
        const blockEditorDebugElement = spectator.debugElement.query(By.css('dot-block-editor'));
        const blockEditorComponent =
            blockEditorDebugElement?.componentInstance as DotBlockEditorComponent;
        expect(blockEditorComponent.languageId).toBe(2);
    });
});
