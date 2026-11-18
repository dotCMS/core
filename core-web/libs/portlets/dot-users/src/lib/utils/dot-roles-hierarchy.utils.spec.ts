import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { DotRole } from '@dotcms/dotcms-models';

import { flattenRoleHierarchy } from './dot-roles-hierarchy.utils';

const role = (id: string, extra: Partial<DotRole> = {}): DotRole => ({
    id,
    name: `Role ${id}`,
    ...extra
});

describe('flattenRoleHierarchy', () => {
    it('flattens roots and their hydrated first level, setting parent from the traversal', () =>
        new Promise<void>((done) => {
            const roots = [role('r1', { roleChildren: [role('c1', { childCount: 0 })] })];
            const fetch = vi.fn();

            flattenRoleHierarchy(roots, fetch).subscribe((flat) => {
                expect(flat.map((r) => [r.id, r.parent])).toEqual([
                    ['r1', undefined],
                    ['c1', 'r1']
                ]);
                done();
            });
        }));

    it("clears a root's self-referential parent so a parent-id lookup works", () =>
        new Promise<void>((done) => {
            // The backend marks a root with `parent === id`; left as-is it would
            // make the root look like its own child.
            const roots = [role('r1', { parent: 'r1', childCount: 0 })];

            flattenRoleHierarchy(roots, vi.fn()).subscribe((flat) => {
                expect(flat[0].parent).toBeUndefined();
                done();
            });
        }));

    it('keeps a genuine parent on the top level — it is not necessarily the true root', () =>
        new Promise<void>((done) => {
            // Callers may hand in a mid-hierarchy level whose `parent` is real.
            const roots = [role('mid', { parent: 'above', childCount: 0 })];

            flattenRoleHierarchy(roots, vi.fn()).subscribe((flat) => {
                expect(flat[0].parent).toBe('above');
                done();
            });
        }));

    it('drops roleChildren from the flattened nodes', () =>
        new Promise<void>((done) => {
            const roots = [role('r1', { roleChildren: [role('c1', { childCount: 0 })] })];

            flattenRoleHierarchy(roots, vi.fn()).subscribe((flat) => {
                expect(flat.every((r) => r.roleChildren === undefined)).toBe(true);
                done();
            });
        }));

    it('does NOT fetch children for a node with childCount 0 (#37071 pruning)', () =>
        new Promise<void>((done) => {
            const roots = [
                role('r1', {
                    roleChildren: [role('c1', { childCount: 0 }), role('c2', { childCount: 0 })]
                })
            ];
            const fetch = vi.fn();

            flattenRoleHierarchy(roots, fetch).subscribe((flat) => {
                expect(fetch).not.toHaveBeenCalled();
                expect(flat).toHaveLength(3);
                done();
            });
        }));

    it('fetches only the children that report childCount > 0', () =>
        new Promise<void>((done) => {
            const roots = [
                role('r1', {
                    roleChildren: [role('c1', { childCount: 1 }), role('c2', { childCount: 0 })]
                })
            ];
            const fetch = vi
                .fn()
                .mockReturnValue(of(role('c1', { roleChildren: [role('g1', { childCount: 0 })] })));

            flattenRoleHierarchy(roots, fetch).subscribe((flat) => {
                expect(fetch).toHaveBeenCalledTimes(1);
                expect(fetch).toHaveBeenCalledWith('c1');
                expect(flat.map((r) => r.id)).toEqual(['r1', 'c1', 'c2', 'g1']);
                expect(flat.find((r) => r.id === 'g1')?.parent).toBe('c1');
                done();
            });
        }));

    it('still visits nodes whose childCount is absent, so an older serializer degrades to the exhaustive walk', () =>
        new Promise<void>((done) => {
            const roots = [role('r1', { roleChildren: [role('c1')] })];
            const fetch = vi.fn().mockReturnValue(of(role('c1', { roleChildren: [] })));

            flattenRoleHierarchy(roots, fetch).subscribe(() => {
                expect(fetch).toHaveBeenCalledWith('c1');
                done();
            });
        }));

    it('deduplicates roles that appear through more than one path', () =>
        new Promise<void>((done) => {
            const roots = [
                role('r1', { roleChildren: [role('shared', { childCount: 0 })] }),
                role('r2', { roleChildren: [role('shared', { childCount: 0 })] })
            ];

            flattenRoleHierarchy(roots, vi.fn()).subscribe((flat) => {
                expect(flat.filter((r) => r.id === 'shared')).toHaveLength(1);
                done();
            });
        }));

    it('propagates a failed lookup instead of yielding a silently partial tree', () =>
        new Promise<void>((done) => {
            const roots = [role('r1', { roleChildren: [role('c1', { childCount: 2 })] })];
            const fetch = vi.fn().mockReturnValue(throwError(() => new Error('boom')));

            flattenRoleHierarchy(roots, fetch).subscribe({
                next: () => expect.fail('should not emit a partial hierarchy'),
                error: (error) => {
                    expect(error.message).toBe('boom');
                    done();
                }
            });
        }));
});
