import { Component, Prop, State, Event, EventEmitter } from '@stencil/core';
import { generateId } from '../../utils/utils';
import Fragment from 'stencil-fragment';

@Component({
    tag: 'dot-checkbox',
    styleUrl: 'dot-checkbox.scss'
})
export class DotCheckboxComponent {
    @Prop() label: string;
    @Prop() hint: string;
    @Prop() options: string;
    @Prop() value: string;
    @Event() onCallback: EventEmitter;

    @State() _options: any;
    @State() _value: string;
    _label: string;
    _values = {};

    componentWillLoad() {
        this._options = this.options
            .replace(/(\\r\\n|\\n|\\r)/gi, '|')
            .split('|')
            .filter((item) => item.length > 0);
        this._label = `dotCheckbox_${generateId()}`;
    }

    setValue(event): void {
        const checkBoxVal = event.target.value.toString();
        this._values = { ...this._values, [checkBoxVal]: !this._values[checkBoxVal] };
        this.onCallback.emit({ value: this._values });
    }

    render() {
        return (
            <Fragment>
                <label htmlFor={this._label}>{this.label}</label>
                {this._options.map((item: string) => {
                    this._values = { ...this._values, [item]: this.value === item ? true : false };
                    return (
                        <div class='dot-checkbox__container'>
                            <input
                                type='checkbox'
                                name={item}
                                checked={this.value === item ? true : null}
                                onInput={(event: Event) => this.setValue(event)}
                                value={item}
                            />
                            <label htmlFor={item}>{item}</label>
                        </div>
                    );
                })}
                {this.hint ? <span class='dot-textfield__hint'>{this.hint}</span> : ''}
            </Fragment>
        );
    }
}
