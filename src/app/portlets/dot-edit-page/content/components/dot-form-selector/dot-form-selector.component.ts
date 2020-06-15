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
import { DotCMSContentType } from 'dotcms-models';
import { LazyLoadEvent, DataTable } from 'primeng/primeng';
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

    @Output() select = new EventEmitter<DotCMSContentType>();

    @Output() close = new EventEmitter<any>();

    @ViewChild('datatable') datatable: DataTable;

    @ViewChild('dialog') dotDialog: DotDialogComponent;

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
                        ? `${this.dotDialog.dialog.nativeElement
                              .querySelector('.ui-datatable')
                              .getBoundingClientRect().height}px`
                        : '';
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
