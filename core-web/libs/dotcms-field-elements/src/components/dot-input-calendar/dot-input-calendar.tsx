import {
    Component,
    Element,
    Event,
    EventEmitter,
    Method,
    Prop,
    State,
    Host,
    h
} from '@stencil/core';

import { DotFieldStatus, DotFieldValueEvent, DotInputCalendarStatusEvent } from '../../models';
import { getErrorClass, getId, getOriginalStatus, updateStatus } from '../../utils';

@Component({
    tag: 'dot-input-calendar',
    styleUrl: 'dot-input-calendar.scss'
})
export class DotInputCalendarComponent {
    @Element() el: HTMLElement;

    /** Value specifies the value of the <input> element */
    @Prop({ mutable: true, reflect: true })
    value = '';

    /** Name that will be used as ID */
    @Prop({ reflect: true })
    name = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ reflect: true })
    required = false;

    /** (optional) Disables field's interaction */
    @Prop({ reflect: true })
    disabled = false;

    /** (optional) Min, minimum value that the field will allow to set, expect a Date Format. */
    @Prop({ reflect: true })
    min = '';

    /** (optional) Max, maximum value that the field will allow to set, expect a Date Format */
    @Prop({ reflect: true })
    max = '';

    /** (optional) Step specifies the legal number intervals for the input field */
    @Prop({ reflect: true })
    step = '1';

    /** type specifies the type of <input> element to display */
    @Prop({ reflect: true })
    type = '';

    @State() status: DotFieldStatus;
    @Event() _valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() _statusChange: EventEmitter<DotInputCalendarStatusEvent>;

    /**
     * Reset properties of the field, clear value and emit events.
     */
    @Method()
    async reset(): Promise<void> {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitValueChange();
        this.emitStatusChange();
    }

    componentWillLoad(): void {
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }

    render() {
        return (
            <Host>
                <input
                    class={getErrorClass(this.status.dotValid)}
                    disabled={this.disabled || null}
                    id={getId(this.name)}
                    onBlur={() => this.blurHandler()}
                    onInput={(event: Event) => this.setValue(event)}
                    required={this.required || null}
                    type={this.type}
                    value={this.value}
                    min={this.min}
                    max={this.max}
                    step={this.step}
                />
            </Host>
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
        return this.min ? this.value >= this.min : true;
    }

    private isInMaxRange(): boolean {
        return this.max ? this.value <= this.max : true;
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

    private emitStatusChange(): void {
        this._statusChange.emit({
            name: this.name,
            status: this.status,
            isValidRange: this.isValueInRange()
        });
    }

    private emitValueChange(): void {
        this._valueChange.emit({
            name: this.name,
            value: this.formattedValue()
        });
    }

    private formattedValue(): string {
        return this.value.length === 5 ? `${this.value}:00` : this.value;
    }
}
