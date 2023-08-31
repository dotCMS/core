import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dotcms-binary-field',
    standalone: true,
    imports: [ButtonModule, DialogModule, MonacoEditorModule, DotMessagePipe],
    templateUrl: './binary-field.component.html',
    styleUrls: ['./binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BinaryFieldComponent implements OnInit {
    public labelCode!: string;
    visible = false;

    constructor(private dotMesagePipe: DotMessagePipe) {}

    ngOnInit() {
        this.labelCode = this.dotMesagePipe.transform('contenttypes.content.edit.write.code');
    }

    showCodeEditor() {
        this.visible = true;
    }
}
