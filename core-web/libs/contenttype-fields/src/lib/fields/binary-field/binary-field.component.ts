import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, ViewChild, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotDropZoneComponent } from '@dotcms/ui';

@Component({
    selector: 'dotcms-binary-field',
    standalone: true,
    imports: [NgClass, ButtonModule, DialogModule, DotDropZoneComponent, MonacoEditorModule],
    templateUrl: './binary-field.component.html',
    styleUrls: ['./binary-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class BinaryFieldComponent {
    // Service injection
    private readonly dotMessageService = inject(DotMessageService);

    @ViewChild('inputFile', { static: true }) inputFile: ElementRef;

    active = false;

    setActiveState(value: boolean) {
        this.active = value;
    }

    openFileSelector() {
        this.inputFile.nativeElement.click();
    }
}
