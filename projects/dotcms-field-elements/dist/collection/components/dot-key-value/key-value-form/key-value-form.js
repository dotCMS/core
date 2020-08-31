const DEFAULT_VALUE = { key: '', value: '' };
export class DotKeyValueComponent {
    constructor() {
        this.disabled = false;
        this.addButtonLabel = 'Add';
        this.keyPlaceholder = '';
        this.valuePlaceholder = '';
        this.keyLabel = 'Key';
        this.valueLabel = 'Value';
        this.inputs = Object.assign({}, DEFAULT_VALUE);
    }
    render() {
        const buttonDisabled = this.isButtonDisabled();
        return (h("form", { onSubmit: this.addKey.bind(this) },
            h("label", null,
                this.keyLabel,
                h("input", { disabled: this.disabled, name: "key", onBlur: (e) => this.lostFocus.emit(e), onInput: (event) => this.setValue(event), placeholder: this.keyPlaceholder, type: "text", value: this.inputs.key })),
            h("label", null,
                this.valueLabel,
                h("input", { disabled: this.disabled, name: "value", onBlur: (e) => this.lostFocus.emit(e), onInput: (event) => this.setValue(event), placeholder: this.valuePlaceholder, type: "text", value: this.inputs.value })),
            h("button", { class: "key-value-form__save__button", type: "submit", disabled: buttonDisabled }, this.addButtonLabel)));
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
        this.inputs = Object.assign({}, this.inputs, { [target.name]: target.value.toString() });
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
    static get is() { return "key-value-form"; }
    static get properties() { return {
        "addButtonLabel": {
            "type": String,
            "attr": "add-button-label",
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
        "inputs": {
            "state": true
        },
        "keyLabel": {
            "type": String,
            "attr": "key-label",
            "reflectToAttr": true
        },
        "keyPlaceholder": {
            "type": String,
            "attr": "key-placeholder",
            "reflectToAttr": true
        },
        "valueLabel": {
            "type": String,
            "attr": "value-label",
            "reflectToAttr": true
        },
        "valuePlaceholder": {
            "type": String,
            "attr": "value-placeholder",
            "reflectToAttr": true
        }
    }; }
    static get events() { return [{
            "name": "add",
            "method": "add",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "lostFocus",
            "method": "lostFocus",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return "/**style-placeholder:key-value-form:**/"; }
}
