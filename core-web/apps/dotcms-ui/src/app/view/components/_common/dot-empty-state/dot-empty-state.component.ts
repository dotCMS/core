import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

@Component({
    selector: 'dot-empty-state',
    templateUrl: './dot-empty-state.component.html',
    styleUrls: ['./dot-empty-state.component.scss'],
    standalone: false
})
export class DotEmptyStateComponent implements OnInit {
    @Input() rows: number;
    @Input() colsTextWidth: number[] = [];
    @Input() icon: string;
    @Input() title: string;
    @Input() content: string;
    @Input() buttonLabel: string;
    @Output() buttonClick = new EventEmitter<string>();

    columnWidth: string;
    public readonly checkBoxWidth: number = 3.5;

    ngOnInit(): void {
        this.columnWidth = this.getColumnWidth();
    }
    /**
     * Sets an array with its length set to the length of the rows property
     *
     * @return {array}  {number[]}
     * @memberof DotEmptyStateComponent
     */
    numberOfRows(): number[] {
        return Array(this.rows).fill(0);
    }
    /**
     *  Emits event to navigate later to a Portlet
     *
     * @param {string} event
     * @memberof DotEmptyStateComponent
     */
    handleClick(event: string) {
        this.buttonClick.emit(event);
    }

    private getColumnWidth(): string {
        return `${(100 - this.checkBoxWidth) / this.colsTextWidth.length}%`;
    }
}
