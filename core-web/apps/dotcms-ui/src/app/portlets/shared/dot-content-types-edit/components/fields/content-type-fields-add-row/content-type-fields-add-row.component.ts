import { Subject } from 'rxjs';

import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
    inject
} from '@angular/core';

import { MenuItem } from 'primeng/api';

import { takeUntil } from 'rxjs/operators';

import { DotEventsService, DotMessageService } from '@dotcms/data-access';

/**
 * Display select columns row
 *
 * @export
 * @class ContentTypeFieldsAddRowComponent
 */

@Component({
    selector: 'dot-add-rows',
    styleUrls: ['./content-type-fields-add-row.component.scss'],
    templateUrl: './content-type-fields-add-row.component.html',
    standalone: false
})
export class ContentTypeFieldsAddRowComponent implements OnDestroy, OnInit {
    private dotEventsService = inject(DotEventsService);
    private dotMessageService = inject(DotMessageService);

    rowState = 'add';
    selectedColumnIndex = 0;
    actions: MenuItem[];

    @Input() columns: number[] = [1, 2, 3, 4];
    @Input() disabled = false;
    @Input()
    toolTips: string[] = [
        'contenttypes.content.single_column',
        'contenttypes.content.many_columns',
        'contenttypes.content.add_column_title'
    ];
    @Output() selectColums: EventEmitter<number> = new EventEmitter<number>();
    @ViewChild('colContainer') colContainerElem: ElementRef;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.loadActions();
        this.dotEventsService
            .listen('add-row')
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                this.setColumnSelect();
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Set columns active when mouse enter
     * @param col
     */
    onMouseEnter(col: number, event): void {
        this.selectedColumnIndex = col;
        this.setFocus(this.getElementSelected());
        event.preventDefault();
    }

    onMouseLeave(event): void {
        this.removeFocus(event.target);
    }

    /**
     * Emit number of columns after select colum then reset state
     * @memberof ContentTypeFieldsAddRowComponent
     */
    emitColumnNumber(): void {
        this.selectColums.emit(this.getNumberColumnsSelected());
        this.resetState();
    }

    /**
     * Return columns inside each li element
     * @param number n
     * @returns number[]
     * @memberof ContentTypeFieldsAddRowComponent
     */
    numberOfCols(n: number): number[] {
        return Array(n).fill('');
    }

    /**
     * Display row when click on add rows button
     * @memberof ContentTypeFieldsAddRowComponent
     */
    setColumnSelect(): void {
        this.rowState = 'select';
        // Transitions over focus event doesn't work, It needs a setTimeout
        // with time over the CSS transition 200 ms
        setTimeout(() => {
            this.setFocus(this.getElementSelected());
        }, 201);
    }

    /**
     * Set focus on element sent as param
     * @param elem
     */
    setFocus(elem: HTMLElement): void {
        elem.focus({ preventScroll: true });
    }

    /**
     * Remove focus on element sent as param
     * @param * elem
     * @returns *
     * @memberof ContentTypeFieldsAddRowComponent
     */
    removeFocus(elem: HTMLElement): void {
        elem.blur();
    }

    /**
     * Set tooltip value to pTooltip directive
     * Receives column index as param
     * @param number col
     * @returns string
     * @memberof ContentTypeFieldsAddRowComponent
     */
    setColumnValue(col: number): string {
        return col === 0
            ? `${col + 1} ${this.dotMessageService.get('contenttypes.content.single_column')}`
            : `${col + 1} ${this.dotMessageService.get('contenttypes.content.many_columns')}`;
    }

    /**
     * Set initial values of component when click on cancel button
     * @memberof ContentTypeFieldsAddRowComponent
     */
    showAddView(): void {
        this.rowState = 'add';
        this.selectedColumnIndex = 0;
    }

    private getElementSelected(): HTMLElement {
        return this.colContainerElem.nativeElement.children[this.selectedColumnIndex];
    }

    private loadActions(): void {
        this.actions = [
            {
                label: this.dotMessageService.get('contenttypes.dropzone.rows.add'),
                command: () => {
                    this.setColumnSelect();
                }
            },
            {
                label: this.dotMessageService.get('contenttypes.dropzone.rows.tab_divider'),
                command: () => {
                    this.dotEventsService.notify('add-tab-divider');
                }
            }
        ];
    }

    private getNumberColumnsSelected() {
        return this.columns[this.selectedColumnIndex];
    }

    private resetState(): void {
        this.removeFocus(this.getElementSelected());
        this.selectedColumnIndex = 0;
        this.rowState = 'add';
    }
}
