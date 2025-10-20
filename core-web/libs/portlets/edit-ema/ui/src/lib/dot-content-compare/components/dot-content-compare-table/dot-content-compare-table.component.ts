import { Component, EventEmitter, Input, Output, inject, input } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotContentCompareTableData } from '../../store/dot-content-compare.store';

@Component({
    selector: 'dot-content-compare-table',
    templateUrl: './dot-content-compare-table.component.html',
    styleUrls: ['./dot-content-compare-table.component.scss'],
    standalone: false
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
