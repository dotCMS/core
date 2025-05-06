import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { AvailableLanguageMonaco } from '../../models/dot-edit-content-field.constant';
import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';

/**
 * JSON field editor component that uses Monaco Editor for JSON content editing.
 * Uses DotEditContentMonacoEditorControl for editor functionality with JSON language forced.
 */
@Component({
    selector: 'dot-edit-content-json-field',
    standalone: true,
    imports: [ReactiveFormsModule, DotEditContentMonacoEditorControlComponent],
    templateUrl: './dot-edit-content-json-field.component.html',
    styleUrls: ['./dot-edit-content-json-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentJsonFieldComponent {
    /**
     * Input field DotCMSContentTypeField
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    /**
     * Available languages for Monaco editor
     */
    protected readonly languages = AvailableLanguageMonaco;
}
