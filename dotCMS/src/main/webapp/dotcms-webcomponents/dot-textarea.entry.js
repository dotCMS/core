import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-bb6a6489.js';
import { h as getOriginalStatus, c as checkProp, a as getClassNames, b as getHintId, k as getErrorClass, l as getId, e as getTagHint, f as getTagError, u as updateStatus } from './index-fca8faa0.js';
import { g as getDotAttributesFromElement, s as setDotAttributesToElement } from './index-b2e001f1.js';

const DotTextareaComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Value specifies the value of the <textarea> element */
        this.value = '';
        /** Name that will be used as ID */
        this.name = '';
        /** (optional) Text to be rendered next to <textarea> element */
        this.label = '';
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
        this.dotValueChange = createEvent(this, "dotValueChange", 7);
        this.dotStatusChange = createEvent(this, "dotStatusChange", 7);
    }
    /**
     * Reset properties of the field, clear value and emit events.
     *
     * @memberof DotTextareaComponent
     */
    async reset() {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
        this.emitValueChange();
    }
    componentWillLoad() {
        this.value = this.value || '';
        this.validateProps();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }
    componentDidLoad() {
        const htmlElement = this.el.querySelector('textarea');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(Array.from(this.el.attributes), []);
            setDotAttributesToElement(htmlElement, attrs);
        }, 0);
    }
    regexCheckWatch() {
        this.regexCheck = checkProp(this, 'regexCheck');
    }
    valueWatch() {
        this.value = this.value || '';
    }
    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);
        return (h(Host, { class: Object.assign({}, classes) }, h("dot-label", { label: this.label, required: this.required, name: this.name }, h("textarea", { "aria-describedby": getHintId(this.hint), class: getErrorClass(this.status.dotValid), id: getId(this.name), name: this.name, value: this.value, required: this.getRequiredAttr(), onInput: (event) => this.setValue(event), onBlur: () => this.blurHandler(), disabled: this.getDisabledAtt() })), getTagHint(this.hint), getTagError(this.shouldShowErrorMessage(), this.getErrorMessage())));
    }
    validateProps() {
        this.regexCheckWatch();
    }
    getDisabledAtt() {
        return this.disabled || null;
    }
    getRequiredAttr() {
        return this.required ? true : null;
    }
    isValid() {
        return !this.isValueRequired() && this.isRegexValid();
    }
    isValueRequired() {
        return this.required && !this.value.length;
    }
    isRegexValid() {
        if (this.regexCheck && this.value.length) {
            const regex = new RegExp(this.regexCheck, 'ig');
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
        "value": ["valueWatch"]
    }; }
    static get style() { return "input {\n  outline: none;\n}\n\n.dot-field__input--error {\n  border: 1px solid red;\n}"; }
};

export { DotTextareaComponent as dot_textarea };
