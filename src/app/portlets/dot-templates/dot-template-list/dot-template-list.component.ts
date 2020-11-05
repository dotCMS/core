import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { pluck, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { DotTemplate } from '@portlets/dot-edit-page/shared/models';
import { DataTableColumn } from '@models/data-table';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

@Component({
    selector: 'dot-template-list',
    templateUrl: './dot-template-list.component.html',
    styleUrls: ['./dot-template-list.component.scss']
})
export class DotTemplateListComponent implements OnInit, OnDestroy {
    tableColumns: DataTableColumn[];
    firstPage: DotTemplate[];

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(private route: ActivatedRoute, private dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.route.data
            .pipe(pluck('dotTemplateListResolverData'), takeUntil(this.destroy$))
            .subscribe((templates: DotTemplate[]) => {
                this.firstPage = templates;
                this.tableColumns = this.setTemplateColumns();
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }


    /**
     * Handle selected template.
     * @param {DotTemplate} template
     *
     * @memberof DotTemplateListComponent
     */
    editTemplate(template: DotTemplate): void {
        console.log(template);
    }


    private setTemplateColumns(): DataTableColumn[] {
        return [
            {
                fieldName: 'name',
                header: this.dotMessageService.get('templates.fieldName.name'),
                sortable: true
            },
            {
                fieldName: 'status',
                header: this.dotMessageService.get('templates.fieldName.status')
            },
            {
                fieldName: 'friendlyName',
                header: this.dotMessageService.get('templates.fieldName.description')
            },
            {
                fieldName: 'modDate',
                format: 'date',
                header: this.dotMessageService.get('templates.fieldName.lastEdit'),
                sortable: true
            }
        ];
    }
}
