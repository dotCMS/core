import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-5e49af37.js';
import { g as getOriginalStatus, c as checkProp, b as getClassNames, d as getHintId, e as getErrorClass, a as getId, f as getTagHint, h as getTagError, u as updateStatus } from './index-d52678cd.js';
import { g as getDotAttributesFromElement, s as setDotAttributesToElement } from './index-e11d3040.js';

const DotTextfieldComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Value specifies the value of the <input> element */
        this.value = '';
        /** Name that will be used as ID */
        this.name = '';
        /** (optional) Text to be rendered next to input field */
        this.label = '';
        /** (optional) Placeholder specifies a short hint that describes the expected value of the input field */
        this.placeholder = '';
        /** (optional) Hint text that suggest a clue of the field */
        this.hint = '';
        /** (optional) Determine if it is mandatory */
        this.required = false;
        /** (optional) Text that be shown when required is set and condition not met */
        this.requiredMessage = 'This field is required';
        /** (optional) Text that be shown when the Regular Expression condition not met */
        this.validationMessage = "The field doesn't comply with the specified format";
        /** (optional) Disables field's interaction */
        this.disabled = false;
        /** (optional) Regular expresion that is checked against the value to determine if is valid  */
        this.regexCheck = '';
        /** type specifies the type of <input> element to display */
        this.type = 'text';
        this.dotValueChange = createEvent(this, "dotValueChange", 7);
        this.dotStatusChange = createEvent(this, "dotStatusChange", 7);
    }
    /**
     * Reset properties of the field, clear value and emit events.
     */
    async reset() {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitValueChange();
    }
    componentWillLoad() {
        this.validateProps();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }
    componentDidLoad() {
        const htmlElement = this.el.querySelector('input');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(Array.from(this.el.attributes), []);
            setDotAttributesToElement(htmlElement, attrs);
        }, 0);
    }
    regexCheckWatch() {
        this.regexCheck = checkProp(this, 'regexCheck');
    }
    typeWatch() {
        this.type = checkProp(this, 'type');
    }
    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);
        return (h(Host, { class: Object.assign({}, classes) }, h("dot-label", { label: this.label, required: this.required, name: this.name }, h("input", { "aria-describedby": getHintId(this.hint), class: getErrorClass(this.status.dotValid), disabled: this.disabled || null, id: getId(this.name), onBlur: () => this.blurHandler(), onInput: (event) => this.setValue(event), placeholder: this.placeholder, required: this.required || null, type: this.type, value: this.value })), getTagHint(this.hint), getTagError(this.shouldShowErrorMessage(), this.getErrorMessage())));
    }
    validateProps() {
        this.regexCheckWatch();
        this.typeWatch();
    }
    isValid() {
        return !this.isValueRequired() && this.isRegexValid();
    }
    isValueRequired() {
        return this.required && !this.value;
    }
    isRegexValid() {
        if (this.regexCheck && this.value) {
            const regex = new RegExp(this.regexCheck);
            return regex.test(this.value);
        }
        return true;
    }
    shouldShowErrorMessage() {
        return this.getErrorMessage() && !this.status.dotPristine;
    }
    getErrorMessage() {
        return this.isRegexValid()
            ? this.isValid()
                ? ''
                : this.requiredMessage
            : this.validationMessage;
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
        this.dotStatusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    emitValueChange() {
        this.dotValueChange.emit({
            name: this.name,
            value: this.value
        });
    }
    get el() { return getElement(this); }
    static get watchers() { return {
        "regexCheck": ["regexCheckWatch"],
        "type": ["typeWatch"]
    }; }
    static get style() { return "input {\n  outline: none;\n}"; }
};

export { DotTextfieldComponent as dot_textfield };
