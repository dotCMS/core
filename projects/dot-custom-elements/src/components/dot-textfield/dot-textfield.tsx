import { Component, Prop, State, Event, EventEmitter } from '@stencil/core';
import { generateId } from '../../utils/utils';
import Fragment from 'stencil-fragment';

@Component({
    tag: 'dot-textfield',
    styleUrl: 'dot-textfield.scss'
})
export class DotTextfieldComponent {
    @Prop() value: string;
    @Prop() regexcheck: string;
    @Prop() readOnly: string;
    @Prop() label: string;
    @Prop() hint: string;
    @Prop() placeholder: string;
    @Prop() required: boolean;
    @Event() onCallback: EventEmitter;

    @State() _value: string;
    @State() _error = false;

    // tslint:disable-next-line:cyclomatic-complexity
    validate(value: string): boolean {
        if (this.required && value.length === 0) {
            return true;
        } else if (this.regexcheck) {
            const regex = new RegExp(this.regexcheck, 'ig');
            return !regex.test(value);
        }
        return false;
    }

    setValue(event): void {
        this._value = event.target.value.toString();
        this._error = this.validate(this._value);
        this.onCallback.emit({ error: this._error, value: this._value });
    }

    componentWillLoad() {
        this._value = this._value && this._value.length > -1 ? this._value : this.value;
    }

    // tslint:disable-next-line:cyclomatic-complexity
    render() {
        const _label = `dotTextfield_${generateId()}`;
        return (
            <Fragment>
                <label htmlFor={_label}>{this.label}</label>
                <input
                    class={this._error ? 'dot-textfield__input--error' : ''}
                    name={_label}
                    type='text'
                    value={this._value}
                    placeholder={this.placeholder}
                    required={this.required ? true : null}
                    onInput={(event: Event) => this.setValue(event)}
                />
                {this.hint ? <span class='hint'>{this.hint}</span> : ''}
            </Fragment>
        );
    }
}
