import { r as registerInstance, c as createEvent, h, d as getElement } from './core-bb6a6489.js';

const DEFAULT_VALUE = { key: '', value: '' };
const DotKeyValueComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** (optional) Disables all form interaction */
        this.disabled = false;
        /** (optional) Label for the add item button */
        this.addButtonLabel = 'Add';
        /** (optional) Placeholder for the key input text */
        this.keyPlaceholder = '';
        /** (optional) Placeholder for the value input text */
        this.valuePlaceholder = '';
        /** (optional) The string to use in the key input label */
        this.keyLabel = 'Key';
        /** (optional) The string to use in the value input label */
        this.valueLabel = 'Value';
        this.inputs = Object.assign({}, DEFAULT_VALUE);
        this.add = createEvent(this, "add", 7);
        this.lostFocus = createEvent(this, "lostFocus", 7);
    }
    render() {
        const buttonDisabled = this.isButtonDisabled();
        return (h("form", { onSubmit: this.addKey.bind(this) }, h("label", null, this.keyLabel, h("input", { disabled: this.disabled, name: "key", onBlur: (e) => this.lostFocus.emit(e), onInput: (event) => this.setValue(event), placeholder: this.keyPlaceholder, type: "text", value: this.inputs.key })), h("label", null, this.valueLabel, h("input", { disabled: this.disabled, name: "value", onBlur: (e) => this.lostFocus.emit(e), onInput: (event) => this.setValue(event), placeholder: this.valuePlaceholder, type: "text", value: this.inputs.value })), h("button", { class: "key-value-form__save__button", type: "submit", disabled: buttonDisabled }, this.addButtonLabel)));
    }
    isButtonDisabled() {
        return !this.isFormValid() || (this.disabled || null);
    }
    isFormValid() {
        return !!(this.inputs.key.length && this.inputs.value.length);
    }
    setValue(event) {
        event.stopImmediatePropagation();
        const target = event.target;
        this.inputs = Object.assign(Object.assign({}, this.inputs), { [target.name]: target.value.toString() });
    }
    addKey(event) {
        event.preventDefault();
        event.stopImmediatePropagation();
        if (this.inputs.key && this.inputs.value) {
            this.add.emit(this.inputs);
            this.clearForm();
            this.focusKeyInputField();
        }
    }
    clearForm() {
        this.inputs = Object.assign({}, DEFAULT_VALUE);
    }
    focusKeyInputField() {
        const input = this.el.querySelector('input[name="key"]');
        input.focus();
    }
    get el() { return getElement(this); }
    static get style() { return "key-value-form form {\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-align: center;\n  align-items: center;\n}\nkey-value-form form button {\n  margin: 0;\n}\nkey-value-form form input {\n  margin: 0 1rem 0 0.5rem;\n}\nkey-value-form form label {\n  -ms-flex-align: center;\n  align-items: center;\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-positive: 1;\n  flex-grow: 1;\n}\nkey-value-form form label input {\n  -ms-flex-positive: 1;\n  flex-grow: 1;\n}"; }
};

export { DotKeyValueComponent as key_value_form };
