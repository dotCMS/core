import Fragment from 'stencil-fragment';
import { getClassNames, getDotOptionsFromFieldValue, getErrorClass, getOriginalStatus, getTagError, getTagHint, updateStatus, checkProp, getId, getHintId } from '../../utils';
import { getDotAttributesFromElement, setDotAttributesToElement } from '../dot-form/utils';
export class DotRadioComponent {
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
    static get style() { return "/**style-placeholder:dot-radio:**/"; }
}
