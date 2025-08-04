import { Component, Prop, Event, EventEmitter, h, Element } from '@stencil/core';
import { DotKeyValueField } from '../../../../../models';

@Component({
    tag: 'key-value-table',
    styleUrl: 'key-value-table.scss'
})
export class KeyValueTableComponent {
    /** to get the current element */
    @Element() el: HTMLElement;

    /** (optional) Items to render in the list of key value */
    @Prop()
    items: DotKeyValueField[] = [];

    /** (optional) Disables all form interaction */
    @Prop({ reflect: true })
    disabled = false;

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
    @Event()
    delete: EventEmitter<number>;

    /** Emit the notification of list reordered */
    @Event()
    reorder: EventEmitter;

    dragSrcEl = null;

    render() {
        return (
            <table>
                <tbody>{this.renderRows(this.items)}</tbody>
            </table>
        );
    }

    componentDidLoad() {
        this.bindDraggableEvents();
    }

    componentDidUpdate() {
        this.bindDraggableEvents();
    }

    // D&D - BEGIN

    private bindDraggableEvents() {
        if (!this.disabled) {
            const rows = this.el.querySelectorAll('key-value-table tr');
            rows.forEach((row) => {
                row.setAttribute('draggable', 'true');

                row.removeEventListener('dragstart', this.handleDragStart.bind(this), false);
                row.removeEventListener('dragenter', this.handleDragEnter, false);
                row.removeEventListener('dragover', this.handleDragOver.bind(this), false);
                row.removeEventListener('dragleave', this.handleDragLeave, false);
                row.removeEventListener('drop', this.handleDrop.bind(this), false);
                row.removeEventListener('dragend', this.handleDragEnd.bind(this), false);

                row.addEventListener('dragstart', this.handleDragStart.bind(this), false);
                row.addEventListener('dragenter', this.handleDragEnter, false);
                row.addEventListener('dragover', this.handleDragOver.bind(this), false);
                row.addEventListener('dragleave', this.handleDragLeave, false);
                row.addEventListener('drop', this.handleDrop.bind(this), false);
                row.addEventListener('dragend', this.handleDragEnd.bind(this), false);
            });
        }
    }

    private removeElementById(elemId) {
        document.getElementById(elemId).remove();
    }

    private isPlaceholderInDOM() {
        return !!document.getElementById('dotKeyValuePlaceholder');
    }

    private isCursorOnUpperSide(cursor, { top, bottom }) {
        return cursor.y - top < (bottom - top) / 2;
    }

    private setPlaceholder() {
        const placeholder = document.createElement('tr');
        placeholder.id = 'dotKeyValuePlaceholder';
        placeholder.classList.add('key-value-table-wc__placeholder-transit');
        return placeholder;
    }

    private insertBeforeElement(newElem, element) {
        element.parentNode.insertBefore(newElem, element);
    }

    private insertAfterElement(newElem, element) {
        element.parentNode.insertBefore(newElem, element.nextSibling);
    }

    private handleDragStart(e) {
        this.dragSrcEl = e.target;
    }

    private handleDragOver(e) {
        if (e.preventDefault) {
            e.preventDefault();
        }
        if (this.dragSrcEl != e.target) {
            const contentlet = e.target.closest('tr');
            const contentletPlaceholder = this.setPlaceholder();
            if (this.isPlaceholderInDOM()) {
                this.removeElementById('dotKeyValuePlaceholder');
            }

            if (this.isCursorOnUpperSide(e, contentlet.getBoundingClientRect())) {
                this.insertBeforeElement(contentletPlaceholder, contentlet);
            } else {
                this.insertAfterElement(contentletPlaceholder, contentlet);
            }
        }
        return false;
    }

    private handleDragEnter(e) {
        e.target.classList.add('over');
    }

    private handleDragLeave(e) {
        e.target.classList.remove('over');
    }

    private handleDrop(e) {
        if (e.stopPropagation) {
            e.stopPropagation(); // stops the browser from redirecting.
        }
        if (this.dragSrcEl != e.target) {
            document
                .getElementById('dotKeyValuePlaceholder')
                .insertAdjacentElement('afterend', this.dragSrcEl);
        }

        return false;
    }

    private handleDragEnd() {
        const rows = document.querySelectorAll('key-value-table tr');
        rows.forEach(function (row) {
            row.classList.remove('over');
        });

        try {
            this.removeElementById('dotKeyValuePlaceholder');
        } catch (e) {
            /**/
        }

        this.reorder.emit();
    }

    // D&D - END

    private onDelete(index: number): void {
        this.delete.emit(index);
    }

    private getRow(item: DotKeyValueField, index: number) {
        const label = `${this.buttonLabel} ${item.key}, ${item.value}`;
        return (
            <tr>
                <td class="key-value-table-wc__key">{item.key}</td>
                <td class="key-value-table-wc__value">{item.value}</td>
                <td class="key-value-table-wc__action">
                    {!this.disabled ? this.getDeleteButton(label, index) : ''}
                </td>
            </tr>
        );
    }

    private getDeleteButton(label: string, index: number) {
        return (
            <button
                aria-label={label}
                onClick={() => this.onDelete(index)}
                class="dot-key-value__delete-button">
                {this.buttonLabel}
            </button>
        );
    }

    private renderRows(items: DotKeyValueField[]) {
        return this.isValidItems(items)
            ? items.map((item, index) => this.getRow(item, index))
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
