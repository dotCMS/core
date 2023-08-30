import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

@Component({
    selector: 'dotcms-binary-field',
    standalone: true,
    imports: [
        BrowserAnimationsModule,
        CommonModule,
        ButtonModule,
        DialogModule,
        MonacoEditorModule
    ],
    templateUrl: './binary-field.component.html',
    styleUrls: ['./binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BinaryFieldComponent {
    visible = false;

    showCodeEditor() {
        this.visible = true;
    }
}
