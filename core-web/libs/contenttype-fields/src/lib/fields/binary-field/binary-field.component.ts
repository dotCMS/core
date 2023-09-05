import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';

@Component({
    selector: 'dotcms-binary-field',
    standalone: true,
    imports: [ButtonModule, DialogModule, MonacoEditorModule],
    templateUrl: './binary-field.component.html',
    styleUrls: ['./binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BinaryFieldComponent {
    private readonly dotMessageService = inject(DotMessageService);
    labelCode = this.dotMessageService.get('contenttypes.content.edit.write.code');
    visible = false;

    showCodeEditor() {
        this.visible = true;
    }
}
