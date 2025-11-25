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
    static exists(value) {
        return !(value === null || value === undefined);
    }

    static empty(value) {
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

    static isObject(value) {
        return typeof value === 'object' || value.constructor === Object;
    }

    static isString(value) {
        return typeof value === 'string' || value instanceof String;
    }

    static isFunction(value) {
        return typeof value === 'function' || value instanceof Function;
    }

    static isArray(value) {
        return Array.isArray(value) || value instanceof Array;
    }

    static emptyObject(value) {
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
    static hasOnly(object, properties = [], allowMissing = false) {
        const keys = Object.keys(object);
        const has = {};
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
    static hasAll(object, properties = []) {
        const keys = Object.keys(object);
        const has = {};
        keys.forEach((key) => {
            has[key] = true;
        });
        const hasAllOfDems = properties.every((propKey) => {
            return has[propKey];
        });

        return hasAllOfDems;
    }

    static maxLength(value, max) {
        return value.length <= max;
    }

    static minLength(value, min) {
        return value.length >= min;
    }

    static isNumber(value) {
        return typeof value === 'number' || value instanceof Number;
    }

    static isInteger(value) {
        return value % 1 === 0;
    }

    static min(value, min) {
        return value >= min;
    }

    static max(value, max) {
        return value <= max;
    }

    static isBoolean(value) {
        return value === true || value === false;
    }
}

export class Verify extends LazyVerify {
    static isString(value, allowEmpty = false) {
        return !LazyVerify.exists(value) ? allowEmpty === true : LazyVerify.isString(value);
    }

    static maxLength(value, max) {
        return Verify.isString(value) && LazyVerify.maxLength(value, max);
    }

    static minLength(value, min) {
        return Verify.isString(value) && LazyVerify.minLength(value, min);
    }

    static isNumber(value) {
        return LazyVerify.exists(value) && LazyVerify.isNumber(value);
    }

    static isInteger(value) {
        return Verify.isNumber(value) && LazyVerify.isInteger(value);
    }

    static min(value, min) {
        return Verify.isNumber(value) && LazyVerify.min(value, min);
    }

    static max(value, max) {
        return Verify.isNumber(value) && LazyVerify.max(value, max);
    }

    static isFunction(value) {
        return LazyVerify.exists(value) && LazyVerify.isFunction(value);
    }

    static isArray(value) {
        return LazyVerify.exists(value) && LazyVerify.isArray(value);
    }
}
