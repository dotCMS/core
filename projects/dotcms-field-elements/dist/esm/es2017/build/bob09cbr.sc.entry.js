import { h } from '../dotcmsfields.core.js';

import { a as Fragment } from './chunk-1d89c98b.js';
import { a as getOriginalStatus, b as checkProp, n as getDotOptionsFromFieldValue, c as getClassNames, h as getHintId, i as getErrorClass, k as getId, d as getTagHint, e as getTagError, f as updateStatus } from './chunk-62cd3eff.js';
import { a as getDotAttributesFromElement, b as setDotAttributesToElement } from './chunk-4205a04e.js';

class DotRadioComponent {
    constructor() {
        this.value = '';
        this.name = '';
        this.label = '';
        this.hint = '';
        this.required = false;
        this.disabled = false;
        this.requiredMessage = '';
        this.options = '';
    }
    reset() {
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
    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { label: this.label, required: this.required, name: this.name },
                h("div", { class: "dot-radio__items", "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, role: "radiogroup" }, this._options.map((item) => {
                    item.value = item.value.trim();
                    return (h("label", null,
                        h("input", { checked: this.value.indexOf(item.value) >= 0 || null, class: getErrorClass(this.isValid()), name: getId(this.name), disabled: this.disabled || null, onInput: (event) => this.setValue(event), type: "radio", value: item.value }),
                        item.label));
                }))),
            getTagHint(this.hint),
            getTagError(this.showErrorMessage(), this.getErrorMessage())));
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
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    emitValueChange() {
        this.valueChange.emit({
            name: this.name,
            value: this.value
        });
    }
    static get is() { return "dot-radio"; }
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
    static get style() { return ".dot-radio__items{display:-ms-flexbox;display:flex;-ms-flex-direction:column;flex-direction:column}.dot-radio__items label{-ms-flex-align:center;align-items:center;display:-ms-flexbox;display:flex}.dot-radio__items input{margin:0 .25rem 0 0}"; }
}

export { DotRadioComponent as DotRadio };
