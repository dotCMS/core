import { useRef } from 'react';

/**
 * Compares two objects and returns true if they are equal, false otherwise.
 * @param objA The first object to compare.
 * @param objB The second object to compare.
 * @returns
 */
function shallowEqual<T>(objA: T, objB: T): boolean {
    if (Object.is(objA, objB)) {
        return true;
    }

    if (typeof objA !== 'object' || objA === null || typeof objB !== 'object' || objB === null) {
        return false;
    }

    const keysA = Object.keys(objA) as Array<keyof T>;

    const keysB = Object.keys(objB) as Array<keyof T>;

    if (keysA.length !== keysB.length) {
        return false;
    }

    for (const key of keysA) {
        if (!Object.prototype.hasOwnProperty.call(objB, key) || !Object.is(objA[key], objB[key])) {
            return false;
        }
    }

    return true;
}

/**
 * Memoizes an object and returns the memoized object.
 * Mantaing the same reference if the object is the same independently if is called inside any component.
 * @param object
 * @returns
 */
export function useMemoizedObject<T extends object>(object: T): T {
    const ref = useRef<T>(object);

    if (!shallowEqual(ref.current, object)) {
        ref.current = object;
    }

    return ref.current;
}
