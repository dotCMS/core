import { renderHook } from '@testing-library/react-hooks';

import { useMemoizedObject } from './memoize';

describe('useMemoizedObject', () => {
    it('should return the same object if it has not changed', () => {
        const initialObject = { a: 1, b: 2 };

        const { result, rerender } = renderHook(({ obj }) => useMemoizedObject(obj), {
            initialProps: { obj: initialObject }
        });

        const firstRenderResult = result.current;

        rerender({ obj: initialObject });
        expect(result.current).toBe(firstRenderResult);
    });

    it('should return a new object if it has changed', () => {
        const initialObject = { a: 1, b: 2 };

        const newObject = { a: 1, b: 3 };

        const { result, rerender } = renderHook(({ obj }) => useMemoizedObject(obj), {
            initialProps: { obj: initialObject }
        });

        const firstRenderResult = result.current;

        rerender({ obj: newObject });
        expect(result.current).not.toBe(firstRenderResult);
        expect(result.current).toBe(newObject);
    });

    it('should return the same object if a deeply equal but different reference is passed', () => {
        const initialObject = { a: 1, b: 2 };

        const newObject = { a: 1, b: 2 };

        const { result, rerender } = renderHook(({ obj }) => useMemoizedObject(obj), {
            initialProps: { obj: initialObject }
        });

        const firstRenderResult = result.current;

        rerender({ obj: newObject });
        expect(result.current).toBe(firstRenderResult);
    });
});
