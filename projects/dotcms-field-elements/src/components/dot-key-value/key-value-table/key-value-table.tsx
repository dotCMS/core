import { Component, Prop, Event, EventEmitter } from '@stencil/core';
import { DotKeyValueField, DotLabel } from '../../../models';
import { getTagLabel } from '../../../utils';

@Component({
    tag: 'key-value-table'
})
export class KeyValueTableComponent {
    @Prop() items: DotKeyValueField[] = [];
    @Prop() disabled = false;

    @Event() deleteItemEvt: EventEmitter;

    render() {
        return (
            <table>
                <tbody>
                    {this.items.map((item: DotKeyValueField, index: number) => {
                        const labelTagParams: DotLabel = {
                            name: `${item.key}_${item.value}_${index}`,
                            label: 'Delete',
                            required: false
                        };
                        return (
                            <tr>
                                <td>
                                    <button
                                        type="button"
                                        id={`${item.key}_${item.value}_${index}`}
                                        disabled={this.disabled || null}
                                        onClick={() => this.deleteItem(index)}
                                        class="dot-key-value__delete__button"
                                    >
                                        {getTagLabel(labelTagParams)}
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
