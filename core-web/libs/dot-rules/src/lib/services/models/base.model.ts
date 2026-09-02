/**
 * Base model class for all rule engine entities
 */
export class BaseModel {
    key: string;
    priority: number;

    constructor(key: string = null) {
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
