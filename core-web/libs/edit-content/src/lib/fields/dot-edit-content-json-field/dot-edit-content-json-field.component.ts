import { MonacoEditorConstructionOptions, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { ControlContainer, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-edit-content-json-field',
    standalone: true,
    imports: [FormsModule, ReactiveFormsModule, MonacoEditorModule],
    templateUrl: './dot-edit-content-json-field.component.html',
    styleUrls: ['./dot-edit-content-json-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentJsonFieldComponent {
    @Input() field!: DotCMSContentTypeField;

    public readonly editorOptions: MonacoEditorConstructionOptions = {
        theme: 'vs',
        minimap: {
            enabled: false
        },
        cursorBlinking: 'solid',
        overviewRulerBorder: false,
        mouseWheelZoom: false,
        lineNumbers: 'on',
        roundedSelection: false,
        automaticLayout: true,
        language: 'json'
    };
}
