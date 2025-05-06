import { Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotEditContentJsonFieldComponent } from './dot-edit-content-json-field.component';

import { AvailableLanguageMonaco } from '../../models/dot-edit-content-field.constant';
import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';
import { JSON_FIELD_MOCK } from '../../utils/mocks';

describe('DotEditContentJsonFieldComponent', () => {
    let spectator: Spectator<DotEditContentJsonFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentJsonFieldComponent,
        imports: [ReactiveFormsModule],
        declarations: [MockComponent(DotEditContentMonacoEditorControlComponent)],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('field', JSON_FIELD_MOCK);
        spectator.detectComponentChanges();
    });

    it('should render the DotEditContentMonacoEditorControl component', () => {
        const editorComponent = spectator.query(DotEditContentMonacoEditorControlComponent);
        expect(editorComponent).not.toBeNull();
    });

    it('should pass the field to the editor component', () => {
        const editorComponent = spectator.query(DotEditContentMonacoEditorControlComponent);
        expect(editorComponent.$field()).toEqual(JSON_FIELD_MOCK);
    });

    it('should force the language to be JSON', () => {
        const editorComponent = spectator.query(DotEditContentMonacoEditorControlComponent);
        expect(editorComponent.$forcedLanguage()).toEqual(AvailableLanguageMonaco.Json);
    });
});
