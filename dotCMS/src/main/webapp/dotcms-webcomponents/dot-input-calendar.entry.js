import { r as registerInstance, c as createEvent, h, d as getElement } from './core-bb6a6489.js';
import { h as getOriginalStatus, k as getErrorClass, l as getId, u as updateStatus } from './index-fca8faa0.js';

const DotInputCalendarComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Value specifies the value of the <input> element */
        this.value = '';
        /** Name that will be used as ID */
        this.name = '';
        /** (optional) Determine if it is mandatory */
        this.required = false;
        /** (optional) Disables field's interaction */
        this.disabled = false;
        /** (optional) Min, minimum value that the field will allow to set, expect a Date Format. */
        this.min = '';
        /** (optional) Max, maximum value that the field will allow to set, expect a Date Format */
        this.max = '';
        /** (optional) Step specifies the legal number intervals for the input field */
        this.step = '1';
        /** type specifies the type of <input> element to display */
        this.type = '';
        this._dotValueChange = createEvent(this, "_dotValueChange", 7);
        this._dotStatusChange = createEvent(this, "_dotStatusChange", 7);
    }
    /**
     * Reset properties of the field, clear value and emit events.
     */
    async reset() {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitValueChange();
        this.emitStatusChange();
    }
    componentWillLoad() {
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }
    render() {
        return (h("input", { class: getErrorClass(this.status.dotValid), disabled: this.disabled || null, id: getId(this.name), onBlur: () => this.blurHandler(), onInput: (event) => this.setValue(event), required: this.required || null, type: this.type, value: this.value, min: this.min, max: this.max, step: this.step }));
    }
    isValid() {
        return this.isValueInRange() && this.isRequired();
    }
    isRequired() {
        return this.required ? !!this.value : true;
    }
    isValueInRange() {
        return this.isInMaxRange() && this.isInMinRange();
    }
    isInMinRange() {
        return !!this.min ? this.value >= this.min : true;
    }
    isInMaxRange() {
        return !!this.max ? this.value <= this.max : true;
    }
    blurHandler() {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
    }
    setValue(event) {
        this.value = event.target.value.toString();
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.emitValueChange();
        this.emitStatusChange();
    }
    emitStatusChange() {
        this._dotStatusChange.emit({
            name: this.name,
            status: this.status,
            isValidRange: this.isValueInRange()
        });
    }
    emitValueChange() {
        this._dotValueChange.emit({
            name: this.name,
            value: this.formattedValue()
        });
    }
    formattedValue() {
        return this.value.length === 5 ? `${this.value}:00` : this.value;
    }
    get el() { return getElement(this); }
    static get style() { return "dot-input-calendar {\n  display: -ms-flexbox;\n  display: flex;\n}\ndot-input-calendar input {\n  -ms-flex-positive: 1;\n  flex-grow: 1;\n}"; }
};

export { DotInputCalendarComponent as dot_input_calendar };
