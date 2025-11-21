import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, input } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotRelativeDatePipe, DotDiffPipe } from '@dotcms/ui';

import { DotContentCompareTableData } from '../../store/dot-content-compare.store';
import { DotContentCompareBlockEditorComponent } from '../dot-content-compare-block-editor/dot-content-compare-block-editor.component';
import { DotContentComparePreviewFieldComponent } from '../fields/dot-content-compare-preview-field/dot-content-compare-preview-field.component';

@Component({
    selector: 'dot-content-compare-table',
    templateUrl: './dot-content-compare-table.component.html',
    styleUrls: ['./dot-content-compare-table.component.scss'],
    imports: [
        CommonModule,
        FormsModule,
        TableModule,
        SelectModule,
        SelectButtonModule,
        ButtonModule,
        DotMessagePipe,
        DotRelativeDatePipe,
        DotDiffPipe,
        DotContentComparePreviewFieldComponent,
        DotContentCompareBlockEditorComponent
    ]
})
export class DotContentCompareTableComponent {
    private dotMessageService = inject(DotMessageService);

    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;
    $showActions = input<boolean>(true, { alias: 'showActions' });

    @Output() changeVersion = new EventEmitter<DotCMSContentlet>();
    @Output() changeDiff = new EventEmitter<boolean>();
    @Output() bringBack = new EventEmitter<string>();

    displayOptions = [
        { label: this.dotMessageService.get('diff'), value: true },
        { label: this.dotMessageService.get('plain'), value: false }
    ];
}
