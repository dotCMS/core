// tslint:disable:typedef

/**
 * Lazy Verifiers DO NOT CHECK ASSUMPTIONS before executing the validation logic.
 *
 * It is important to realize that executing LazyVerify methods in the wrong order can result in false positives,
 * not just errors. For example 'LazyVerify.isInteger("")' will return true.
 *
 * For example, a non-lazy
 * minLength(value) function would normally ensure that 'value' exists and is in fact a string. The lazy version
 * simply assumes those tests have already been done.
 *
 * Lazy verifiers are useful for Validation chains, where it is necessary to run each specific check to obtain
 * a accurate error messages, while not forcing users to add each validation into the chain themselves.
 *
 */
export class LazyVerify {
    static exists(value: unknown): value is NonNullable<unknown> {
        return !(value === null || value === undefined);
    }

    static empty(value: unknown): boolean {
        let empty = !LazyVerify.exists(value);
        if (!empty) {
            if (LazyVerify.isString(value)) {
                empty = LazyVerify.maxLength(value, 0);
            } else if (LazyVerify.isObject(value)) {
                empty = LazyVerify.emptyObject(value);
            }
        }

        return empty;
    }

    static isObject(value: unknown): value is Record<string, unknown> {
        return typeof value === 'object' && value !== null && value.constructor === Object;
    }

    static isString(value: unknown): value is string {
        return typeof value === 'string' || value instanceof String;
    }

    static isFunction(value: unknown): value is (...args: unknown[]) => unknown {
        return typeof value === 'function' || value instanceof Function;
    }

    static isArray(value: unknown): value is unknown[] {
        return Array.isArray(value) || value instanceof Array;
    }

    static emptyObject(value: Record<string, unknown>): boolean {
        return Object.getOwnPropertyNames(value).length === 0;
    }

    /**
     * The object has only the specified property keys, and by default must have all of the specified keys.
     * Setting allowMissing to true
     * @param object anything that works with Object.keys
     * @param properties Array of strings that represent keys to check for
     * @param allowMissing If true this test will fail if the object has a key that is not specified in properties,
     * but will not fail if a key is not present on the object.
     *
     */
    static hasOnly(
        object: Record<string, unknown>,
        properties: string[] = [],
        allowMissing = false
    ): boolean {
        const keys = Object.keys(object);
        const has: Record<string, boolean> = {};
        let count = 0;
        keys.forEach((key) => {
            has[key] = true;
            count++;
        });
        const hasAllOfDems = properties.every((propKey) => {
            count--;
            const x = has[propKey];

            return x === undefined || x === true;
        });

        return hasAllOfDems && (allowMissing ? count <= 0 : count === 0);
    }

    /**
     * The object has all the specified keys, and perhaps others.
     * @param object anything that works with Object.keys
     * @param properties Array of strings that represent keys to check for
     *
     */
    static hasAll(object: Record<string, unknown>, properties: string[] = []): boolean {
        const keys = Object.keys(object);
        const has: Record<string, boolean> = {};
        keys.forEach((key) => {
            has[key] = true;
        });
        const hasAllOfDems = properties.every((propKey) => {
            return has[propKey];
        });

        return hasAllOfDems;
    }

    static maxLength(value: string, max: number): boolean {
        return value.length <= max;
    }

    static minLength(value: string, min: number): boolean {
        return value.length >= min;
    }

    static isNumber(value: unknown): value is number {
        return typeof value === 'number' || value instanceof Number;
    }

    static isInteger(value: number): boolean {
        return value % 1 === 0;
    }

    static min(value: number, min: number): boolean {
        return value >= min;
    }

    static max(value: number, max: number): boolean {
        return value <= max;
    }

    static isBoolean(value: unknown): value is boolean {
        return value === true || value === false;
    }
}

export class Verify extends LazyVerify {
    static isString(value: unknown): value is string {
        return LazyVerify.isString(value);
    }

    static isStringWithEmpty(value: unknown, allowEmpty = false): boolean {
        return !LazyVerify.exists(value) ? allowEmpty === true : Verify.isString(value);
    }

    static maxLength(value: unknown, max: number): boolean {
        return Verify.isString(value) && LazyVerify.maxLength(value, max);
    }

    static minLength(value: unknown, min: number): boolean {
        return Verify.isString(value) && LazyVerify.minLength(value, min);
    }

    static isNumber(value: unknown): value is number {
        return LazyVerify.exists(value) && LazyVerify.isNumber(value);
    }

    static isInteger(value: unknown): boolean {
        return Verify.isNumber(value) && LazyVerify.isInteger(value);
    }

    static min(value: unknown, min: number): boolean {
        return Verify.isNumber(value) && LazyVerify.min(value, min);
    }

    static max(value: unknown, max: number): boolean {
        return Verify.isNumber(value) && LazyVerify.max(value, max);
    }

    static isFunction(value: unknown): value is (...args: unknown[]) => unknown {
        return LazyVerify.exists(value) && LazyVerify.isFunction(value);
    }

    static isArray(value: unknown): value is unknown[] {
        return LazyVerify.exists(value) && LazyVerify.isArray(value);
    }
}
