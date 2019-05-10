import {
    Component,
    Element,
    Event,
    EventEmitter,
    Listen,
    Method,
    Prop,
    State
} from '@stencil/core';
import Fragment from 'stencil-fragment';
import {
    DotFieldStatus,
    DotFieldStatusClasses,
    DotFieldStatusEvent,
    DotFieldValueEvent,
    DotLabel
} from '../../models';
import { Components } from '../../components';
import DotInputCalendar = Components.DotInputCalendar;
import { getClassNames, getTagError, getTagHint, getTagLabel } from '../../utils';

const DATE_REGEX = new RegExp('(19|20)\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])');
const TIME_REGEX = new RegExp('^(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])$');

interface FormattedDate {
    date: string;
    time: string;
}

@Component({
    tag: 'dot-date-time',
    styleUrl: 'dot-date-time.scss'
})
export class DotDateTimeComponent {
    @Element() el: HTMLElement;

    /** Value should be year-month-day hour:minute:second e.g., 2005-12-01 15:22:00 */
    @Prop({ mutable: true })
    value = '';

    /** Name that will be used as ID */
    @Prop() name: string;

    /** (optional) Text to be rendered next to input field */
    @Prop() label: string;

    /** (optional) Hint text that suggest a clue of the field */
    @Prop() hint: string;

    /** (optional) Determine if it is needed */
    @Prop() required: boolean;

    /** (optional) Text that be shown when required is set and condition not met */
    @Prop() requiredMessage: string;

    /** (optional) Text that be shown when min or max are set and condition not met */
    @Prop() validationMessage: string;

    /** (optional) Disables field's interaction */
    @Prop() disabled = false;

    /** (optional) Min value that the field will allow to set. Format should be year-month-day hour:minute:second | year-month-day | hour:minute:second */
    @Prop() min: string;

    /** (optional) Max value that the field will allow to set. Format should be year-month-day hour:minute:second | year-month-day | hour:minute:second */
    @Prop() max: string;

    /** (optional) Step that are indicated for the date and time input's separates by a comma (2,10) */
    @Prop() step: string;

    @State() classNames: DotFieldStatusClasses;
    @State() errorMessageElement: JSX.Element;

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    private _minDateTime: FormattedDate;
    private _maxDateTime: FormattedDate;
    private _value: FormattedDate;
    private _dateStatus: DotFieldStatus;
    private _timeStatus: DotFieldStatus;
    private _dateStep: string;
    private _timeStep: string;

    /**
     * Reset properties of the filed, clear value and emit events.
     */
    @Method()
    reset(): void {
        this._dateStatus = null;
        this._timeStatus = null;
        const inputs = this.el.querySelectorAll('dot-input-calendar');
        inputs.forEach((input: DotInputCalendar) => {
            input.reset();
        });
        this.valueChange.emit({ name: this.name, value: '' });
    }

    @Listen('_valueChange')
    emitValueChange(event: CustomEvent) {
        const valueEvent: DotFieldValueEvent = event.detail;
        event.stopImmediatePropagation();

        if (valueEvent.name.indexOf('-date') > 0) {
            this._value.date = valueEvent.value;
        } else {
            this._value.time = valueEvent.value;
        }
        if (!!this._value.time && !!this._value.date) {
            this.valueChange.emit({ name: this.name, value: this.getValue() });
        }
    }

    @Listen('_statusChange')
    emitStatusChange(event: CustomEvent) {
        const statusEvent: DotFieldStatusEvent = event.detail;
        let status: DotFieldStatus;
        event.stopImmediatePropagation();

        if (statusEvent.name.indexOf('-date') > 0) {
            this._dateStatus = statusEvent.status;
        } else {
            this._timeStatus = statusEvent.status;
        }
        if (this._dateStatus && this._timeStatus) {
            status = this.statusHandler();
            this.classNames = getClassNames(status, status.dotValid, this.required);
            this.statusChange.emit({ name: this.name, status: status });
        }
    }

    @Listen('_errorMessage')
    showErrorElement(event: CustomEvent) {
        event.stopImmediatePropagation();
        this.errorMessageElement = getTagError(event.detail.show, this.validationMessage);
    }

    componentWillLoad() {
        this.setDatesFormat();
        [this._dateStep, this._timeStep] = this.step.split(',');
    }

    hostData() {
        return {
            class: this.classNames
        };
    }

    render() {
        const labelTagParams: DotLabel = {
            name: this.name,
            label: this.label,
            required: this.required
        };
        return (
            <Fragment>
                {getTagLabel(labelTagParams)}
                <dot-input-calendar
                    disabled={this.disabled}
                    type="date"
                    name={this.name + '-date'}
                    hint={this.hint}
                    value={this._value.date}
                    required={this.required}
                    required-message={this.requiredMessage}
                    validation-message={this.validationMessage}
                    min={this._minDateTime.date}
                    max={this._maxDateTime.date}
                    step={this._dateStep}
                />
                <dot-input-calendar
                    disabled={this.disabled}
                    type="time"
                    name={this.name + '-time'}
                    hint={this.hint}
                    value={this._value.time}
                    required={this.required}
                    required-message={this.requiredMessage}
                    validation-message={this.validationMessage}
                    min={this._minDateTime.time}
                    max={this._maxDateTime.time}
                    step={this._timeStep}
                />
                {getTagHint(this.hint)}
                {this.errorMessageElement}
            </Fragment>
        );
    }

    private setDatesFormat(): void {
        this._minDateTime = this.parseDate(this.min);
        this._maxDateTime = this.parseDate(this.max);
        this._value = this.parseDate(this.value);
    }

    private parseDate(data: string): FormattedDate {
        const [dateOrTime, time] = data.split(' ');
        return {
            date: this.validateDate(dateOrTime),
            time: this.validateTime(time) || this.validateTime(dateOrTime)
        };
    }

    private validateDate(date: string): string {
        return DATE_REGEX.test(date) ? date : null;
    }

    private validateTime(time: string): string {
        return TIME_REGEX.test(time) ? time : null;
    }

    private statusHandler(): DotFieldStatus {
        return {
            dotTouched: this._dateStatus.dotTouched || this._timeStatus.dotTouched,
            dotValid: this._dateStatus.dotValid && this._timeStatus.dotValid,
            dotPristine: this._dateStatus.dotPristine && this._timeStatus.dotPristine
        };
    }

    private getValue(): string {
        return this._value.date && this._value.time
            ? `${this._value.date} ${this._value.time}`
            : '';
    }
}
