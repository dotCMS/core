import { Component, Prop, State, Event, EventEmitter } from '@stencil/core';
import { generateId, getDotOptionsFromFieldValue } from '../../utils';
import { DotOption } from '../../models/dot-option.model';
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
    @Event() onChange: EventEmitter;

    @State() _options: DotOption[];
    @State() _value: string;
    _label: string;
    _values = {};

    componentWillLoad() {
        this._options = getDotOptionsFromFieldValue(this.options);
        this._label = `dotCheckbox_${generateId()}`;
    }

    // Todo: find how to set proper TYPE in TS
    setValue(event): void {
        const checkBoxVal = event.target.value.toString();
        // Format values to be emmitted, this might change when implemented on the form
        this._values = { ...this._values, [checkBoxVal]: !this._values[checkBoxVal] };
        this.onChange.emit({ value: this._values });
    }

    render() {
        return (
            <Fragment>
                <label htmlFor={this._label}>{this.label}</label>
                {this._options.map((item: DotOption) => {
                    this._values = { ...this._values, [item.value]: this.value === item.value ? true : false };
                    return (
                        <div class='dot-checkbox__container'>
                            <input
                                type='checkbox'
                                name={item.value}
                                checked={this.value === item.value ? true : null}
                                onInput={(event: Event) => this.setValue(event)}
                                value={item.value}
                            />
                            <label htmlFor={item.value}>{item.label}</label>
                        </div>
                    );
                })}
                {this.hint ? <span class='dot-textfield__hint'>{this.hint}</span> : ''}
            </Fragment>
        );
    }
}
