import Fragment from 'stencil-fragment';
import { checkProp, getClassNames, getTagError, getTagHint, getHintId } from '../../utils';
import { setDotAttributesToElement, getDotAttributesFromElement } from '../dot-form/utils';
export class DotDateComponent {
    constructor() {
        this.value = '';
        this.name = '';
        this.label = '';
        this.hint = '';
        this.required = false;
        this.requiredMessage = 'This field is required';
        this.validationMessage = "The field doesn't comply with the specified format";
        this.disabled = false;
        this.min = '';
        this.max = '';
        this.step = '1';
    }
    reset() {
        const input = this.el.querySelector('dot-input-calendar');
        input.reset();
    }
    componentWillLoad() {
        this.validateProps();
    }
    componentDidLoad() {
        const attrException = ['dottype'];
        const htmlElement = this.el.querySelector('input[type="date"]');
        setTimeout(() => {
            const attrs = getDotAttributesFromElement(Array.from(this.el.attributes), attrException);
            setDotAttributesToElement(htmlElement, attrs);
        }, 0);
    }
    minWatch() {
        this.min = checkProp(this, 'min', 'date');
    }
    maxWatch() {
        this.max = checkProp(this, 'max', 'date');
    }
    emitValueChange(event) {
        event.stopImmediatePropagation();
        const valueEvent = event.detail;
        this.value = valueEvent.value;
        this.valueChange.emit(valueEvent);
    }
    emitStatusChange(event) {
        event.stopImmediatePropagation();
        const inputCalendarStatus = event.detail;
        this.classNames = getClassNames(inputCalendarStatus.status, inputCalendarStatus.status.dotValid, this.required);
        this.setErrorMessageElement(inputCalendarStatus);
        this.statusChange.emit({
            name: inputCalendarStatus.name,
            status: inputCalendarStatus.status
        });
    }
    hostData() {
        return {
            class: this.classNames
        };
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { label: this.label, required: this.required, name: this.name },
                h("dot-input-calendar", { "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, disabled: this.disabled, type: "date", name: this.name, value: this.value, required: this.required, min: this.min, max: this.max, step: this.step })),
            getTagHint(this.hint),
            this.errorMessageElement));
    }
    validateProps() {
        this.minWatch();
        this.maxWatch();
    }
    setErrorMessageElement(statusEvent) {
        this.errorMessageElement = getTagError(!statusEvent.status.dotValid && !statusEvent.status.dotPristine, this.getErrorMessage(statusEvent));
    }
    getErrorMessage(statusEvent) {
        return !!this.value
            ? statusEvent.isValidRange
                ? ''
                : this.validationMessage
            : this.requiredMessage;
    }
    static get is() { return "dot-date"; }
    static get properties() { return {
        "classNames": {
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
        "errorMessageElement": {
            "state": true
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
        "max": {
            "type": String,
            "attr": "max",
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["maxWatch"]
        },
        "min": {
            "type": String,
            "attr": "min",
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["minWatch"]
        },
        "name": {
            "type": String,
            "attr": "name",
            "reflectToAttr": true
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
        "step": {
            "type": String,
            "attr": "step",
            "reflectToAttr": true,
            "mutable": true
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
    static get listeners() { return [{
            "name": "_valueChange",
            "method": "emitValueChange"
        }, {
            "name": "_statusChange",
            "method": "emitStatusChange"
        }]; }
    static get style() { return "/**style-placeholder:dot-date:**/"; }
}
