import {
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ChangeDetectionStrategy,
    input
} from '@angular/core';

import { ButtonModule } from 'primeng/button';

@Component({
    selector: 'dot-empty-state',
    templateUrl: './dot-empty-state.component.html',
    styleUrls: ['./dot-empty-state.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [ButtonModule]
})
export class DotEmptyStateComponent implements OnInit {
    readonly rows = input<number>();
    readonly colsTextWidth = input<number[]>([]);
    // TODO: Skipped for migration because:
    //  This input is used in a control flow expression (e.g. `@if` or `*ngIf`)
    //  and migrating would break narrowing currently.
    @Input() icon!: string;
    // TODO: Skipped for migration because:
    //  This input is used in a control flow expression (e.g. `@if` or `*ngIf`)
    //  and migrating would break narrowing currently.
    @Input() title!: string;
    // TODO: Skipped for migration because:
    //  This input is used in a control flow expression (e.g. `@if` or `*ngIf`)
    //  and migrating would break narrowing currently.
    @Input() content!: string;
    readonly buttonLabel = input<string>();
    @Output() buttonClick = new EventEmitter<void>();

    columnWidth!: string;
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
        return Array(this.rows()).fill(0);
    }
    /**
     *  Emits event to navigate later to a Portlet
     *
     * @memberof DotEmptyStateComponent
     */
    handleClick() {
        this.buttonClick.emit();
    }

    private getColumnWidth(): string {
        return `${(100 - this.checkBoxWidth) / this.colsTextWidth().length}%`;
    }
}
