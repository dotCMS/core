import Fragment from 'stencil-fragment';
import { checkProp, getClassNames, getErrorClass, getOriginalStatus, getTagError, getTagHint, updateStatus, getHintId, isStringType } from '../../utils';
export class DotTagsComponent {
    constructor() {
        this.value = '';
        this.data = null;
        this.name = '';
        this.label = '';
        this.hint = '';
        this.placeholder = '';
        this.required = false;
        this.requiredMessage = 'This field is required';
        this.disabled = false;
        this.threshold = 0;
        this.debounce = 300;
    }
    reset() {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitChanges();
    }
    valueWatch() {
        this.value = checkProp(this, 'value', 'string');
    }
    componentWillLoad() {
        this.status = getOriginalStatus(this.isValid());
        this.validateProps();
        this.emitStatusChange();
    }
    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { label: this.label, required: this.required, name: this.name },
                h("div", { "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, class: "dot-tags__container" },
                    h("dot-autocomplete", { class: getErrorClass(this.status.dotValid), data: this.data, debounce: this.debounce, disabled: this.isDisabled(), onEnter: this.onEnterHandler.bind(this), onLostFocus: this.blurHandler.bind(this), onSelection: this.onSelectHandler.bind(this), placeholder: this.placeholder || null, threshold: this.threshold }),
                    h("div", { class: "dot-tags__chips" }, this.getValues().map((tagLab) => (h("dot-chip", { disabled: this.isDisabled(), label: tagLab, onRemove: this.removeTag.bind(this) })))))),
            getTagHint(this.hint),
            getTagError(this.showErrorMessage(), this.getErrorMessage())));
    }
    addTag(label) {
        const values = this.getValues();
        if (!values.includes(label)) {
            values.push(label);
            this.value = values.join(',');
            this.updateStatus();
            this.emitChanges();
        }
    }
    blurHandler() {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
    }
    emitChanges() {
        this.emitStatusChange();
        this.emitValueChange();
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
    getErrorMessage() {
        return this.isValid() ? '' : this.requiredMessage;
    }
    getValues() {
        return isStringType(this.value) ? this.value.split(',') : [];
    }
    isDisabled() {
        return this.disabled || null;
    }
    isValid() {
        return !this.required || (this.required && !!this.value);
    }
    onEnterHandler({ detail = '' }) {
        detail.split(',').forEach((label) => {
            this.addTag(label.trim());
        });
    }
    onSelectHandler({ detail = '' }) {
        const value = detail.replace(',', ' ').replace(/\s+/g, ' ');
        this.addTag(value);
    }
    removeTag(event) {
        const values = this.getValues().filter((item) => item !== event.detail);
        this.value = values.join(',');
        this.updateStatus();
        this.emitChanges();
    }
    showErrorMessage() {
        return this.getErrorMessage() && !this.status.dotPristine;
    }
    updateStatus() {
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
    }
    validateProps() {
        this.valueWatch();
    }
    static get is() { return "dot-tags"; }
    static get properties() { return {
        "data": {
            "type": "Any",
            "attr": "data"
        },
        "debounce": {
            "type": Number,
            "attr": "debounce",
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
        "placeholder": {
            "type": String,
            "attr": "placeholder",
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
        "threshold": {
            "type": Number,
            "attr": "threshold",
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
    static get style() { return "/**style-placeholder:dot-tags:**/"; }
}
