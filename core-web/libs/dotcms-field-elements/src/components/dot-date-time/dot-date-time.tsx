import {
    Component,
    Element,
    Event,
    EventEmitter,
    Listen,
    Method,
    Prop,
    State,
    Watch,
    Host,
    h
} from '@stencil/core';

import { Components } from '../../components';
import {
    DotFieldStatus,
    DotFieldStatusClasses,
    DotFieldStatusEvent,
    DotFieldValueEvent,
    DotDateSlot,
    DotInputCalendarStatusEvent
} from '../../models';

import DotInputCalendar = Components.DotInputCalendar;

import { checkProp, getClassNames, getTagError, getTagHint, getHintId } from '../../utils';
import { dotParseDate } from '../../utils/props/validators';
import {
    setDotAttributesToElement,
    getDotAttributesFromElement,
    DOT_ATTR_PREFIX
} from '../dot-form/utils';

const DATE_SUFFIX = '-date';
const TIME_SUFFIX = '-time';

@Component({
    tag: 'dot-date-time',
    styleUrl: 'dot-date-time.scss'
})
export class DotDateTimeComponent {
    @Element() el: HTMLElement;

    /** Value format yyyy-mm-dd hh:mm:ss e.g., 2005-12-01 15:22:00 */
    @Prop({ mutable: true, reflect: true })
    value = '';

    /** Name that will be used as ID */
    @Prop({ reflect: true })
    name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop({ reflect: true })
    label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop({ reflect: true })
    hint = '';

    /** (optional) Determine if it is mandatory */
    @Prop({ reflect: true })
    required = false;

    /** (optional) Text that be shown when required is set and condition not met */
    @Prop({ reflect: true })
    requiredMessage = 'This field is required';

    /** (optional) Text that be shown when min or max are set and condition not met */
    @Prop({ reflect: true })
    validationMessage = "The field doesn't comply with the specified format";

    /** (optional) Disables field's interaction */
    @Prop({ reflect: true })
    disabled = false;

    /** (optional) Min, minimum value that the field will allow to set. Format should be yyyy-mm-dd hh:mm:ss | yyyy-mm-dd | hh:mm:ss */
    @Prop({ mutable: true, reflect: true })
    min = '';

    /** (optional) Max, maximum value that the field will allow to set. Format should be yyyy-mm-dd hh:mm:ss | yyyy-mm-dd | hh:mm:ss */
    @Prop({ mutable: true, reflect: true })
    max = '';

    /** (optional) Step specifies the legal number intervals for the input fields date && time e.g., 2,10 */
    @Prop({ mutable: true, reflect: true })
    step = '1,1';

    /** (optional) The string to use in the date label field */
    @Prop({ reflect: true })
    dateLabel = 'Date';

    /** (optional) The string to use in the time label field */
    @Prop({ reflect: true })
    timeLabel = 'Time';

    @State() classNames: DotFieldStatusClasses;
    @State() errorMessageElement: any;

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
    async reset(): Promise<void> {
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
    }

    @Watch('value')
    valueWatch(): void {
        this.value = checkProp(this, 'value', 'dateTime');
        this._value = dotParseDate(this.value);
    }

    @Watch('min')
    minWatch(): void {
        this.min = checkProp(this, 'min', 'dateTime');
        this._minDateTime = dotParseDate(this.min);
    }

    @Watch('max')
    maxWatch(): void {
        this.max = checkProp(this, 'max', 'dateTime');
        this._maxDateTime = dotParseDate(this.max);
    }

    @Watch('step')
    stepWatch(): void {
        this.step = checkProp(this, 'step') || '1,1';
        [this._step.date, this._step.time] = this.step.split(',');
    }

    @Listen('_valueChange')
    emitValueChange(event: CustomEvent) {
        const valueEvent: DotFieldValueEvent = event.detail;
        event.stopImmediatePropagation();
        this.formatValue(valueEvent);
        if (this.isValueComplete()) {
            this.value = this.getValue();
            this.valueChange.emit({ name: this.name, value: this.value });
        }
    }

