import { Component, OnInit } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotActionBulkResult } from '@dotcms/dotcms-models';
@Component({
    selector: 'dot-bulk-information',
    templateUrl: './dot-bulk-information.component.html',
    styleUrls: ['./dot-bulk-information.component.scss']
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
