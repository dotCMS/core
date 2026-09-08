import { Node, Schema } from 'prosemirror-model';
import { Step, StepResult } from 'prosemirror-transform';

/**
 * ProseMirror declares `Node.attrs` readonly, but changing document attributes requires
 * mutating them in place — that is the whole point of these steps. This view is the single
 * place where that is spelled out, instead of casting at each assignment.
 */
type MutableDoc = { attrs: Record<string, unknown>; type: { defaultAttrs: unknown } };

const asMutable = (doc: Node) => doc as unknown as MutableDoc;

const isDuplicateStepId = (err: unknown, stepType: string) =>
    err instanceof Error && err.message === `Duplicate use of step JSON ID ${stepType}`;

// Adapted from https://discuss.prosemirror.net/t/changing-doc-attrs/784
export class SetDocAttrStep extends Step {
    private key: string;
    private value: unknown;
    /** Captured by {@link apply} so {@link invert} can put the old value back. */
    private prevValue: unknown;
    private STEP_TYPE = 'setDocAttr';

    constructor(key: string, value: unknown) {
        super();
        this.key = key;
        this.value = value;
    }

    get stepType() {
        return this.STEP_TYPE;
    }

    static override fromJSON(_schema: Schema, json: { key: string; value: unknown }) {
        return new SetDocAttrStep(json.key, json.value);
    }

    static register() {
        try {
            Step.jsonID(this.prototype.STEP_TYPE, SetDocAttrStep);
        } catch (err) {
            if (!isDuplicateStepId(err, this.prototype.STEP_TYPE)) {
                throw err;
            }
        }

        return true;
    }

    apply(doc: Node) {
        const mutableDoc = asMutable(doc);
        this.prevValue = mutableDoc.attrs[this.key];

        // avoid clobbering doc.type.defaultAttrs
        if (mutableDoc.attrs === mutableDoc.type.defaultAttrs) {
            mutableDoc.attrs = { ...mutableDoc.attrs };
        }

        mutableDoc.attrs[this.key] = this.value;

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
            if (!isDuplicateStepId(err, this.prototype.STEP_TYPE)) {
                throw err;
            }
        }

        return true;
    }

    apply(doc: Node) {
        const mutableDoc = asMutable(doc);
        mutableDoc.attrs = { ...(mutableDoc.type.defaultAttrs as Record<string, unknown>) };

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
