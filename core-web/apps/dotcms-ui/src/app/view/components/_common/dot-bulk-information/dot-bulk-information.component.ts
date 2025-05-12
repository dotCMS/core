import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';

import { DynamicDialog, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotActionBulkResult } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-bulk-information',
    templateUrl: './dot-bulk-information.component.html',
    styleUrls: ['./dot-bulk-information.component.scss'],
    standalone: true,
    imports: [CommonModule, DynamicDialog, DotMessagePipe]
})

export class DotBulkInformationComponent implements OnInit {
    data: DotActionBulkResult;
    constructor(
        public ref: DynamicDialogRef,
        public config: DynamicDialogConfig
    ) {}
    ngOnInit(): void {
        this.data = this.config.data;
    }
}
