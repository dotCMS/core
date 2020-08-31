import { h } from '../dotcmsfields.core.js';

import { a as Fragment } from './chunk-1d89c98b.js';
import { a as getOriginalStatus, b as checkProp, n as getDotOptionsFromFieldValue, c as getClassNames, h as getHintId, i as getErrorClass, k as getId, d as getTagHint, e as getTagError, f as updateStatus } from './chunk-62cd3eff.js';
import { a as getDotAttributesFromElement, b as setDotAttributesToElement } from './chunk-4205a04e.js';

class DotCheckboxComponent {
    constructor() {
        this.disabled = false;
        this.name = '';
        this.label = '';
        this.hint = '';
        this.options = '';
        this.required = false;
        this.requiredMessage = `This field is required`;
        this.value = '';
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
    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }
    reset() {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitValueChange();
        this.emitStatusChange();
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { label: this.label, required: this.required, name: this.name },
                h("div", { "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, class: "dot-checkbox__items" }, this._options.map((item) => {
                    const trimmedValue = item.value.trim();
                    return (h("label", null,
                        h("input", { class: getErrorClass(this.isValid()), name: getId(this.name), type: "checkbox", disabled: this.disabled || null, checked: this.value.indexOf(trimmedValue) >= 0 || null, onInput: (event) => this.setValue(event), value: trimmedValue }),
                        item.label));
                }))),
            getTagHint(this.hint),
            getTagError(!this.isValid(), this.requiredMessage)));
    }
    validateProps() {
        this.optionsWatch();
    }
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
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    isValid() {
        return this.required ? !!this.value : true;
    }
    emitValueChange() {
        this.valueChange.emit({
            name: this.name,
            value: this.value
        });
    }
    static get is() { return "dot-checkbox"; }
    static get properties() { return {
        "_options": {
            "state": true
        },
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true,
            "mutable": true
        },
        "el": {
            "elementRef": true
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
        "name": {
            "type": String,
            "attr": "name",
            "reflectToAttr": true
        },
        "options": {
            "type": String,
            "attr": "options",
            "reflectToAttr": true,
            "watchCallbacks": ["optionsWatch"]
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
        "status": {
            "state": true
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
    static get style() { return ".dot-checkbox__items{display:-ms-flexbox;display:flex;-ms-flex-direction:column;flex-direction:column}.dot-checkbox__items label{display:-ms-flexbox;display:flex;-ms-flex-align:center;align-items:center}.dot-checkbox__items input{margin:0 .25rem 0 0}"; }
}

export { DotCheckboxComponent as DotCheckbox };
