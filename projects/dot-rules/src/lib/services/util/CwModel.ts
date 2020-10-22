export class CwModel {
    key: string;
    priority: number;

    constructor(key: string = null) {
        this.key = key;
    }

    isPersisted(): boolean {
        return !!this.key;
    }

    /**
     * Override me.
     */
    isValid(): boolean {
        return true;
    }
}
