import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
    WritableSignal,
    inject
} from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule, Dialog } from 'primeng/dialog';
import { Table, TableModule } from 'primeng/table';

import { take } from 'rxjs/operators';

import { PaginatorService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-form-selector',
    templateUrl: './dot-form-selector.component.html',
    styleUrls: ['./dot-form-selector.component.scss'],
    imports: [TableModule, DialogModule, ButtonModule, DotMessagePipe],
    providers: [PaginatorService]
})
export class DotFormSelectorComponent implements OnInit, OnChanges {
    paginatorService = inject(PaginatorService);

    @Input() show = false;

    @Output() pick = new EventEmitter<DotCMSContentType>();

    @Output() shutdown = new EventEmitter<boolean>();

    @ViewChild('datatable', { static: true }) datatable: Table;

    @ViewChild('dialog', { static: true }) dotDialog: Dialog;

    items: DotCMSContentType[];
    contentMinHeight: string;

    ngOnInit() {
        this.paginatorService.paginationPerPage = 5;
        this.paginatorService.url = 'v1/contenttype?type=FORM';
    }
    ngOnChanges(changes: SimpleChanges) {
        setTimeout(() => {
            if (changes.show.currentValue) {
                // container is already the native element
                // TODO: (migration) update to use signal because it was failing in the build after upgrading to primeng 21, needs to test
                const dialogElement = this.dotDialog.container as unknown as WritableSignal<HTMLElement | undefined>;
                const tableElement = dialogElement()?.querySelector('.p-datatable');
                this.contentMinHeight =
                    this.paginatorService.totalRecords > this.paginatorService.paginationPerPage &&
                    tableElement
                        ? `${tableElement.getBoundingClientRect().height}px`
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
