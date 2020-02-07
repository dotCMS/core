import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-bb6a6489.js';
import { c as checkProp, a as getClassNames, b as getHintId, e as getTagHint, f as getTagError } from './index-fca8faa0.js';
import { g as getDotAttributesFromElement, s as setDotAttributesToElement } from './index-b2e001f1.js';

const DotDateComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Value format yyyy-mm-dd  e.g., 2005-12-01 */
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
        /** (optional) Min, minimum value that the field will allow to set. Format should be yyyy-mm-dd */
        this.min = '';
        /** (optional) Max, maximum value that the field will allow to set. Format should be yyyy-mm-dd */
        this.max = '';
        /** (optional) Step specifies the legal number intervals for the input field */
        this.step = '1';
        this.dotValueChange = createEvent(this, "dotValueChange", 7);
        this.dotStatusChange = createEvent(this, "dotStatusChange", 7);
    }
    /**
     * Reset properties of the field, clear value and emit events.
     */
    async reset() {
        const input = this.el.querySelector('dot-input-calendar');
        input.reset();
    }
    componentWillLoad() {
        this.validateProps();
    }
    componentDidLoad() {
        const attrException = ['dottype'];
        const htmlElement = this.el.querySelector('input[type="date"]');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(Array.from(this.el.attributes), attrException);
            setDotAttributesToElement(htmlElement, attrs);
        }, 0);
    }
    minWatch() {
        this.min = checkProp(this, 'min', 'date');
    }
    maxWatch() {
        this.max = checkProp(this, 'max', 'date');
    }
    emitValueChange(event) {
        event.stopImmediatePropagation();
        const valueEvent = event.detail;
        this.value = valueEvent.value;
        this.dotValueChange.emit(valueEvent);
    }
    emitStatusChange(event) {
        event.stopImmediatePropagation();
        const inputCalendarStatus = event.detail;
        this.classNames = getClassNames(inputCalendarStatus.status, inputCalendarStatus.status.dotValid, this.required);
        this.setErrorMessageElement(inputCalendarStatus);
        this.dotStatusChange.emit({
            name: inputCalendarStatus.name,
            status: inputCalendarStatus.status
        });
    }
    render() {
        return (h(Host, { class: Object.assign({}, this.classNames) }, h("dot-label", { label: this.label, required: this.required, name: this.name }, h("dot-input-calendar", { "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, disabled: this.disabled, type: "date", name: this.name, value: this.value, required: this.required, min: this.min, max: this.max, step: this.step })), getTagHint(this.hint), this.errorMessageElement));
    }
    validateProps() {
        this.minWatch();
        this.maxWatch();
    }
    setErrorMessageElement(statusEvent) {
        this.errorMessageElement = getTagError(!statusEvent.status.dotValid && !statusEvent.status.dotPristine, this.getErrorMessage(statusEvent));
    }
    getErrorMessage(statusEvent) {
        return !!this.value
            ? statusEvent.isValidRange
                ? ''
                : this.validationMessage
            : this.requiredMessage;
    }
    get el() { return getElement(this); }
    static get watchers() { return {
        "min": ["minWatch"],
        "max": ["maxWatch"]
    }; }
    static get style() { return ""; }
};

export { DotDateComponent as dot_date };
