import { ChangeDetectionStrategy, Component, input, viewChild } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotLanguageVariableSelectorComponent } from '@dotcms/ui';

import { AvailableLanguageMonaco } from '../../models/dot-edit-content-field.constant';
import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';

/**
 * JSON field editor component that uses Monaco Editor for JSON content editing.
 * Uses DotEditContentMonacoEditorControl for editor functionality with JSON language forced.
 * Supports language variable insertion through DotLanguageVariableSelectorComponent.
 */
@Component({
    selector: 'dot-edit-content-json-field',
    imports: [
        ReactiveFormsModule,
        DotEditContentMonacoEditorControlComponent,
        DotLanguageVariableSelectorComponent
    ],
    templateUrl: './dot-edit-content-json-field.component.html',
    styleUrls: ['./dot-edit-content-json-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentJsonFieldComponent {
    /**
     * Input field DotCMSContentTypeField
     */
    $field = input<DotCMSContentTypeField | null>(null, {
        alias: 'field'
    });

    /**
     * Reference to the Monaco editor component
     */
    private readonly $monacoComponent =
        viewChild.required<DotEditContentMonacoEditorControlComponent>('monaco');

    /**
     * Available languages for Monaco editor
     */
    protected readonly languages = AvailableLanguageMonaco;

    /**
     * Handler for language variable selection
     * Inserts the selected language variable at current cursor position in Monaco editor
     *
     * @param languageVariable - The parsed language variable string to insert
     */
    onSelectLanguageVariable(languageVariable: string): void {
        this.insertLanguageVariableInMonaco(languageVariable);
    }

    /**
     * Insert language variable at current cursor position in Monaco editor
     *
     * @param languageVariable - The parsed language variable string to insert
     * @private
     */
    private insertLanguageVariableInMonaco(languageVariable: string): void {
        const monaco = this.$monacoComponent();
        if (monaco) {
            monaco.insertContent(languageVariable);
        } else {
            console.warn('Monaco component is not available');
        }
    }
}
