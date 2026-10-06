import { describe, expect, it } from 'vitest';
import { reactive, ref, shallowRef } from 'vue';

import { toPlain } from './toPlain';

describe('toPlain', () => {
    it('returns primitives unchanged', () => {
        expect(toPlain(42)).toBe(42);
        expect(toPlain('hi')).toBe('hi');
        expect(toPlain(null)).toBe(null);
        expect(toPlain(undefined)).toBe(undefined);
    });

    it('deep-copies a plain object into a new, equal object', () => {
        const input = { a: 1, nested: { b: 2 } };
        const out = toPlain(input);
        expect(out).toEqual(input);
        expect(out).not.toBe(input);
        expect(out.nested).not.toBe(input.nested);
    });

    it('unwraps a reactive proxy into a plain object', () => {
        const input = reactive({ pageAsset: { title: 'Home' } });
        const out = toPlain(input);
        expect(out).toEqual({ pageAsset: { title: 'Home' } });
        // structuredClone throws on a Vue Proxy — it must succeed here.
        expect(() => structuredClone(out)).not.toThrow();
    });

    it('unwraps refs (including nested refs)', () => {
        expect(toPlain(ref('x'))).toBe('x');
        const input = { page: shallowRef({ id: 1 }), list: [ref(2), ref(3)] };
        expect(toPlain(input)).toEqual({ page: { id: 1 }, list: [2, 3] });
    });

    it('unwraps arrays of reactive items', () => {
        const input = reactive([{ a: 1 }, { a: 2 }]);
        const out = toPlain(input);
        expect(out).toEqual([{ a: 1 }, { a: 2 }]);
        expect(() => structuredClone(out)).not.toThrow();
    });

    it('handles circular references without infinite recursion', () => {
        const input: Record<string, unknown> = { name: 'root' };
        input.self = input;
        const out = toPlain(input) as Record<string, unknown>;
        expect(out.name).toBe('root');
        expect(out.self).toBe(out); // cycle preserved, pointing at the copy
    });

    it('preserves non-plain objects (e.g. Date) as-is', () => {
        const date = new Date('2026-01-01T00:00:00Z');
        const out = toPlain({ when: date });
        expect(out.when).toBe(date);
        expect(() => structuredClone(out)).not.toThrow();
    });

    it('produces a structured-clone-safe copy of a nested reactive page response', () => {
        const page = reactive({
            pageAsset: {
                page: { pageURI: '/index' },
                layout: { body: { rows: [{ columns: [{ containers: [] }] }] } }
            },
            content: {}
        });
        const out = toPlain(page);
        expect(() => structuredClone(out)).not.toThrow();
        expect(out.pageAsset.page.pageURI).toBe('/index');
    });
});
