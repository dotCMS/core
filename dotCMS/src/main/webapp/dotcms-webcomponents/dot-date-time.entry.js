import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-5e49af37.js';
import { c as checkProp, m as dotParseDate, b as getClassNames, d as getHintId, f as getTagHint, h as getTagError } from './index-d52678cd.js';
import { D as DOT_ATTR_PREFIX, g as getDotAttributesFromElement, s as setDotAttributesToElement } from './index-e11d3040.js';

const DATE_SUFFIX = '-date';
const TIME_SUFFIX = '-time';
const DotDateTimeComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Value format yyyy-mm-dd hh:mm:ss e.g., 2005-12-01 15:22:00 */
        this.value = '';
        /** Name that will be used as ID */
        this.name = '';
        /** (optional) Text to be rendered next to input field */
        this.label = '';
        /** (optional) Hint text that suggest a clue of the field */
        this.hint = '';
        /** (optional) Determine if it is mandatory */
        this.required = false;
        /** (optional) Text that be shown when required is set and condition not met */
        this.requiredMessage = 'This field is required';
        /** (optional) Text that be shown when min or max are set and condition not met */
        this.validationMessage = "The field doesn't comply with the specified format";
        /** (optional) Disables field's interaction */
        this.disabled = false;
        /** (optional) Min, minimum value that the field will allow to set. Format should be yyyy-mm-dd hh:mm:ss | yyyy-mm-dd | hh:mm:ss */
        this.min = '';
        /** (optional) Max, maximum value that the field will allow to set. Format should be yyyy-mm-dd hh:mm:ss | yyyy-mm-dd | hh:mm:ss */
        this.max = '';
        /** (optional) Step specifies the legal number intervals for the input fields date && time e.g., 2,10 */
        this.step = '1,1';
        /** (optional) The string to use in the date label field */
        this.dateLabel = 'Date';
        /** (optional) The string to use in the time label field */
        this.timeLabel = 'Time';
        this._step = {
            date: null,
            time: null
        };
        this._status = {
            date: null,
            time: null
        };
        this.dotValueChange = createEvent(this, "dotValueChange", 7);
        this.dotStatusChange = createEvent(this, "dotStatusChange", 7);
    }
    /**
     * Reset properties of the filed, clear value and emit events.
     */
    async reset() {
        this._status.date = null;
        this._status.time = null;
        const inputs = this.el.querySelectorAll('dot-input-calendar');
        inputs.forEach((input) => {
            input.reset();
        });
        this.dotValueChange.emit({ name: this.name, value: '' });
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
            this.dotValueChange.emit({ name: this.name, value: this.value });
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
            this.dotStatusChange.emit({ name: this.name, status: status });
        }
    }
    componentDidLoad() {
        this.setDotAttributes();
    }
    render() {
        return (h(Host, { class: Object.assign({}, this.classNames) }, h("dot-label", { label: this.label, required: this.required, name: this.name }, h("div", { class: "dot-date-time__body", "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null }, h("label", null, this.dateLabel, h("dot-input-calendar", { disabled: this.disabled, type: "date", name: this.name + DATE_SUFFIX, value: this._value.date, required: this.required, min: this._minDateTime.date, max: this._maxDateTime.date, step: this._step.date })), h("label", null, this.timeLabel, h("dot-input-calendar", { disabled: this.disabled, type: "time", name: this.name + TIME_SUFFIX, value: this._value.time, required: this.required, min: this._minDateTime.time, max: this._maxDateTime.time, step: this._step.time })))), getTagHint(this.hint), this.errorMessageElement));
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
    // tslint:disable-next-line:cyclomatic-complexity
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
    get el() { return getElement(this); }
    static get watchers() { return {
        "value": ["valueWatch"],
        "min": ["minWatch"],
        "max": ["maxWatch"],
        "step": ["stepWatch"]
    }; }
    static get style() { return ".dot-date-time__body {\n  display: -ms-flexbox;\n  display: flex;\n}\n.dot-date-time__body label {\n  -ms-flex-align: center;\n  align-items: center;\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-positive: 1;\n  flex-grow: 1;\n  margin-right: 1rem;\n}\n.dot-date-time__body label:last-child {\n  margin-right: 0;\n}\n.dot-date-time__body label dot-input-calendar {\n  -ms-flex-positive: 1;\n  flex-grow: 1;\n  margin-left: 0.5rem;\n}"; }
};

export { DotDateTimeComponent as dot_date_time };
