import {
    Component,
    OnDestroy,
    Input,
    Output,
    EventEmitter,
    OnInit,
    ViewChild,
    ElementRef
} from '@angular/core';
import { HotkeysService, Hotkey } from 'angular2-hotkeys';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs/internal/Subject';
import { MenuItem } from 'primeng/primeng';

/**
 * Display select columns row
 *
 * @export
 * @class ContentTypeFieldsAddRowComponent
 */

@Component({
    selector: 'dot-add-rows',
    styleUrls: ['./content-type-fields-add-row.component.scss'],
    templateUrl: './content-type-fields-add-row.component.html'
})
export class ContentTypeFieldsAddRowComponent implements OnDestroy, OnInit {
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

    constructor(
        private dotEventsService: DotEventsService,
        private hotkeysService: HotkeysService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit(): void {
        this.setKeyboardEvent('ctrl+a', this.setColumnSelect.bind(this));
        this.loadActions();
        this.dotEventsService
            .listen('add-row')
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                this.setColumnSelect();
            });
    }

    ngOnDestroy(): void {
        this.removeHotKeys();
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
        this.bindKeyboardEvents();
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
    setFocus(elem: any): void {
        elem.focus({ preventScroll: true });
    }

    /**
     * Remove focus on element sent as param
     * @param * elem
     * @returns *
     * @memberof ContentTypeFieldsAddRowComponent
     */
    removeFocus(elem: any): void {
        elem.blur();
    }

    /**
     * Set keyboard event receiving key and function as param
     * @param (string | string[]) key
     * @param any keyEvent
     * @returns *
     * @memberof ContentTypeFieldsAddRowComponent
     */
    setKeyboardEvent(key: string | string[], keyEvent): any {
        this.hotkeysService.add(
            new Hotkey(key, (_event: KeyboardEvent): boolean => {
                keyEvent();
                return false;
            })
        );
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

    private bindKeyboardEvents(): void {
        this.setKeyboardEvent('left', this.leftKeyboardEvent.bind(this));
        this.setKeyboardEvent('right', this.rightKeyboardEvent.bind(this));
        this.setKeyboardEvent('esc', this.resetState.bind(this));
        this.setKeyboardEvent('enter', this.emitColumnNumber.bind(this));
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

    private getMaxIndex(): number {
        return this.columns.length - 1;
    }

    private leftKeyboardEvent(): any {
        this.selectedColumnIndex = this.selectedColumnIndex - 1;

        if (this.selectedColumnIndex < 0) {
            this.selectedColumnIndex = this.getMaxIndex();
        }

        this.setFocus(this.getElementSelected());
    }

    private rightKeyboardEvent(): any {
        this.selectedColumnIndex = this.selectedColumnIndex + 1;

        if (this.selectedColumnIndex > this.getMaxIndex()) {
            this.selectedColumnIndex = 0;
        }

        this.setFocus(this.getElementSelected());
    }

    private resetState(): any {
        this.removeFocus(this.getElementSelected());
        this.selectedColumnIndex = 0;
        this.rowState = 'add';
        this.removeHotKeys();
    }

    private removeHotKeys(): void {
        this.hotkeysService.remove(this.hotkeysService.get(['left', 'right', 'enter', 'esc']));
    }
}
