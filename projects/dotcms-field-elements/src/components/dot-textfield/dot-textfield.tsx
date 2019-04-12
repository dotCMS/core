import { Component, Prop, State, Event, EventEmitter, Method } from '@stencil/core';
import Fragment from 'stencil-fragment';
import { DotFieldStatus } from '../../models/dot-field-status.model';

@Component({
    tag: 'dot-textfield',
    styleUrl: 'dot-textfield.scss'
})
export class DotTextfieldComponent {
    @Prop({ mutable: true }) value: string;
    @Prop() name: string;
    @Prop() regexcheck: string;
    @Prop() regexcheckmessage: string;
    @Prop() label: string;
    @Prop() hint: string;
    @Prop() placeholder: string;
    @Prop() required: boolean;
    @Prop() requiredmessage: string;
    @Prop() disabled = false;

    @Event() valueChanges: EventEmitter;
    @Event() statusChanges: EventEmitter;

    @State() _valid = true;
    @State() _dotTouched = false;
    _dotPristine = true;

    /**
     * Reset properties of the filed, clear value and emit events.
     */
    @Method()
    reset(): void {
        this._dotPristine = true;
        this._dotTouched = false;
        this.value = '';
        this._valid = true;
        this.emitStatusChange();
        this.emitValueChange();
    }

    componentWillLoad(): void {
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

    // tslint:disable-next-line:cyclomatic-complexity
    render() {
        return (
            <Fragment>
                <label>{this.label}</label>
                <input
                    class={this._valid ? '' : 'dot-field__input--error'}
                    name={this.name}
                    type="text"
                    value={this.value}
                    placeholder={this.placeholder}
                    required={this.required ? true : null}
                    onInput={(event: Event) => this.setValue(event)}
                    onBlur={() => this.blurHandler()}
                    disabled={this.disabled ? true : null}
                />
                {this.hint ? <span class="dot-field__hint">{this.hint}</span> : ''}
                {this.showErrorMessage() ? (
                    <span class="dot-field__error-meessage">{this.getErrorMessage()}</span>
                ) : (
                    ''
                )}
            </Fragment>
        );
    }

    private isValid(): boolean {
        return !this.isValueRequired() && this.isRegexValid();
    }

    private isValueRequired(): boolean {
        return this.required && !this.value.length;
    }

    private isRegexValid(): boolean {
        if (this.regexcheck && this.value.length) {
            const regex = new RegExp(this.regexcheck, 'ig');
            return regex.test(this.value);
        }
        return true;
    }

    private showErrorMessage(): boolean {
        return this.getErrorMessage() && !this._dotPristine;
    }

    private getErrorMessage(): string {
        return this.isRegexValid()
            ? this.isValid() ? '' : this.requiredmessage
            : this.regexcheckmessage;
    }

    private blurHandler(): void {
        if (!this._dotTouched) {
            this._dotTouched = true;
            this.emitStatusChange();
        }
    }

    private setValue(event): void {
        this._dotPristine = false;
        this._dotTouched = true;
        this.value = event.target.value.toString();
        this._valid = this.isValid();
        this.emitValueChange();
        this.emitStatusChange();
    }

    private emitStatusChange(): void {
        this.statusChanges.emit({
            name: this.name,
            ...this.getStatus()
        });
    }

    private emitValueChange(): void {
        this.valueChanges.emit({ name: this.name, value: this.value });
    }

    private getStatus(): DotFieldStatus {
        return {
            dotTouched: this._dotTouched,
            dotValid: this.isValid(),
            dotPristine: this._dotPristine
        };
    }
}
