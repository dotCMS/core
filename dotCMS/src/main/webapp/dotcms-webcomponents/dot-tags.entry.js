import { r as registerInstance, c as createEvent, h, H as Host, d as getElement } from './core-5e49af37.js';
import { g as getOriginalStatus, c as checkProp, b as getClassNames, d as getHintId, e as getErrorClass, f as getTagHint, h as getTagError, u as updateStatus, i as isStringType } from './index-d52678cd.js';

const DotTagsComponent = class {
    constructor(hostRef) {
        registerInstance(this, hostRef);
        /** Value formatted splitted with a comma, for example: tag-1,tag-2 */
        this.value = '';
        /** Name that will be used as ID */
        this.name = '';
        /** (optional) Text to be rendered next to input field */
        this.label = '';
        /** (optional) Hint text that suggest a clue of the field */
        this.hint = '';
        /** (optional) text to show when no value is set */
        this.placeholder = '';
        /** (optional) Determine if it is mandatory */
        this.required = false;
        /** (optional) Text that be shown when required is set and value is not set */
        this.requiredMessage = 'This field is required';
        /** (optional) Disables field's interaction */
        this.disabled = false;
        /** Min characters to start search in the autocomplete input */
        this.threshold = 0;
        /** Duraction in ms to start search into the autocomplete */
        this.debounce = 300;
        /** Function or array of string to get the data to use for the autocomplete search */
        this.data = null;
        this.dotValueChange = createEvent(this, "dotValueChange", 7);
        this.dotStatusChange = createEvent(this, "dotStatusChange", 7);
    }
    /**
     * Reset properties of the filed, clear value and emit events.
     */
    async reset() {
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
    render() {
        const classes = getClassNames(this.status, this.isValid(), this.required);
        return (h(Host, { class: Object.assign({}, classes) }, h("dot-label", { label: this.label, required: this.required, name: this.name }, h("div", { "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, class: "dot-tags__container" }, h("dot-autocomplete", { class: getErrorClass(this.status.dotValid), data: this.data, debounce: this.debounce, disabled: this.isDisabled(), onEnter: this.onEnterHandler.bind(this), onLostFocus: this.blurHandler.bind(this), onSelection: this.onSelectHandler.bind(this), placeholder: this.placeholder || null, threshold: this.threshold }), h("div", { class: "dot-tags__chips" }, this.getValues().map((tagLab) => (h("dot-chip", { disabled: this.isDisabled(), label: tagLab, onRemove: this.removeTag.bind(this) })))))), getTagHint(this.hint), getTagError(this.showErrorMessage(), this.getErrorMessage())));
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
        this.dotStatusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    emitValueChange() {
        this.dotValueChange.emit({
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
    get el() { return getElement(this); }
    static get watchers() { return {
        "value": ["valueWatch"]
    }; }
    static get style() { return "dot-tags .dot-tags__container {\n  display: -ms-flexbox;\n  display: flex;\n  -ms-flex-align: start;\n  align-items: flex-start;\n  border: solid 1px lightgray;\n}\ndot-tags .dot-tags__container dot-autocomplete {\n  margin: 0.5rem 1rem 0.5rem 0.5rem;\n}\ndot-tags .dot-tags__container .dot-tags__chips {\n  margin: 0.5rem 1rem 0 0;\n}\ndot-tags .dot-tags__container dot-chip {\n  border: solid 1px #ccc;\n  display: inline-block;\n  margin: 0 0.5rem 0.5rem 0;\n  padding: 0.2rem;\n}\ndot-tags button {\n  border: 0;\n}"; }
};

export { DotTagsComponent as dot_tags };
