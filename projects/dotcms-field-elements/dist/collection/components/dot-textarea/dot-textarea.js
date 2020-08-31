import Fragment from 'stencil-fragment';
import { getClassNames, getOriginalStatus, getTagHint, getTagError, getErrorClass, updateStatus, getId, checkProp, getHintId } from '../../utils';
import { setDotAttributesToElement, getDotAttributesFromElement } from '../dot-form/utils';
export class DotTextareaComponent {
    constructor() {
        this.value = '';
        this.name = '';
        this.label = '';
        this.hint = '';
        this.required = false;
        this.requiredMessage = 'This field is required';
        this.validationMessage = "The field doesn't comply with the specified format";
        this.disabled = false;
        this.regexCheck = '';
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
    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { label: this.label, required: this.required, name: this.name },
                h("textarea", { "aria-describedby": getHintId(this.hint), class: getErrorClass(this.status.dotValid), id: getId(this.name), name: this.name, value: this.value, required: this.getRequiredAttr(), onInput: (event) => this.setValue(event), onBlur: () => this.blurHandler(), disabled: this.getDisabledAtt() })),
            getTagHint(this.hint),
            getTagError(this.shouldShowErrorMessage(), this.getErrorMessage())));
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
    static get is() { return "dot-textarea"; }
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
            "attr": "name",
            "reflectToAttr": true
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
            "attr": "required-message",
            "reflectToAttr": true
        },
        "reset": {
            "method": true
        },
        "status": {
            "state": true
        },
        "validationMessage": {
            "type": String,
            "attr": "validation-message",
            "reflectToAttr": true
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
    static get style() { return "/**style-placeholder:dot-textarea:**/"; }
}
