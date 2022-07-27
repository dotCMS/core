import {
    Component,
    OnInit,
    Input,
    Output,
    EventEmitter,
    ViewChild,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { LazyLoadEvent } from 'primeng/api';
import { Table } from 'primeng/table';
import { take } from 'rxjs/operators';
import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { PaginatorService } from '@services/paginator';

@Component({
    selector: 'dot-form-selector',
    templateUrl: './dot-form-selector.component.html',
    styleUrls: ['./dot-form-selector.component.scss'],
    providers: [PaginatorService]
})
export class DotFormSelectorComponent implements OnInit, OnChanges {
    @Input() show = false;

    @Output() pick = new EventEmitter<DotCMSContentType>();

    @Output() shutdown = new EventEmitter<boolean>();

    @ViewChild('datatable', { static: true }) datatable: Table;

    @ViewChild('dialog', { static: true }) dotDialog: DotDialogComponent;

    items: DotCMSContentType[];
    contentMinHeight: string;

    constructor(public paginatorService: PaginatorService) {}

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