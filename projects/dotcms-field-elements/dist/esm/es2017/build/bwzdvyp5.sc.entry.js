import { h } from '../dotcmsfields.core.js';

import { a as Fragment } from './chunk-1d89c98b.js';
import { a as getOriginalStatus, b as checkProp, c as getClassNames, h as getHintId, i as getErrorClass, k as getId, d as getTagHint, e as getTagError, f as updateStatus } from './chunk-62cd3eff.js';
import { a as getDotAttributesFromElement, b as setDotAttributesToElement } from './chunk-4205a04e.js';

class DotTextfieldComponent {
    constructor() {
        this.value = '';
        this.name = '';
        this.label = '';
        this.placeholder = '';
        this.hint = '';
        this.required = false;
        this.requiredMessage = 'This field is required';
        this.validationMessage = "The field doesn't comply with the specified format";
        this.disabled = false;
        this.regexCheck = '';
        this.type = 'text';
    }
    reset() {
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
    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { label: this.label, required: this.required, name: this.name },
                h("input", { "aria-describedby": getHintId(this.hint), class: getErrorClass(this.status.dotValid), disabled: this.disabled || null, id: getId(this.name), onBlur: () => this.blurHandler(), onInput: (event) => this.setValue(event), placeholder: this.placeholder, required: this.required || null, type: this.type, value: this.value })),
            getTagHint(this.hint),
            getTagError(this.shouldShowErrorMessage(), this.getErrorMessage())));
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
    static get is() { return "dot-textfield"; }
    static get properties() { return {
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
            "attr": "name"
        },
        "placeholder": {
            "type": String,
            "attr": "placeholder",
            "reflectToAttr": true,
            "mutable": true
        },
        "regexCheck": {
            "type": String,
            "attr": "regex-check",
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["regexCheckWatch"]
        },
        "required": {
            "type": Boolean,
            "attr": "required",
            "reflectToAttr": true,
            "mutable": true
        },
        "requiredMessage": {
            "type": String,
            "attr": "required-message"
        },
        "reset": {
            "method": true
        },
        "status": {
            "state": true
        },
        "type": {
            "type": String,
            "attr": "type",
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["typeWatch"]
        },
        "validationMessage": {
            "type": String,
            "attr": "validation-message"
        },
        "value": {
            "type": String,
            "attr": "value",
            "mutable": true
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
    static get style() { return "input{outline:none}"; }
}

export { DotTextfieldComponent as DotTextfield };
