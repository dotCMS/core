import { Component, Prop, State } from '@stencil/core';
import { generateId } from '../../utils/utils';
import Fragment from 'stencil-fragment';

@Component({
    tag: 'dot-dropdown',
    styleUrl: 'dot-dropdown.scss'
})
export class DotDropdownComponent {
    @Prop() value: string;
    @Prop() label: string;

    @State() _value: any;
    @State() _error = false;


    componentWillLoad() {
        this._value = this.value.split(' ');
    }

    render() {
        const _label = `dotTextfield_${generateId()}`;
        return (
            <Fragment>
                <label htmlFor={_label}>{this.label}</label>
                <select id='pet-select'>
                    { this._value.map((rawItem) => {
                        const item = rawItem.split('|');
                        return (<option value={item[1]}>{item[0]}</option>);
                    })}
                </select>
            </Fragment>
        );
    }
}
