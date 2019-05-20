import { Component, Prop, Event, EventEmitter } from '@stencil/core';
import { DotKeyValueField } from '../../../models';

@Component({
    tag: 'key-value-table'
})
export class KeyValueTableComponent {
    @Prop() items: DotKeyValueField[] = [];
    @Prop() disabled = false;
    @Prop() buttonDeleteLabel = 'Delete';

    @Event() deleteItemEvt: EventEmitter;

    render() {
        return (
            <table>
                <tbody>
                    {this.items.map((item: DotKeyValueField, index: number) => {
                        return (
                            <tr>
                                <td>
                                    <button
                                        type="button"
                                        id={`${item.key}_${item.value}_${index}`}
                                        disabled={this.disabled || null}
                                        onClick={() => this.deleteItem(index)}
                                        class="dot-key-value__delete-button"
                                    >
                                        {this.buttonDeleteLabel}
                                    </button>
                                </td>
                                <td>{item.key}</td>
                                <td>{item.value}</td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        );
    }

    private deleteItem(index: number): void {
        this.deleteItemEvt.emit(index);
    }
}
