import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-5e49af37.js';
import { g as getOriginalStatus, c as checkProp, k as getDotOptionsFromFieldValue, b as getClassNames, d as getHintId, e as getErrorClass, a as getId, f as getTagHint, h as getTagError, u as updateStatus } from './index-d52678cd.js';
import { g as getDotAttributesFromElement, s as setDotAttributesToElement } from './index-e11d3040.js';

const DotRadioComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Value set from the ratio option */
        this.value = '';
        /** Name that will be used as ID */
        this.name = '';
        /** (optional) Text to be rendered next to input field */
        this.label = '';
        /** (optional) Hint text that suggest a clue of the field */
        this.hint = '';
        /** (optional) Determine if it is mandatory */
        this.required = false;
        /** (optional) Disables field's interaction */
        this.disabled = false;
        /** (optional) Text that will be shown when required is set and condition is not met */
        this.requiredMessage = '';
        /** Value/Label ratio options separated by comma, to be formatted as: Value|Label */
        this.options = '';
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
        this.value = this.value || '';
        this.validateProps();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }
    componentDidLoad() {
        const attrException = ['dottype'];
        const htmlElements = this.el.querySelectorAll('input[type="radio"]');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(Array.from(this.el.attributes), attrException);
            htmlElements.forEach((htmlElement) => {
                setDotAttributesToElement(htmlElement, attrs);
            });
        }, 0);
    }
    optionsWatch() {
        const validOptions = checkProp(this, 'options');
        this._options = getDotOptionsFromFieldValue(validOptions);
    }
    valueWatch() {
        this.value = this.value || '';
    }
    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);
        return (h(Host, { class: Object.assign({}, classes) }, h("dot-label", { label: this.label, required: this.required, name: this.name }, h("div", { class: "dot-radio__items", "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, role: "radiogroup" }, this._options.map((item) => {
            item.value = item.value.trim();
            return (h("label", null, h("input", { checked: this.value.indexOf(item.value) >= 0 || null, class: getErrorClass(this.isValid()), name: getId(this.name), disabled: this.disabled || null, onInput: (event) => this.setValue(event), type: "radio", value: item.value }), item.label));
        }))), getTagHint(this.hint), getTagError(this.showErrorMessage(), this.getErrorMessage())));
    }
    validateProps() {
        this.optionsWatch();
    }
    isValid() {
        return this.required ? !!this.value : true;
    }
    showErrorMessage() {
        return this.getErrorMessage() && !this.status.dotPristine;
    }
    getErrorMessage() {
        return this.isValid() ? '' : this.requiredMessage;
    }
    setValue(event) {
        this.value = event.target.value.trim();
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
        "options": ["optionsWatch"],
        "value": ["valueWatch"]
    }; }
    static get style() { return ".dot-radio__items {\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-direction: column;\n  flex-direction: column;\n}\n.dot-radio__items label {\n  -ms-flex-align: center;\n  align-items: center;\n  display: -ms-flexbox;\n  display: flex;\n}\n.dot-radio__items input {\n  margin: 0 0.25rem 0 0;\n}"; }
};

export { DotRadioComponent as dot_radio };
