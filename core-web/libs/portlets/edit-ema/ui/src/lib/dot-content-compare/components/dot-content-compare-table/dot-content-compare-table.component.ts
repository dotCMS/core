import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotContentCompareTableData } from '../../store/dot-content-compare.store';
import { DotContentComparePreviewFieldComponent } from '../fields/dot-content-compare-preview-field/dot-content-compare-preview-field.component';

@Component({
    selector: 'dot-content-compare-table',
    templateUrl: './dot-content-compare-table.component.html',
    styleUrls: ['./dot-content-compare-table.component.scss'],
    imports: [
        CommonModule,
        FormsModule,
        TableModule,
        DropdownModule,
        SelectButtonModule,
        ButtonModule,
        DotMessagePipe,
        DotRelativeDatePipe,
        DotContentComparePreviewFieldComponent
    ]
})
export class DotContentCompareTableComponent {
    private dotMessageService = inject(DotMessageService);

    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;

    @Output() changeVersion = new EventEmitter<DotCMSContentlet>();
    @Output() changeDiff = new EventEmitter<boolean>();
    @Output() bringBack = new EventEmitter<string>();

    displayOptions = [
        { label: this.dotMessageService.get('diff'), value: true },
        { label: this.dotMessageService.get('plain'), value: false }
    ];
}
