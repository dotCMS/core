import {
    Component,
    Element,
    Event,
    EventEmitter,
    Listen,
    Method,
    Prop,
    State,
    Watch
} from '@stencil/core';
import Fragment from 'stencil-fragment';
import {
    DotFieldStatus,
    DotFieldStatusClasses,
    DotFieldStatusEvent,
    DotFieldValueEvent,
    DotDateSlot
} from '../../models';
import { Components } from '../../components';
import DotInputCalendar = Components.DotInputCalendar;
import { checkProp, getClassNames, getTagError, getTagHint } from '../../utils';
import { dotParseDate } from '../../utils/props/validators';

const DATE_SUFFIX = '-date';
const TIME_SUFFIX = '-time';

@Component({
    tag: 'dot-date-time',
    styleUrl: 'dot-date-time.scss'
})
export class DotDateTimeComponent {
    @Element() el: HTMLElement;

    /** Value format yyyy-mm-dd hh:mm:ss e.g., 2005-12-01 15:22:00 */
    @Prop({ mutable: true })
    value = '';

    /** Name that will be used as ID */
    @Prop() name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop() label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop() hint = '';

    /** (optional) Determine if it is mandatory */
    @Prop() required = false;

    /** (optional) Text that be shown when required is set and condition not met */
    @Prop() requiredMessage = '';

    /** (optional) Text that be shown when min or max are set and condition not met */
    @Prop() validationMessage = '';

    /** (optional) Disables field's interaction */
    @Prop() disabled = false;

    /** (optional) Min, minimum value that the field will allow to set. Format should be yyyy-mm-dd hh:mm:ss | yyyy-mm-dd | hh:mm:ss */
    @Prop({ mutable: true })
    min = '';

    /** (optional) Max, maximum value that the field will allow to set. Format should be yyyy-mm-dd hh:mm:ss | yyyy-mm-dd | hh:mm:ss */
    @Prop({ mutable: true })
    max = '';

    /** (optional) Step specifies the legal number intervals for the input fields date && time e.g., 2,10 */
    @Prop({ mutable: true })
    step = '1,1';

    /** (optional) The string to use in the date label field */
    @Prop() dateLabel = 'Date';

    /** (optional) The string to use in the time label field */
    @Prop() timeLabel = 'Time';

    @State() classNames: DotFieldStatusClasses;
    @State() errorMessageElement: JSX.Element;

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    private _minDateTime: DotDateSlot;
    private _maxDateTime: DotDateSlot;
    private _value: DotDateSlot;
    private _step = {
        date: null,
        time: null
    };
    private _status = {
        date: null,
        time: null
    };

    /**
     * Reset properties of the filed, clear value and emit events.
     */
    @Method()
    reset(): void {
        this._status.date = null;
        this._status.time = null;
        const inputs = this.el.querySelectorAll('dot-input-calendar');
        inputs.forEach((input: DotInputCalendar) => {
            input.reset();
        });
        this.valueChange.emit({ name: this.name, value: '' });
    }

    componentWillLoad(): void {
        this.validateProps();
        this.setDatesFormat();
        [this._step.date, this._step.time] = this.step.split(',');
    }

    @Watch('value')
    valueWatch(): void {
        this.value = checkProp(this, 'value', 'dateTime');
    }

    @Watch('min')
    minWatch(): void {
        this.min = checkProp(this, 'min', 'dateTime');
    }

    @Watch('max')
    maxWatch(): void {
        this.max = checkProp(this, 'max', 'dateTime');
    }

    @Watch('step')
    stepWatch(): void {
        this.step = checkProp(this, 'step');
    }

    @Listen('_valueChange')
    emitValueChange(event: CustomEvent) {
        const valueEvent: DotFieldValueEvent = event.detail;
        event.stopImmediatePropagation();
        this.setValue(valueEvent);
        if (this.isValueComplete()) {
            this.valueChange.emit({ name: this.name, value: this.getValue() });
        }
    }

    @Listen('_statusChange')
    emitStatusChange(event: CustomEvent) {
        const statusEvent: DotFieldStatusEvent = event.detail;
        let status: DotFieldStatus;
        event.stopImmediatePropagation();
        this.setStatus(statusEvent);
        if (this.isStatusComplete()) {
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

    hostData() {
        return {
            class: this.classNames
        };
    }

    render() {
        return (
            <Fragment>
                <dot-label label={this.label} required={this.required} name={this.name}>
                    <label>
                        {this.dateLabel}
                        <dot-input-calendar
                            disabled={this.disabled}
                            type="date"
                            name={this.name + DATE_SUFFIX}
                            value={this._value.date}
                            required={this.required}
                            required-message={this.requiredMessage}
                            validation-message={this.validationMessage}
                            min={this._minDateTime.date}
                            max={this._maxDateTime.date}
                            step={this._step.date}
                        />
                    </label>
                    <label>
                        {this.timeLabel}
                        <dot-input-calendar
                            disabled={this.disabled}
                            type="time"
                            name={this.name + TIME_SUFFIX}
                            value={this._value.time}
                            required={this.required}
                            required-message={this.requiredMessage}
                            validation-message={this.validationMessage}
                            min={this._minDateTime.time}
                            max={this._maxDateTime.time}
                            step={this._step.time}
                        />
                    </label>
                </dot-label>
                {getTagHint(this.hint, this.name)}
                {this.errorMessageElement}
            </Fragment>
        );
    }

    private validateProps(): void {
        this.minWatch();
        this.maxWatch();
        this.stepWatch();
        this.valueWatch();
    }

    private setDatesFormat(): void {
        this._minDateTime = dotParseDate(this.min);
        this._maxDateTime = dotParseDate(this.max);
        this._value = dotParseDate(this.value);
    }

    // tslint:disable-next-line:cyclomatic-complexity
    private statusHandler(): DotFieldStatus {
        return {
            dotTouched: this._status.date.dotTouched || this._status.time.dotTouched,
            dotValid: this._status.date.dotValid && this._status.time.dotValid,
            dotPristine: this._status.date.dotPristine && this._status.time.dotPristine
        };
    }

    private getValue(): string {
        return this._value.date && this._value.time
            ? `${this._value.date} ${this._value.time}`
            : '';
    }

    private setValue(event: DotFieldValueEvent) {
        if (event.name.indexOf(DATE_SUFFIX) > 0) {
            this._value.date = event.value;
        } else {
            this._value.time = event.value;
        }
    }

    private setStatus(event: DotFieldStatusEvent) {
        if (event.name.indexOf(DATE_SUFFIX) > 0) {
            this._status.date = event.status;
        } else {
            this._status.time = event.status;
        }
    }

    private isValueComplete(): boolean {
        return !!this._value.time && !!this._value.date;
    }

    private isStatusComplete(): boolean {
        return this._status.date && this._status.time;
    }
}
