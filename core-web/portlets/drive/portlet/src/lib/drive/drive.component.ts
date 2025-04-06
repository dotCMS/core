import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';

import { TableModule } from 'primeng/table';
import { take } from 'rxjs/operators';

import { DotESContentService, queryEsParams } from '@dotcms/data-access';
import { ESContent, DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'lib-drive',
    standalone: true,
    imports: [CommonModule, TableModule],
    providers: [DotESContentService],
    templateUrl: './drive.component.html',
    styleUrl: './drive.component.scss'
})
export class DriveComponent implements OnInit {
    items: DotCMSContentlet[] = [];
    loading = false;

    constructor(private dotESContentService: DotESContentService) {}

    ngOnInit() {
        this.loadRootFiles();
    }

    private loadRootFiles(): void {
        this.loading = true;

        const params: queryEsParams = {
            query: '+path:/*',
            itemsPerPage: 40
        };

        this.dotESContentService.get(params)
            .pipe(take(1))
            .subscribe((response: ESContent) => {
                this.items = response.jsonObjectView.contentlets;
                this.loading = false;
            });
    }
}
