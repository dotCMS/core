import { Component, Prop, State, Event, EventEmitter } from '@stencil/core';
import { generateId } from '../../utils/utils';
import Fragment from 'stencil-fragment';

@Component({
    tag: 'dot-dropdown',
    styleUrl: 'dot-dropdown.scss'
})
export class DotDropdownComponent {
    @Prop() label: string;
    @Prop() hint: string;
    @Prop() options: string;
    @Prop() value: string;
    @Event() onCallback: EventEmitter;

    @State() _options: string[];
    @State() _value: string;
    _label: string;

    componentWillLoad() {
        this._options = this.options
            .replace(/(\\r\\n|\\n|\\r)/gi, '|')
            .split('|')
            .filter((item) => item.length > 0);
        this._label = `dotDropdown_${generateId()}`;
    }

    setValue(event): void {
        this._value = event.target[event.target.selectedIndex].label;
        this.onCallback.emit({ value: this._value });
    }

    render() {
        return (
            <Fragment>
                <label htmlFor={this._label}>{this.label}</label>
                <select name={this._label} onChange={(event: Event) => this.setValue(event)}>
                    {this._options.map((item: string, index: number) => {
                        return (
                            <option
                                selected={this.value === item ? true : null}
                                value={index}
                            >
                                {item}
                            </option>
                        );
                    })}
                </select>
                {this.hint ? <span class='dot-textfield__hint'>{this.hint}</span> : ''}
            </Fragment>
        );
    }
}
