import { Component, OnInit, inject } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotActionBulkResult } from '@dotcms/dotcms-models';
@Component({
    selector: 'dot-bulk-information',
    templateUrl: './dot-bulk-information.component.html',
    styleUrls: ['./dot-bulk-information.component.scss'],
    standalone: false
})
export class DotBulkInformationComponent implements OnInit {
    ref = inject(DynamicDialogRef);
    config = inject(DynamicDialogConfig);

    data: DotActionBulkResult;
    ngOnInit(): void {
        this.data = this.config.data;
    }
}
