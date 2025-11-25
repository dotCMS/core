import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
    inject
} from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Table, TableModule } from 'primeng/table';

import { take } from 'rxjs/operators';

import { PaginatorService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotDialogComponent, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-form-selector',
    templateUrl: './dot-form-selector.component.html',
    styleUrls: ['./dot-form-selector.component.scss'],
    imports: [TableModule, DotDialogComponent, ButtonModule, DotMessagePipe],
    providers: [PaginatorService]
})
export class DotFormSelectorComponent implements OnInit, OnChanges {
    paginatorService = inject(PaginatorService);

    @Input() show = false;

    @Output() pick = new EventEmitter<DotCMSContentType>();

    @Output() shutdown = new EventEmitter<boolean>();

    @ViewChild('datatable', { static: true }) datatable: Table;

    @ViewChild('dialog', { static: true }) dotDialog: DotDialogComponent;

    items: DotCMSContentType[];
    contentMinHeight: string;

    ngOnInit() {
        this.paginatorService.paginationPerPage = 5;
        this.paginatorService.url = 'v1/contenttype?type=FORM';
    }
    ngOnChanges(changes: SimpleChanges) {
        setTimeout(() => {
            if (changes.show.currentValue) {
                this.contentMinHeight =
                    this.paginatorService.totalRecords > this.paginatorService.paginationPerPage
                        ? `${
                              this.dotDialog.dialog.nativeElement
                                  .querySelector('.p-datatable')
                                  .getBoundingClientRect().height
                          }px`
                        : '';
                this.datatable.tableViewChild.nativeElement.querySelector('button')?.focus();
            }
        }, 0);
    }

    /**
     * Call when click on any pagination link
     *
     * @param LazyLoadEvent event
     * @memberof DotFormSelectorComponent
     */
    loadData(event: LazyLoadEvent): void {
        this.paginatorService
            .getWithOffset(event.first)
            .pipe(take(1))
            .subscribe((items: DotCMSContentType[]) => {
                this.items = items;
            });
    }
}
