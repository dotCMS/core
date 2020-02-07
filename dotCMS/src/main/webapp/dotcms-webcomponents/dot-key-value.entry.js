import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-bb6a6489.js';
import { c as checkProp, j as getDotOptionsFromFieldValue, h as getOriginalStatus, a as getClassNames, b as getHintId, e as getTagHint, f as getTagError, u as updateStatus, g as getStringFromDotKeyArray } from './index-fca8faa0.js';

const mapToKeyValue = ({ label, value }) => {
    return {
        key: label,
        value
    };
};
const DotKeyValueComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Value of the field */
        this.value = '';
        /** Name that will be used as ID */
        this.name = '';
        /** (optional) Text to be rendered next to input field */
        this.label = '';
        /** (optional) Hint text that suggest a clue of the field */
        this.hint = '';
        /** (optional) Determine if it is mandatory */
        this.required = false;
        /** (optional) Text that will be shown when required is set and condition is not met */
        this.requiredMessage = 'This field is required';
        /** (optional) Disables field's interaction */
        this.disabled = false;
        this.items = [];
        this.dotValueChange = createEvent(this, "dotValueChange", 7);
        this.dotStatusChange = createEvent(this, "dotStatusChange", 7);
    }
    valueWatch() {
        this.value = checkProp(this, 'value', 'string');
        this.items = getDotOptionsFromFieldValue(this.value).map(mapToKeyValue);
    }
    /**
     * Reset properties of the field, clear value and emit events.
     */
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
    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);
        return (h(Host, { class: Object.assign({}, classes) }, h("dot-label", { "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, label: this.label, required: this.required, name: this.name }, h("key-value-form", { onLostFocus: this.blurHandler.bind(this), "add-button-label": this.formAddButtonLabel, disabled: this.isDisabled(), "key-label": this.formKeyLabel, "key-placeholder": this.formKeyPlaceholder, "value-label": this.formValueLabel, "value-placeholder": this.formValuePlaceholder }), h("key-value-table", { onClick: (e) => {
                e.preventDefault();
            }, "button-label": this.listDeleteLabel, disabled: this.isDisabled(), items: this.items })), getTagHint(this.hint), getTagError(this.showErrorMessage(), this.getErrorMessage())));
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
        this.dotStatusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    emitValueChange() {
        const returnedValue = getStringFromDotKeyArray(this.items);
        this.dotValueChange.emit({
            name: this.name,
            value: returnedValue
        });
    }
    emitChanges() {
        this.emitStatusChange();
        this.emitValueChange();
    }
    get el() { return getElement(this); }
    static get watchers() { return {
        "value": ["valueWatch"]
    }; }
    static get style() { return ""; }
};

export { DotKeyValueComponent as dot_key_value };
