import { useState } from 'react';

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
 * Maintaining the same reference if the object is shallowly equal, independently of
 * whether it is called inside any component.
 *
 * Uses React's "derived state" pattern (calling setState during rendering) to avoid
 * reading/writing `ref.current` during render, which violates the `react-hooks/refs` rule.
 *
 * React explicitly supports calling `setState` during rendering to update derived state.
 * @see https://react.dev/reference/react/useState#storing-information-from-previous-renders
 *
 * @param object
 * @returns
 */
export function useMemoizedObject<T extends object>(object: T): T {
    const [memoized, setMemoized] = useState<T>(object);

    // React-approved pattern: calling setState synchronously during render triggers an
    // immediate re-render (React batches it) and avoids reading/writing ref.current during
    // render (which violates the react-hooks/refs rule).
    if (!shallowEqual(memoized, object)) {
        setMemoized(object);

        return object;
    }

    return memoized;
}
