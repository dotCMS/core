import Fragment from 'stencil-fragment';
import { checkProp, getClassNames, getErrorClass, getId, getOriginalStatus, getTagError, getTagHint, updateStatus, getHintId } from '../../utils';
import flatpickr from 'flatpickr';
export class DotDateRangeComponent {
    constructor() {
        this.value = '';
        this.name = 'daterange';
        this.label = '';
        this.hint = '';
        this.max = '';
        this.min = '';
        this.required = false;
        this.requiredMessage = 'This field is required';
        this.disabled = false;
        this.displayFormat = 'Y-m-d';
        this.presets = [
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
        this.presetLabel = 'Presets';
        this.defaultPresets = [
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
    }
    reset() {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitValueChange();
    }
    valueWatch() {
        const dates = checkProp(this, 'value', 'dateRange');
        if (dates) {
            const [startDate, endDate] = dates.split(',');
            this.flatpickr.setDate([this.parseDate(startDate), this.parseDate(endDate)], false);
        }
    }
    presetsWatch() {
        this.presets = Array.isArray(this.presets) ? this.presets : this.defaultPresets;
    }
    componentWillLoad() {
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.presetsWatch();
    }
    componentDidLoad() {
        this.flatpickr = flatpickr(`#${getId(this.name)}`, {
            mode: 'range',
            altFormat: this.displayFormat,
            altInput: true,
            maxDate: this.max ? this.parseDate(this.max) : null,
            minDate: this.min ? this.parseDate(this.min) : null,
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
        return (h(Fragment, null,
            h("dot-label", { label: this.label, required: this.required, name: this.name },
                h("div", { "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, class: "dot-range__body" },
                    h("input", { class: getErrorClass(this.status.dotValid), disabled: this.isDisabled(), id: getId(this.name), required: this.required || null, type: "text", value: this.value }),
                    h("label", null,
                        this.presetLabel,
                        h("select", { disabled: this.isDisabled(), onChange: this.setPreset.bind(this) }, this.presets.map((item) => {
                            return h("option", { value: item.days }, item.label);
                        }))))),
            getTagHint(this.hint),
            getTagError(this.showErrorMessage(), this.getErrorMessage())));
    }
    parseDate(strDate) {
        const [year, month, day] = strDate.split('-');
        const newDate = new Date(parseInt(year, 10), parseInt(month, 10) - 1, parseInt(day, 10));
        return newDate;
    }
    validateProps() {
        this.valueWatch();
    }
    isDisabled() {
        return this.disabled || null;
    }
    setPreset(event) {
        const dateRange = [];
        const dt = new Date();
        dt.setDate(dt.getDate() + parseInt(event.target.value, 10));
        if (event.target.value.indexOf('-') > -1) {
            dateRange.push(dt);
            dateRange.push(new Date());
        }
        else {
            dateRange.push(new Date());
            dateRange.push(dt);
        }
        this.flatpickr.setDate(dateRange, true);
    }
    isValid() {
        return !(this.required && !(this.value && this.value.length));
    }
    isDateRangeValid(selectedDates) {
        return selectedDates && selectedDates.length === 2;
    }
    setValue(selectedDates, _dateStr, _instance) {
        this.value = this.isDateRangeValid(selectedDates)
            ? `${selectedDates[0].toISOString().split('T')[0]},${selectedDates[1].toISOString().split('T')[0]}`
            : '';
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.emitValueChange();
        this.emitStatusChange();
    }
    showErrorMessage() {
        return this.getErrorMessage() && !this.status.dotPristine;
    }
    getErrorMessage() {
        return this.isValid() ? '' : this.requiredMessage;
    }
    emitStatusChange() {
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    emitValueChange() {
        this.valueChange.emit({
            name: this.name,
            value: this.value
        });
    }
    static get is() { return "dot-date-range"; }
    static get properties() { return {
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "displayFormat": {
            "type": String,
            "attr": "display-format",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "hint": {
            "type": String,
            "attr": "hint",
            "reflectToAttr": true
        },
        "label": {
            "type": String,
            "attr": "label",
            "reflectToAttr": true
        },
        "max": {
            "type": String,
            "attr": "max",
            "reflectToAttr": true
        },
        "min": {
            "type": String,
            "attr": "min",
            "reflectToAttr": true
        },
        "name": {
            "type": String,
            "attr": "name",
            "reflectToAttr": true
        },
        "presetLabel": {
            "type": String,
            "attr": "preset-label",
            "reflectToAttr": true
        },
        "presets": {
            "type": "Any",
            "attr": "presets",
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["presetsWatch"]
        },
        "required": {
            "type": Boolean,
            "attr": "required",
            "reflectToAttr": true
        },
        "requiredMessage": {
            "type": String,
            "attr": "required-message",
            "reflectToAttr": true
        },
        "reset": {
            "method": true
        },
        "status": {
            "state": true
        },
        "value": {
            "type": String,
            "attr": "value",
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["valueWatch"]
        }
    }; }
    static get events() { return [{
            "name": "valueChange",
            "method": "valueChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "statusChange",
            "method": "statusChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return "/**style-placeholder:dot-date-range:**/"; }
}
