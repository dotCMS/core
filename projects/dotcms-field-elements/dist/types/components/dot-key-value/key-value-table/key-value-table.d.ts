import '../../../stencil.core';
import { EventEmitter } from '../../../stencil.core';
import { DotKeyValueField } from '../../../models';
export declare class KeyValueTableComponent {
    /** (optional) Items to render in the list of key value */
    items: DotKeyValueField[];
    /** (optional) Disables all form interaction */
    disabled: boolean;
    /** (optional) Label for the delete button in each item list */
    buttonLabel: string;
    /** (optional) Message to show when the list of items is empty */
    emptyMessage: string;
    /** Emit the index of the item deleted from the list */
    delete: EventEmitter<number>;
    render(): JSX.Element;
    private onDelete;
    private getRow;
    private renderRows;
    private getEmptyRow;
    private isValidItems;
}
