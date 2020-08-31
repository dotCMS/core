import Fragment from 'stencil-fragment';
import { getClassNames, getDotOptionsFromFieldValue, getErrorClass, getId, getOriginalStatus, getTagError, getTagHint, updateStatus, checkProp, getHintId } from '../../utils';
import { getDotAttributesFromElement, setDotAttributesToElement } from '../dot-form/utils';
export class DotMultiSelectComponent {
    constructor() {
        this.disabled = false;
        this.name = '';
        this.label = '';
        this.hint = '';
        this.options = '';
        this.required = false;
        this.requiredMessage = `This field is required`;
        this.size = '3';
        this.value = '';
        this._dotTouched = false;
        this._dotPristine = true;
    }
    componentWillLoad() {
        this.validateProps();
        this.emitInitialValue();
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }
    componentDidLoad() {
        const htmlElement = this.el.querySelector('select');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(Array.from(this.el.attributes), []);
            setDotAttributesToElement(htmlElement, attrs);
        }, 0);
    }
    optionsWatch() {
        const validOptions = checkProp(this, 'options');
        this._options = getDotOptionsFromFieldValue(validOptions);
    }
    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }
    reset() {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitInitialValue();
        this.emitStatusChange();
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { label: this.label, required: this.required, name: this.name },
                h("select", { multiple: true, "aria-describedby": getHintId(this.hint), size: +this.size, class: getErrorClass(this.status.dotValid), id: getId(this.name), disabled: this.shouldBeDisabled(), onChange: () => this.setValue() }, this._options.map((item) => {
                    return (h("option", { selected: this.value === item.value ? true : null, value: item.value }, item.label));
                }))),
            getTagHint(this.hint),
            getTagError(!this.isValid(), this.requiredMessage)));
    }
    validateProps() {
        this.optionsWatch();
    }
    shouldBeDisabled() {
        return this.disabled ? true : null;
    }
    setValue() {
        this.value = this.getValueFromMultiSelect();
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
        this.emitValueChange();
        this.emitStatusChange();
    }
    getValueFromMultiSelect() {
        const selected = this.el.querySelectorAll('option:checked');
        const values = Array.from(selected).map((el) => el.value);
        return Array.from(values).join(',');
    }
    emitInitialValue() {
        if (!this.value) {
            this.value = this._options.length ? this._options[0].value : '';
            this.emitValueChange();
        }
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
    static get is() { return "dot-multi-select"; }
    static get properties() { return {
        "_options": {
            "state": true
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
        "size": {
            "type": String,
            "attr": "size",
            "reflectToAttr": true
        },
        "status": {
            "state": true
        },
        "value": {
            "type": String,
            "attr": "value",
            "reflectToAttr": true,
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
    static get style() { return "/**style-placeholder:dot-multi-select:**/"; }
}
