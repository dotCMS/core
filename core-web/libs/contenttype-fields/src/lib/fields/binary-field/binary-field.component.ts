import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';

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
export class BinaryFieldComponent implements OnInit {
    public labelCode!: string;
    visible = false;

    constructor(private readonly dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.labelCode = this.dotMessageService.get('contenttypes.content.edit.write.code');
    }

    showCodeEditor() {
        this.visible = true;
    }
}
