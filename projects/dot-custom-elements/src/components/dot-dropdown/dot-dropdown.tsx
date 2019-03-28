import { Component, Prop, State, Event, EventEmitter } from '@stencil/core';
import { generateId, getItemsFromString } from '../../utils';
import Fragment from 'stencil-fragment';
import { DotOption } from '../../models/dot-option.model';

@Component({
    tag: 'dot-dropdown',
    styleUrl: 'dot-dropdown.scss'
})
export class DotDropdownComponent {
    @Prop() label: string;
    @Prop() hint: string;
    @Prop() options: string;
    @Prop() value: string;
    @Event() onChange: EventEmitter;

    @State() _options: DotOption[];
    @State() _value: string;
    _label: string;

    componentWillLoad() {
        this._options = getItemsFromString(this.options);
        this._label = `dotDropdown_${generateId()}`;
    }

    // Todo: find how to set proper TYPE in TS
    setValue(event): void {
        this._value = event.target[event.target.selectedIndex].label;
        this.onChange.emit({ value: this._value });
    }

    render() {
        return (
            <Fragment>
                <label htmlFor={this._label}>{this.label}</label>
                <select name={this._label} onChange={(event: Event) => this.setValue(event)}>
                    {this._options.map((item: DotOption) => {
                        return (
                            <option
                                selected={this.value === item.value ? true : null}
                                value={item.value}
                            >
                                {item.label}
                            </option>
                        );
                    })}
                </select>
                {this.hint ? <span class='dot-textfield__hint'>{this.hint}</span> : ''}
            </Fragment>
        );
    }
}
