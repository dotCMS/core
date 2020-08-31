import Fragment from 'stencil-fragment';
import { getErrorClass, getId, getOriginalStatus, updateStatus } from '../../utils';
export class DotInputCalendarComponent {
    constructor() {
        this.value = '';
        this.name = '';
        this.required = false;
        this.disabled = false;
        this.min = '';
        this.max = '';
        this.step = '1';
        this.type = '';
    }
    reset() {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitValueChange();
        this.emitStatusChange();
    }
    componentWillLoad() {
        this.status = getOriginalStatus(this.isValid());
        this.emitStatusChange();
    }
    render() {
        return (h(Fragment, null,
            h("input", { class: getErrorClass(this.status.dotValid), disabled: this.disabled || null, id: getId(this.name), onBlur: () => this.blurHandler(), onInput: (event) => this.setValue(event), required: this.required || null, type: this.type, value: this.value, min: this.min, max: this.max, step: this.step })));
    }
    isValid() {
        return this.isValueInRange() && this.isRequired();
    }
    isRequired() {
        return this.required ? !!this.value : true;
    }
    isValueInRange() {
        return this.isInMaxRange() && this.isInMinRange();
    }
    isInMinRange() {
        return !!this.min ? this.value >= this.min : true;
    }
    isInMaxRange() {
        return !!this.max ? this.value <= this.max : true;
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
        this._statusChange.emit({
            name: this.name,
            status: this.status,
            isValidRange: this.isValueInRange()
        });
    }
    emitValueChange() {
        this._valueChange.emit({
            name: this.name,
            value: this.formattedValue()
        });
    }
    formattedValue() {
        return this.value.length === 5 ? `${this.value}:00` : this.value;
    }
    static get is() { return "dot-input-calendar"; }
    static get properties() { return {
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "max": {
            "type": String,
            "attr": "max",
            "reflectToAttr": true
        },
        "min": {
            "type": String,
            "attr": "min",
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
        "reset": {
            "method": true
        },
        "status": {
            "state": true
        },
        "step": {
            "type": String,
            "attr": "step",
            "reflectToAttr": true
        },
        "type": {
            "type": String,
            "attr": "type",
            "reflectToAttr": true
        },
        "value": {
            "type": String,
            "attr": "value",
            "reflectToAttr": true,
            "mutable": true
        }
    }; }
    static get events() { return [{
            "name": "_valueChange",
            "method": "_valueChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "_statusChange",
            "method": "_statusChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return "/**style-placeholder:dot-input-calendar:**/"; }
}
