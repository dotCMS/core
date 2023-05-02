import { Step, StepResult } from 'prosemirror-transform';

// Adapted from https://discuss.prosemirror.net/t/changing-doc-attrs/784
export class SetDocAttrStep extends Step {
    private key: string;
    private value: string;
    private prevValue: string;
    private STEP_TYPE = 'setDocAttr';

    constructor(key, value) {
        super();
        this.key = key;
        this.value = value;
    }

    get stepType() {
        return this.STEP_TYPE;
    }

    static fromJSON(_schema, json) {
        return new SetDocAttrStep(json.key, json.value);
    }

    static register() {
        try {
            Step.jsonID(this.prototype.STEP_TYPE, SetDocAttrStep);
        } catch (err) {
            if (err.message !== `Duplicate use of step JSON ID ${this.prototype.STEP_TYPE}`)
                throw err;
        }

        return true;
    }

    apply(doc) {
        this.prevValue = doc.attrs[this.key];
        // avoid clobbering doc.type.defaultAttrs
        if (doc.attrs === doc.type.defaultAttrs) {
            doc.attrs = Object.assign({}, doc.attrs);
        }

        doc.attrs[this.key] = this.value;

        return StepResult.ok(doc);
    }

    invert() {
        return new SetDocAttrStep(this.key, this.prevValue);
    }

    // position never changes so map should always return same step
    map() {
        return this;
    }

    toJSON() {
        return {
            stepType: this.stepType,
            key: this.key,
            value: this.value
        };
    }
}

/**
 * Restore Default DOM Attributes
 *
 * @export
 * @class RestoreDefaultDOMAttrs
 * @extends {Step}
 */
export class RestoreDefaultDOMAttrs extends Step {
    private STEP_TYPE = 'restoreDefaultDOMAttrs';

    constructor() {
        super();
    }

    get stepType() {
        return this.STEP_TYPE;
    }

    static register() {
        try {
            Step.jsonID(this.prototype.STEP_TYPE, RestoreDefaultDOMAttrs);
        } catch (err) {
            if (err.message !== `Duplicate use of step JSON ID ${this.prototype.STEP_TYPE}`)
                throw err;
        }

        return true;
    }

    apply(doc) {
        doc.attrs = Object.assign({}, doc.type.defaultAttrs);

        return StepResult.ok(doc);
    }

    invert() {
        return new RestoreDefaultDOMAttrs();
    }

    // position never changes so map should always return same step
    map() {
        return this;
    }

    toJSON() {
        return {
            stepType: this.stepType
        };
    }
}