    @Listen('_statusChange')
    emitStatusChange(event: CustomEvent) {
        const inputCalendarStatus: DotInputCalendarStatusEvent = event.detail;
        let status: DotFieldStatus;
        event.stopImmediatePropagation();
        this.setStatus(inputCalendarStatus);
        this.setErrorMessageElement(inputCalendarStatus);
        if (this.isStatusComplete()) {
            status = this.statusHandler();
            this.classNames = getClassNames(status, status.dotValid, this.required);
            this.statusChange.emit({ name: this.name, status: status });
        }
    }

    componentDidLoad(): void {
        this.setDotAttributes();
    }

    render() {
        return (
            <Host class={{ ...this.classNames }}>
                <dot-label label={this.label} required={this.required} name={this.name}>
                    <div
                        class="dot-date-time__body"
                        aria-describedby={getHintId(this.hint)}
                        tabIndex={this.hint ? 0 : null}>
                        <label>
                            {this.dateLabel}
                            <dot-input-calendar
                                disabled={this.disabled}
                                type="date"
                                name={this.name + DATE_SUFFIX}
                                value={this._value.date}
                                required={this.required}
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
                                min={this._minDateTime.time}
                                max={this._maxDateTime.time}
                                step={this._step.time}
                            />
                        </label>
                    </div>
                </dot-label>
                {getTagHint(this.hint)}
                {this.errorMessageElement}
            </Host>
        );
    }

    private setDotAttributes(): void {
        const htmlDateElement = this.el.querySelector('input[type="date"]');
        const htmlTimeElement = this.el.querySelector('input[type="time"]');
        const attrException = ['dottype', 'dotstep', 'dotmin', 'dotmax', 'dotvalue'];

        setTimeout(() => {
            let attrs: Attr[] = Array.from(this.el.attributes);
            attrs.forEach(({ name, value }) => {
                const attr = name.replace(DOT_ATTR_PREFIX, '');
                if (this[attr]) {
                    this[attr] = value;
                }
            });

            attrs = getDotAttributesFromElement(Array.from(this.el.attributes), attrException);

            setDotAttributesToElement(htmlDateElement, attrs);
            setDotAttributesToElement(htmlTimeElement, attrs);
        }, 0);
    }

    private validateProps(): void {
        this.minWatch();
        this.maxWatch();
        this.stepWatch();
        this.valueWatch();
    }

    // tslint:disable-next-line:cyclomatic-complexity
    private statusHandler(): DotFieldStatus {
        return {
            dotTouched: this._status.date.dotTouched || this._status.time.dotTouched,
            dotValid: this._status.date.dotValid && this._status.time.dotValid,
            dotPristine: this._status.date.dotPristine && this._status.time.dotPristine
        };
    }

    private formatValue(event: DotFieldValueEvent) {
        if (event.name.indexOf(DATE_SUFFIX) >= 0) {
            this._value.date = event.value as string;
        } else {
            this._value.time = event.value as string;
        }
    }

    private getValue(): string {
        return !!this._value.date && !!this._value.time
            ? `${this._value.date} ${this._value.time}`
            : '';
    }
    private setStatus(event: DotInputCalendarStatusEvent) {
        if (event.name.indexOf(DATE_SUFFIX) >= 0) {
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

    private isValid(): boolean {
        return this.isStatusComplete() ? (this.isStatusInRange() ? true : false) : true;
    }

    private isStatusInRange(): boolean {
        return this._status.time.isValidRange && this._status.date.isValidRange;
    }

    private setErrorMessageElement(statusEvent: DotInputCalendarStatusEvent): void {
        if (this.isStatusComplete()) {
            this.errorMessageElement = getTagError(
                !this.statusHandler().dotValid && !this.statusHandler().dotPristine,
                this.getErrorMessage()
            );
        } else {
            this.errorMessageElement = getTagError(
                !statusEvent.status.dotPristine,
                this.getErrorMessage()
            );
        }
    }

    private getErrorMessage(): string {
        return this.getValue()
            ? this.isValid()
                ? ''
                : this.validationMessage
            : this.requiredMessage;
    }
}
