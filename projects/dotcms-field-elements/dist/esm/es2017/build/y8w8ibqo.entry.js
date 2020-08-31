import { h } from '../dotcmsfields.core.js';

import { a as Fragment } from './chunk-1d89c98b.js';
import { b as checkProp, o as dotParseDate, c as getClassNames, h as getHintId, d as getTagHint, e as getTagError } from './chunk-62cd3eff.js';
import { c as DOT_ATTR_PREFIX, a as getDotAttributesFromElement, b as setDotAttributesToElement } from './chunk-4205a04e.js';

const DATE_SUFFIX = '-date';
const TIME_SUFFIX = '-time';
class DotDateTimeComponent {
    constructor() {
        this.value = '';
        this.name = '';
        this.label = '';
        this.hint = '';
        this.required = false;
        this.requiredMessage = 'This field is required';
        this.validationMessage = "The field doesn't comply with the specified format";
        this.disabled = false;
        this.min = '';
        this.max = '';
        this.step = '1,1';
        this.dateLabel = 'Date';
        this.timeLabel = 'Time';
        this._step = {
            date: null,
            time: null
        };
        this._status = {
            date: null,
            time: null
        };
    }
    reset() {
        this._status.date = null;
        this._status.time = null;
        const inputs = this.el.querySelectorAll('dot-input-calendar');
        inputs.forEach((input) => {
            input.reset();
        });
        this.valueChange.emit({ name: this.name, value: '' });
    }
    componentWillLoad() {
        this.validateProps();
    }
    valueWatch() {
        this.value = checkProp(this, 'value', 'dateTime');
        this._value = dotParseDate(this.value);
    }
    minWatch() {
        this.min = checkProp(this, 'min', 'dateTime');
        this._minDateTime = dotParseDate(this.min);
    }
    maxWatch() {
        this.max = checkProp(this, 'max', 'dateTime');
        this._maxDateTime = dotParseDate(this.max);
    }
    stepWatch() {
        this.step = checkProp(this, 'step') || '1,1';
        [this._step.date, this._step.time] = this.step.split(',');
    }
    emitValueChange(event) {
        const valueEvent = event.detail;
        event.stopImmediatePropagation();
        this.formatValue(valueEvent);
        if (this.isValueComplete()) {
            this.value = this.getValue();
            this.valueChange.emit({ name: this.name, value: this.value });
        }
    }
    emitStatusChange(event) {
        const inputCalendarStatus = event.detail;
        let status;
        event.stopImmediatePropagation();
        this.setStatus(inputCalendarStatus);
        this.setErrorMessageElement(inputCalendarStatus);
        if (this.isStatusComplete()) {
            status = this.statusHandler();
            this.classNames = getClassNames(status, status.dotValid, this.required);
            this.statusChange.emit({ name: this.name, status: status });
        }
    }
    hostData() {
        return {
            class: this.classNames
        };
    }
    componentDidLoad() {
        this.setDotAttributes();
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { label: this.label, required: this.required, name: this.name },
                h("div", { class: "dot-date-time__body", "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null },
                    h("label", null,
                        this.dateLabel,
                        h("dot-input-calendar", { disabled: this.disabled, type: "date", name: this.name + DATE_SUFFIX, value: this._value.date, required: this.required, min: this._minDateTime.date, max: this._maxDateTime.date, step: this._step.date })),
                    h("label", null,
                        this.timeLabel,
                        h("dot-input-calendar", { disabled: this.disabled, type: "time", name: this.name + TIME_SUFFIX, value: this._value.time, required: this.required, min: this._minDateTime.time, max: this._maxDateTime.time, step: this._step.time })))),
            getTagHint(this.hint),
            this.errorMessageElement));
    }
    setDotAttributes() {
        const htmlDateElement = this.el.querySelector('input[type="date"]');
        const htmlTimeElement = this.el.querySelector('input[type="time"]');
        const attrException = ['dottype', 'dotstep', 'dotmin', 'dotmax', 'dotvalue'];
        setTimeout(() => {
            let attrs = Array.from(this.el.attributes);
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
    validateProps() {
        this.minWatch();
        this.maxWatch();
        this.stepWatch();
        this.valueWatch();
    }
    statusHandler() {
        return {
            dotTouched: this._status.date.dotTouched || this._status.time.dotTouched,
            dotValid: this._status.date.dotValid && this._status.time.dotValid,
            dotPristine: this._status.date.dotPristine && this._status.time.dotPristine
        };
    }
    formatValue(event) {
        if (event.name.indexOf(DATE_SUFFIX) >= 0) {
            this._value.date = event.value;
        }
        else {
            this._value.time = event.value;
        }
    }
    getValue() {
        return !!this._value.date && !!this._value.time
            ? `${this._value.date} ${this._value.time}`
            : '';
    }
    setStatus(event) {
        if (event.name.indexOf(DATE_SUFFIX) >= 0) {
            this._status.date = event.status;
        }
        else {
            this._status.time = event.status;
        }
    }
    isValueComplete() {
        return !!this._value.time && !!this._value.date;
    }
    isStatusComplete() {
        return this._status.date && this._status.time;
    }
    isValid() {
        return this.isStatusComplete() ? (this.isStatusInRange() ? true : false) : true;
    }
    isStatusInRange() {
        return this._status.time.isValidRange && this._status.date.isValidRange;
    }
    setErrorMessageElement(statusEvent) {
        if (this.isStatusComplete()) {
            this.errorMessageElement = getTagError(!this.statusHandler().dotValid && !this.statusHandler().dotPristine, this.getErrorMessage());
        }
        else {
            this.errorMessageElement = getTagError(!statusEvent.status.dotPristine, this.getErrorMessage());
        }
    }
    getErrorMessage() {
        return !!this.getValue()
            ? this.isValid()
                ? ''
                : this.validationMessage
            : this.requiredMessage;
    }
    static get is() { return "dot-date-time"; }
    static get properties() { return {
        "classNames": {
            "state": true
        },
        "dateLabel": {
            "type": String,
            "attr": "date-label",
            "reflectToAttr": true
        },
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "errorMessageElement": {
            "state": true
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
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["maxWatch"]
        },
        "min": {
            "type": String,
            "attr": "min",
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["minWatch"]
        },
        "name": {
            "type": String,
            "attr": "name",
            "reflectToAttr": true
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
        "step": {
            "type": String,
            "attr": "step",
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["stepWatch"]
        },
        "timeLabel": {
            "type": String,
            "attr": "time-label",
            "reflectToAttr": true
        },
        "validationMessage": {
            "type": String,
            "attr": "validation-message",
            "reflectToAttr": true
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
    static get listeners() { return [{
            "name": "_valueChange",
            "method": "emitValueChange"
        }, {
            "name": "_statusChange",
            "method": "emitStatusChange"
        }]; }
    static get style() { return ".dot-date-time__body,.dot-date-time__body label{display:-ms-flexbox;display:flex}.dot-date-time__body label{-ms-flex-align:center;align-items:center;-ms-flex-positive:1;flex-grow:1;margin-right:1rem}.dot-date-time__body label:last-child{margin-right:0}.dot-date-time__body label dot-input-calendar{-ms-flex-positive:1;flex-grow:1;margin-left:.5rem}"; }
}

export { DotDateTimeComponent as DotDateTime };
