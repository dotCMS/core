/**
 * Base model class for all rule engine entities
 */
export class BaseModel {
    /**
     * `null` until the entity is persisted — `isPersisted()` below is the whole reason this
     * class exists, and `ActionModel`/`ConditionModel` are routinely constructed without a key.
     */
    key: string | null;

    /** Every subclass constructor already collapses a missing priority to 1. */
    priority = 1;

    constructor(key: string | null = null) {
        this.key = key;
    }

    isPersisted(): boolean {
        return !!this.key;
    }

    /**
     * Override in subclasses to provide custom validation
     */
    isValid(): boolean {
        return true;
    }
}

/** @deprecated Use BaseModel instead */
export const CwModel = BaseModel;
