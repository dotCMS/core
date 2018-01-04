import { Component, OnDestroy, Input, Output, EventEmitter, OnInit,
         ViewChild, ElementRef } from '@angular/core';
import { HotkeysService, Hotkey } from 'angular2-hotkeys';
import { DotMessageService } from '../../../../api/services/dot-messages-service';

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
})
export class ContentTypeFieldsAddRowComponent implements OnDestroy, OnInit {
    rowState = 'add';
    selectedColumnIndex = 0;
    i18nMessages = {};

    @Input() columns: number[] = [1, 2, 3, 4];
    @Input() disabled = false;
    @Input() toolTips: string[] = [
        'contenttypes.content.one_column',
        'contenttypes.content.two_columns',
        'contenttypes.content.three_columns',
        'contenttypes.content.four_columns'
    ];
    @Output() selectColums: EventEmitter<number> = new EventEmitter<number>();
    @ViewChild('colContainer') colContainerElem: ElementRef;

    constructor(private hotkeysService: HotkeysService , public dotMessageService: DotMessageService) {
    }

    ngOnInit(): void {
        this.setKeyboardEvent('ctrl+a', this.setColumnSelect.bind(this));
        this.loadMessages();
    }

    ngOnDestroy(): void {
        this.removeHotKeys();
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
     * @param {number} n
     * @returns {number[]}
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
        setTimeout(() => { this.setFocus(this.getElementSelected()); }, 201);
    }

    /**
     * Set focus on element sent as param
     * @param elem
     */
    setFocus(elem: any): void {
        elem.focus();
    }

    /**
     * Remove focus on element sent as param
     * @param {*} elem
     * @returns {*}
     * @memberof ContentTypeFieldsAddRowComponent
     */
    removeFocus(elem: any): void {
        elem.blur();
    }

    /**
     * Set keyboard event receiving key and function as param
     * @param {(string | string[])} key
     * @param {any} keyEvent
     * @returns {*}
     * @memberof ContentTypeFieldsAddRowComponent
     */
    setKeyboardEvent(key: string | string[], keyEvent): any {
        this.hotkeysService.add(new Hotkey(key, (event: KeyboardEvent): boolean => {
            keyEvent();
            return false;
        }));
    }

    /**
     * Set tooltip value to pTooltip directive
     * Receives column index as param
     * @param {number} col
     * @returns {string}
     * @memberof ContentTypeFieldsAddRowComponent
     */
    setTooltipValue(col: number): string {
        return this.i18nMessages[this.toolTips[col]];
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

    private loadMessages(): void {
        const i18nKeys = [
            ...this.toolTips,
            'contenttypes.dropzone.rows.add'
        ];

        this.dotMessageService.getMessages(i18nKeys).subscribe(res => {
            this.i18nMessages = res;
        });
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
