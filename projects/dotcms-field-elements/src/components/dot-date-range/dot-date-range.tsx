import { Component, Prop, State, Element, Event, EventEmitter, Method, Watch } from '@stencil/core';
import Fragment from 'stencil-fragment';
import { DotFieldStatus, DotFieldValueEvent, DotFieldStatusEvent } from '../../models';
import {
    checkProp,
    getClassNames,
    getErrorClass,
    getId,
    getOriginalStatus,
    getTagError,
    getTagHint,
    updateStatus
} from '../../utils';
import flatpickr from 'flatpickr';

@Component({
    tag: 'dot-date-range',
    styleUrl: 'dot-date-range.scss'
})
export class DotDateRangeComponent {
    @Element() el: HTMLElement;

    /** Value formatted with start and end date splitted with a comma */
    @Prop({ mutable: true }) value = '';

    /** Name that will be used as ID */
    @Prop() name = '';

    /** (optional) Text to be rendered next to input field */
    @Prop() label = '';

    /** (optional) Hint text that suggest a clue of the field */
    @Prop() hint = '';

    /** (optional) Max value that the field will allow to set */
    @Prop() max = '';

    /** (optional) Min value that the field will allow to set */
    @Prop() min = '';

    /** (optional) Determine if it is needed */
    @Prop() required = false;

    /** (optional) Text that be shown when required is set and condition not met */
    @Prop() requiredMessage = '';

    /** (optional) Disables field's interaction */
    @Prop() disabled = false;

    /** (optional) Date format used by the field on every operation */
    @Prop() dateFormat = 'Y-m-d';

    /** (optional) Array of date presets formatted as [{ label: 'PRESET_LABEL', days: NUMBER }] */
    @Prop() presets = [
        {
            label: 'Date Presets',
            days: 0
        },
        {
            label: 'Last Week',
            days: -7
        },
        {
            label: 'Next Week',
            days: 7
        },
        {
            label: 'Last Month',
            days: -30
        },
        {
            label: 'Next Month',
            days: 30
        }
    ];

    @Prop() presetLabel = 'Presets';

    @State() status: DotFieldStatus;

    @Event() valueChange: EventEmitter<DotFieldValueEvent>;
    @Event() statusChange: EventEmitter<DotFieldStatusEvent>;

    private fp: any;

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

    @Watch('value')
    valueWatch(): void {
        if (this.value) {
            const dates = checkProp<DotDateRangeComponent, string>(this, 'value', 'string');
            const [startDate, endDate] = dates.split(',');
            this.fp.setDate([new Date(startDate), new Date(endDate)], true);
        }
    }

    componentWillLoad(): void {
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }

    componentDidLoad(): void {
        this.fp = flatpickr(`#${getId(this.name)}`, {
            mode: 'range',
            dateFormat: this.dateFormat,
            maxDate: this.max,
            minDate: this.min,
            onChange: this.setValue.bind(this)
        });
        this.validateProps();
    }

    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }

    render() {
        return (
            <Fragment>
                <dot-label label={this.label} required={this.required} name={this.name}>
                    <input
                        class={getErrorClass(this.status.dotValid)}
                        disabled={this.isDisabled()}
                        id={getId(this.name)}
                        required={this.required || null}
                        type="text"
                        value={this.value}
                    />
                    <label>
                        {this.presetLabel}
                        <select disabled={this.isDisabled()} onChange={this.setPreset.bind(this)}>
                            {this.presets.map((item) => {
                                return <option value={item.days}>{item.label}</option>;
                            })}
                        </select>
                    </label>
                </dot-label>
                {getTagHint(this.hint, this.name)}
                {getTagError(this.showErrorMessage(), this.getErrorMessage())}
            </Fragment>
        );
    }

    private validateProps(): void {
        this.valueWatch();
    }

    private isDisabled(): boolean {
        return this.disabled || null;
    }

    private setPreset(event) {
        const dateRange = [];
        const dt = new Date();
        dt.setDate(dt.getDate() + parseInt(event.target.value, 10));

        if (event.target.value.indexOf('-') > -1) {
            dateRange.push(dt);
            dateRange.push(new Date());
        } else {
            dateRange.push(new Date());
            dateRange.push(dt);
        }

        this.fp.setDate(dateRange, true);
    }

    private isValid(): boolean {
        return !(this.required && !(this.value && this.value.length));
    }

    private isDateRangeValid(selectedDates: Date[]): boolean {
        return selectedDates && selectedDates.length === 2;
    }

    private setValue(selectedDates: Date[], _dateStr: string, _instance): void {
        this.value = this.isDateRangeValid(selectedDates)
            ? `${selectedDates[0].toISOString().split('T')[0]},${
                  selectedDates[1].toISOString().split('T')[0]
              }`
            : '';
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.emitValueChange();
        this.emitStatusChange();
    }

    private showErrorMessage(): boolean {
        return this.getErrorMessage() && !this.status.dotPristine;
    }

    private getErrorMessage(): string {
        return this.isValid() ? '' : this.requiredMessage;
    }

    private emitStatusChange(): void {
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }

    private emitValueChange(): void {
        this.valueChange.emit({
            name: this.name,
            value: this.value
        });
    }
}
