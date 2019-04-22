import { Component, Prop, State, Event, EventEmitter, Element, Method } from '@stencil/core';
import { getDotOptionsFromFieldValue } from '../../utils';
import Fragment from 'stencil-fragment';
import { DotOption } from '../../models/dot-option.model';
import { DotFieldStatus } from '../../models/dot-field-status.model';

/**
 * Represent a dotcms select control.
 *
 * @export
 * @class DotSelectComponent
 */
@Component({
    tag: 'dot-select',
    styleUrl: 'dot-select.scss'
})
export class DotSelectComponent {
    @Element() el: HTMLElement;

    @Prop() disabled = false;
    @Prop() name: string;
    @Prop() label: string;
    @Prop() hint: string;
    @Prop() options: string;
    @Prop() required: boolean;
    @Prop() requiredmessage: string;
    @Prop({ mutable: true }) value: string;

    @Event() valueChange: EventEmitter;
    @Event() statusChange: EventEmitter;

    @State() _options: DotOption[];
    @State() _valid = true;
    _dotTouched = false;
    _dotPristine = true;

    componentWillLoad() {
        this._options = getDotOptionsFromFieldValue(this.options);
        this.emitInitialValue();
        this.emitStatusChange();
    }

    hostData() {
        return {
            class: {
                'dot-valid': this.isValid(),
                'dot-invalid': !this.isValid(),
                'dot-pristine': this._dotPristine,
                'dot-dirty': !this._dotPristine,
                'dot-touched': this._dotTouched,
                'dot-untouched': !this._dotTouched
            }
        };
    }

    /**
     * Reset properties of the field, clear value and emit events.
     *
     * @memberof DotSelectComponent
     */
    @Method()
    reset(): void {
        this._dotPristine = true;
        this._dotTouched = false;
        this._valid = true;
        this.value = '';
        this.emitInitialValue();
        this.emitStatusChange();
    }

    render() {
        return (
            <Fragment>
                <label htmlFor={this.name}>{this.label}</label>
                <select
                    class={this.getClassName()}
                    id={this.name}
                    disabled={this.shouldBeDisabled()}
                    onChange={(event: Event) => this.setValue(event)}>

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
                {this.hint ? <span class='dot-field__hint'>{this.hint}</span> : ''}
                {!this.isValid() ? <span class='dot-field__error-message'>{this.requiredmessage}</span> : ''}
            </Fragment>
        );
    }

    private getClassName(): string {
        return this.isValid() ? '' : 'dot-field__select--error';
    }

    private shouldBeDisabled(): boolean {
        return this.disabled ? true : null;
    }

     // Todo: find how to set proper TYPE in TS
    private setValue(event): void {
        this._dotPristine = false;
        this._dotTouched = true;
        this.value = event.target.value;
        this.emitValueChange();
        this.emitStatusChange();
    }

    private emitInitialValue() {
        if (!this.value) {
            this.value = this._options[0].value;
            this.emitValueChange();
        }
    }

    private emitStatusChange(): void {
        this.statusChange.emit({
            name: this.name,
            status: this.getStatus()
        });
    }

    private getStatus(): DotFieldStatus {
        return {
            dotTouched: this._dotTouched,
            dotValid: this.isValid(),
            dotPristine: this._dotPristine
        };
    }

    private isValid(): boolean {
        return this.required ? !!this.value : true;
    }

    private emitValueChange(): void {
        this.valueChange.emit({ name: this.name, value: this.value });
    }
}
