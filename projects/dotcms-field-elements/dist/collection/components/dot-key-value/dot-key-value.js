import Fragment from 'stencil-fragment';
import { checkProp, getClassNames, getDotOptionsFromFieldValue, getOriginalStatus, getStringFromDotKeyArray, getTagError, getTagHint, updateStatus, getHintId } from '../../utils';
const mapToKeyValue = ({ label, value }) => {
    return {
        key: label,
        value
    };
};
export class DotKeyValueComponent {
    constructor() {
        this.disabled = false;
        this.hint = '';
        this.label = '';
        this.name = '';
        this.required = false;
        this.requiredMessage = 'This field is required';
        this.value = '';
        this.items = [];
    }
    valueWatch() {
        this.value = checkProp(this, 'value', 'string');
        this.items = getDotOptionsFromFieldValue(this.value).map(mapToKeyValue);
    }
    reset() {
        this.items = [];
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitChanges();
    }
    deleteItemHandler(event) {
        event.stopImmediatePropagation();
        this.items = this.items.filter((_item, index) => index !== event.detail);
        this.refreshStatus();
        this.emitChanges();
    }
    addItemHandler({ detail }) {
        this.items = [...this.items, detail];
        this.refreshStatus();
        this.emitChanges();
    }
    componentWillLoad() {
        this.validateProps();
        this.setOriginalStatus();
        this.emitStatusChange();
    }
    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, label: this.label, required: this.required, name: this.name },
                h("key-value-form", { onLostFocus: this.blurHandler.bind(this), "add-button-label": this.formAddButtonLabel, disabled: this.isDisabled(), "key-label": this.formKeyLabel, "key-placeholder": this.formKeyPlaceholder, "value-label": this.formValueLabel, "value-placeholder": this.formValuePlaceholder }),
                h("key-value-table", { onClick: (e) => {
                        e.preventDefault();
                    }, "button-label": this.listDeleteLabel, disabled: this.isDisabled(), items: this.items })),
            getTagHint(this.hint),
            getTagError(this.showErrorMessage(), this.getErrorMessage())));
    }
    isDisabled() {
        return this.disabled || null;
    }
    blurHandler() {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
    }
    validateProps() {
        this.valueWatch();
    }
    setOriginalStatus() {
        this.status = getOriginalStatus(this.isValid());
    }
    isValid() {
        return !(this.required && !this.items.length);
    }
    showErrorMessage() {
        return this.getErrorMessage() && !this.status.dotPristine;
    }
    getErrorMessage() {
        return this.isValid() ? '' : this.requiredMessage;
    }
    refreshStatus() {
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
    }
    emitStatusChange() {
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    emitValueChange() {
        const returnedValue = getStringFromDotKeyArray(this.items);
        this.valueChange.emit({
            name: this.name,
            value: returnedValue
        });
    }
    emitChanges() {
        this.emitStatusChange();
        this.emitValueChange();
    }
    static get is() { return "dot-key-value"; }
    static get properties() { return {
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "formAddButtonLabel": {
            "type": String,
            "attr": "form-add-button-label",
            "reflectToAttr": true
        },
        "formKeyLabel": {
            "type": String,
            "attr": "form-key-label",
            "reflectToAttr": true
        },
        "formKeyPlaceholder": {
            "type": String,
            "attr": "form-key-placeholder",
            "reflectToAttr": true
        },
        "formValueLabel": {
            "type": String,
            "attr": "form-value-label",
            "reflectToAttr": true
        },
        "formValuePlaceholder": {
            "type": String,
            "attr": "form-value-placeholder",
            "reflectToAttr": true
        },
        "hint": {
            "type": String,
            "attr": "hint",
            "reflectToAttr": true
        },
        "items": {
            "state": true
        },
        "label": {
            "type": String,
            "attr": "label",
            "reflectToAttr": true
        },
        "listDeleteLabel": {
            "type": String,
            "attr": "list-delete-label",
            "reflectToAttr": true
        },
        "name": {
            "type": String,
            "attr": "name",
            "reflectToAttr": true
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
    static get listeners() { return [{
            "name": "delete",
            "method": "deleteItemHandler"
        }, {
            "name": "add",
            "method": "addItemHandler"
        }]; }
    static get style() { return "/**style-placeholder:dot-key-value:**/"; }
}
