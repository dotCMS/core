import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { pluck, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { DataTableColumn } from '@models/data-table';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { ActionHeaderOptions } from '@shared/models/action-header';
import { DotTemplate } from '@shared/models/dot-edit-layout-designer';

@Component({
    selector: 'dot-template-list',
    templateUrl: './dot-template-list.component.html',
    styleUrls: ['./dot-template-list.component.scss']
})
export class DotTemplateListComponent implements OnInit, OnDestroy {
    actions: ActionHeaderOptions;
    firstPage: DotTemplate[];
    tableColumns: DataTableColumn[];

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private route: ActivatedRoute,
        private dotMessageService: DotMessageService,
        private dotRouterService: DotRouterService
    ) {}

    ngOnInit(): void {
        this.actions = {
            primary: {
                model: [
                    {
                        command: () => {
                            this.dotRouterService.gotoPortlet('/templates/new/designer');
                        },
                        label: 'Designer'
                    },
                    {
                        command: () => {
                            this.dotRouterService.gotoPortlet('/templates/new/advanced');
                        },
                        label: 'Advanced'
                    }
                ]
            }
        };

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
     *
     * @param {DotTemplate} { identifier }
     * @memberof DotTemplateListComponent
     */
    editTemplate({ identifier }: DotTemplate): void {
        this.dotRouterService.goToEditTemplate(identifier);
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
                header: this.dotMessageService.get('templates.fieldName.lastEdit'),
                sortable: true,
                format: 'date'
            }
        ];
    }
}
