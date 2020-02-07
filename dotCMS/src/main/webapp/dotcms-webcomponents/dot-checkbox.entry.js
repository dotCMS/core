import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-bb6a6489.js';
import { h as getOriginalStatus, c as checkProp, j as getDotOptionsFromFieldValue, a as getClassNames, b as getHintId, k as getErrorClass, l as getId, e as getTagHint, f as getTagError, u as updateStatus } from './index-fca8faa0.js';
import { g as getDotAttributesFromElement, s as setDotAttributesToElement } from './index-b2e001f1.js';

const DotCheckboxComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Name that will be used as ID */
        this.name = '';
        /** (optional) Text to be rendered next to input field */
        this.label = '';
        /** (optional) Hint text that suggest a clue of the field */
        this.hint = '';
        /** Value/Label checkbox options separated by comma, to be formatted as: Value|Label */
        this.options = '';
        /** (optional) Determine if it is mandatory */
        this.required = false;
        /** (optional) Disables field's interaction */
        this.disabled = false;
        /** (optional) Text that will be shown when required is set and condition is not met */
        this.requiredMessage = `This field is required`;
        /** Value set from the checkbox option */
        this.value = '';
        this.dotValueChange = createEvent(this, "dotValueChange", 7);
        this.dotStatusChange = createEvent(this, "dotStatusChange", 7);
    }
    componentWillLoad() {
        this.value = this.value || '';
        this.validateProps();
        this.emitValueChange();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }
    componentDidLoad() {
        const attrException = ['dottype'];
        const htmlElements = this.el.querySelectorAll('input[type="checkbox"]');
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
    /**
     * Reset properties of the field, clear value and emit events.
     *
     * @memberof DotSelectComponent
     */
    async reset() {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitValueChange();
        this.emitStatusChange();
    }
    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);
        return (h(Host, { class: Object.assign({}, classes) }, h("dot-label", { label: this.label, required: this.required, name: this.name }, h("div", { "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, class: "dot-checkbox__items" }, this._options.map((item) => {
            const trimmedValue = item.value.trim();
            return (h("label", null, h("input", { class: getErrorClass(this.isValid()), name: getId(this.name), type: "checkbox", disabled: this.disabled || null, checked: this.value.indexOf(trimmedValue) >= 0 || null, onInput: (event) => this.setValue(event), value: trimmedValue }), item.label));
        }))), getTagHint(this.hint), getTagError(!this.isValid(), this.requiredMessage)));
    }
    validateProps() {
        this.optionsWatch();
    }
    // Todo: find how to set proper TYPE in TS
    setValue(event) {
        this.value = this.getValueFromCheckInputs(event.target.value.trim(), event.target.checked);
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.emitValueChange();
        this.emitStatusChange();
    }
    getValueFromCheckInputs(value, checked) {
        const valueArray = this.value.trim().length ? this.value.split(',') : [];
        const valuesSet = new Set(valueArray);
        if (checked) {
            valuesSet.add(value);
        }
        else {
            valuesSet.delete(value);
        }
        return Array.from(valuesSet).join(',');
    }
    emitStatusChange() {
        this.dotStatusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    isValid() {
        return this.required ? !!this.value : true;
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
    static get style() { return ".dot-checkbox__items {\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-direction: column;\n  flex-direction: column;\n}\n.dot-checkbox__items label {\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-align: center;\n  align-items: center;\n}\n.dot-checkbox__items input {\n  margin: 0 0.25rem 0 0;\n}"; }
};

export { DotCheckboxComponent as dot_checkbox };
