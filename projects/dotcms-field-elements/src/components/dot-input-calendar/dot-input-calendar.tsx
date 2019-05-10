import { Component, Element, Event, EventEmitter, Method, Prop, State } from '@stencil/core';
import Fragment from 'stencil-fragment';
import { DotFieldStatus, DotFieldStatusEvent, DotFieldValueEvent } from '../../models';
import { getErrorClass, getOriginalStatus, updateStatus } from '../../utils';

@Component({
    tag: 'dot-input-calendar',
    styleUrl: 'dot-input-calendar.scss'
})
export class DotInputCalendarComponent {
    @Element() el: HTMLElement;
    @Prop({ mutable: true })
    value: string;
    @Prop() name: string;
    @Prop() label: string;
    @Prop() hint: string;
    @Prop() required: boolean;
    @Prop() requiredMessage: string;
    @Prop() validationMessage: string;
    @Prop() disabled = false;
    @Prop() min: string;
    @Prop() max: string;
    @Prop() step: string;
    @Prop() type: string;

    @State() status: DotFieldStatus;
    @Event() _valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() _statusChange: EventEmitter<DotFieldStatusEvent>;
    @Event() _errorMessage: EventEmitter;
    /**
     * Reset properties of the field, clear value and emit events.
     */
    @Method()
    reset(): void {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitValueChange();
    }

    componentWillLoad(): void {
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitErrorMessage();
    }

    componentWillUpdate(): void {
        this.emitErrorMessage();
    }

    render() {
        return (
            <Fragment>
                <input
                    class={getErrorClass(this.status.dotValid)}
                    disabled={this.disabled || null}
                    id={this.name}
                    onBlur={() => this.blurHandler()}
                    onInput={(event: Event) => this.setValue(event)}
                    required={this.required || null}
                    type={this.type}
                    value={this.value}
                    min={this.min}
                    max={this.max}
                    step={this.step}
                />
            </Fragment>
        );
    }

    private isValid(): boolean {
        return this.isValueInRange() && this.isRequired();
    }

    private isRequired(): boolean {
        return this.required ? !!this.value : true;
    }

    private isValueInRange(): boolean {
        return this.isInMaxRange() && this.isInMinRange();
    }

    private isInMinRange(): boolean {
        return !!this.min ? this.value >= this.min : true;
    }

    private isInMaxRange(): boolean {
        return !!this.max ? this.value <= this.max : true;
    }

    private showErrorMessage(): boolean {
        return !this.status.dotPristine && !!this.getErrorMessage();
    }

    private getErrorMessage(): string {
        return this.isValueInRange()
            ? this.isRequired() ? '' : this.requiredMessage
            : this.validationMessage;
    }

    private blurHandler(): void {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
    }

    private setValue(event): void {
        this.value = event.target.value.toString();
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.emitValueChange();
        this.emitStatusChange();
    }

    private emitErrorMessage(): void {
        this._errorMessage.emit({
            show: this.showErrorMessage(),
            message: this.getErrorMessage()
        });
    }

    private emitStatusChange(): void {
        this._statusChange.emit({
            name: this.name,
            status: this.status
        });
    }

    private emitValueChange(): void {
        this._valueChange.emit({
            name: this.name,
            value: this.value
        });
    }
}
