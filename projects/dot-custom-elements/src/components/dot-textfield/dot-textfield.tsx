import { Component, Prop, State, Event, EventEmitter } from '@stencil/core';
import { generateId } from '../../utils/utils';
import Fragment from 'stencil-fragment';

@Component({
    tag: 'dot-textfield',
    styleUrl: 'dot-textfield.scss'
})
export class DotTextfieldComponent {
    @Prop() value: string;
    @Prop() regexCheck: string;
    @Prop() readOnly: string;
    @Prop() label: string;
    @Prop() hint: string;
    @Prop() placeholder: string;
    @Prop() required: boolean;
    @Event() onCallback: EventEmitter;

    @State() _value: string;

    setValue(event: Event) {
        const value = event.target.value.toString();
        // todo: make regex work!
        const re1 = new RegExp(this.regexCheck);
        console.log(re1.test(value));
        this.onCallback.emit({ value });
    }

    render() {
        this._value = this.value;
        const _label = `dotTextfield_${generateId()}`;

        return (
            <Fragment>
                <label htmlFor={_label}>{this.label}</label>
                <input
                    name={_label}
                    type='text'
                    value={this._value}
                    placeholder={this.placeholder}
                    onInput={(event: Event) => this.setValue(event)}
                />
                {this.hint ? <span class='hint'>{this.hint}</span> : ''}
            </Fragment>
        );
    }
}
