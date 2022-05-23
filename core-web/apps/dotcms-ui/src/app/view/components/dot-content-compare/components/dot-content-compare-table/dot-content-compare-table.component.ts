import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotContentCompareTableData } from '@components/dot-content-compare/store/dot-content-compare.store';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

@Component({
    selector: 'dot-content-compare-table',
    templateUrl: './dot-content-compare-table.component.html',
    styleUrls: ['./dot-content-compare-table.component.scss']
})
export class DotContentCompareTableComponent {
    @Input() data: DotContentCompareTableData;
    @Input() showDiff: boolean;

    @Output() changeVersion = new EventEmitter<DotCMSContentlet>();
    @Output() changeDiff = new EventEmitter<boolean>();
    @Output() bringBack = new EventEmitter<string>();

    displayOptions = [
        { label: this.dotMessageService.get('diff'), value: true },
        { label: this.dotMessageService.get('plain'), value: false }
    ];

    constructor(private dotMessageService: DotMessageService) {}
}
