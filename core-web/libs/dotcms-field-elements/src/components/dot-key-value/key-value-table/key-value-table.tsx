import { Component, Prop, Event, EventEmitter, h } from '@stencil/core';

import { DotKeyValueField } from '../../../models';

@Component({
    tag: 'key-value-table'
})
export class KeyValueTableComponent {
    /** (optional) Items to render in the list of key value */
    @Prop() items: DotKeyValueField[] = [];

    /** (optional) Disables all form interaction */
    @Prop({ reflect: true }) disabled = false;

    /** (optional) Label for the delete button in each item list */
    @Prop({
        reflect: true
    })
    buttonLabel = 'Delete';

    /** (optional) Message to show when the list of items is empty */
    @Prop({
        reflect: true
    })
    emptyMessage = 'No values';

    /** Emit the index of the item deleted from the list */
    @Event() delete: EventEmitter<number>;

    render() {
        return (
            <table>
                <tbody>{this.renderRows(this.items)}</tbody>
            </table>
        );
    }

    private onDelete(index: number): void {
        this.delete.emit(index);
    }

    private getRow(item: DotKeyValueField, index: number) {
        const label = `${this.buttonLabel} ${item.key}, ${item.value}`;
        return (
            <tr>
                <td>
                    <button
                        aria-label={label}
                        disabled={this.disabled || null}
                        onClick={() => this.onDelete(index)}
                        class="dot-key-value__delete-button">
                        {this.buttonLabel}
                    </button>
                </td>
                <td>{item.key}</td>
                <td>{item.value}</td>
            </tr>
        );
    }

    private renderRows(items: DotKeyValueField[]) {
        return this.isValidItems(items)
            ? (items.map(this.getRow.bind(this)) as unknown)
            : this.getEmptyRow();
    }

    private getEmptyRow() {
        return (
            <tr>
                <td>{this.emptyMessage}</td>
            </tr>
        );
    }

    private isValidItems(items: DotKeyValueField[]): boolean {
        return Array.isArray(items) && !!items.length;
    }
}
