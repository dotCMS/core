import {
    LegacyRoleSearchNode,
    RoleHierarchyEntry,
    sanitizeRoleForm,
    toRoleMemberResults,
    unwrapLegacySearchNode
} from './dot-roles.adapters';

import { DotRoleFormValue } from '../models/dot-roles.models';

describe('dot-roles.adapters', () => {
    describe('unwrapLegacySearchNode', () => {
        it('restores dashes in the id (Dojo tree DnD artifact)', () => {
            const legacy: LegacyRoleSearchNode = {
                id: 'abc_123_def_456',
                name: 'Editor'
            };

            const result = unwrapLegacySearchNode(legacy);

            expect(result.id).toBe('abc-123-def-456');
        });

        it('preserves name and locked flag', () => {
            const legacy: LegacyRoleSearchNode = {
                id: 'a',
                name: 'System Role',
                locked: true
            };

            expect(unwrapLegacySearchNode(legacy)).toMatchObject({
                name: 'System Role',
                locked: true
            });
        });

        it('leaves `locked` undefined when the source omits it (no default)', () => {
            const result = unwrapLegacySearchNode({ id: 'a', name: 'Free' });

            expect(result.locked).toBeUndefined();
        });

        it('adapts nested children recursively', () => {
            const legacy: LegacyRoleSearchNode = {
                id: 'parent_1',
                name: 'Parent',
                children: [
                    {
                        id: 'child_a_1',
                        name: 'Child A',
                        children: [{ id: 'grand_a_1', name: 'Grand A' }]
                    }
                ]
            };

            const result = unwrapLegacySearchNode(legacy);

            expect(result.roleChildren?.[0].id).toBe('child-a-1');
            expect(result.roleChildren?.[0].roleChildren?.[0].id).toBe('grand-a-1');
        });

        it('normalizes missing children to an empty array', () => {
            const result = unwrapLegacySearchNode({ id: 'a', name: 'A' });

            expect(result.roleChildren).toEqual([]);
        });
    });

    describe('toRoleMemberResults', () => {
        it('filters non-user entries out (ancestors of the role are skipped)', () => {
            const entries: RoleHierarchyEntry[] = [
                { id: 'r1', name: 'Reviewer', user: false },
                { id: 'u1', roleKey: 'user-1', name: 'Jane Doe', user: true }
            ];

            const result = toRoleMemberResults(entries);

            expect(result).toHaveLength(1);
            expect(result[0].userId).toBe('user-1');
        });

        it('prefers roleKey as userId, falls back to id', () => {
            const entries: RoleHierarchyEntry[] = [
                { id: 'ur-1', roleKey: 'user-key-1', name: 'A', user: true },
                { id: 'ur-2', name: 'B', user: true }
            ];

            expect(toRoleMemberResults(entries).map((r) => r.userId)).toEqual([
                'user-key-1',
                'ur-2'
            ]);
        });

        it('splits `name` on the first space (firstName / lastName)', () => {
            const entries: RoleHierarchyEntry[] = [
                { id: 'u', roleKey: 'u', name: 'María del Carmen', user: true }
            ];

            const [row] = toRoleMemberResults(entries);
            expect(row.firstName).toBe('María');
            expect(row.lastName).toBe('del Carmen');
        });

        it('handles single-word names (lastName is empty)', () => {
            const [row] = toRoleMemberResults([
                { id: 'u', roleKey: 'u', name: 'Prince', user: true }
            ]);

            expect(row.firstName).toBe('Prince');
            expect(row.lastName).toBe('');
        });

        it('tolerates a missing name', () => {
            const [row] = toRoleMemberResults([{ id: 'u', roleKey: 'u', user: true }]);

            expect(row.firstName).toBe('');
            expect(row.lastName).toBe('');
        });

        it('always returns an empty email (endpoint returns Role objects, not Users)', () => {
            const [row] = toRoleMemberResults([{ id: 'u', roleKey: 'u', name: 'X Y', user: true }]);

            expect(row.emailAddress).toBe('');
        });

        it('returns an empty array when the input has no user rows', () => {
            const entries: RoleHierarchyEntry[] = [
                { id: 'a', name: 'A', user: false },
                { id: 'b', name: 'B' }
            ];

            expect(toRoleMemberResults(entries)).toEqual([]);
        });
    });

    describe('sanitizeRoleForm', () => {
        const base: DotRoleFormValue = {
            roleName: 'New Role',
            roleKey: 'new-role',
            parentRoleId: 'parent-1',
            description: 'desc',
            canEditUsers: true,
            canEditPermissions: true,
            canEditLayouts: true
        };

        it('leaves populated values untouched', () => {
            expect(sanitizeRoleForm(base)).toEqual(base);
        });

        it('drops an empty roleKey to undefined so JSON.stringify omits it (avoids UNIQUE violation)', () => {
            const result = sanitizeRoleForm({ ...base, roleKey: '' });

            expect(result.roleKey).toBeUndefined();
        });

        it('drops a whitespace-only roleKey to undefined', () => {
            const result = sanitizeRoleForm({ ...base, roleKey: '   ' });

            expect(result.roleKey).toBeUndefined();
        });

        it('trims a padded roleKey', () => {
            const result = sanitizeRoleForm({ ...base, roleKey: '  editor  ' });

            expect(result.roleKey).toBe('editor');
        });

        it('drops an empty description to undefined', () => {
            const result = sanitizeRoleForm({ ...base, description: '' });

            expect(result.description).toBeUndefined();
        });

        it('drops a whitespace-only description', () => {
            const result = sanitizeRoleForm({ ...base, description: '\n \t' });

            expect(result.description).toBeUndefined();
        });

        it('coerces a null parentRoleId to undefined so the BE reparents to root', () => {
            const result = sanitizeRoleForm({ ...base, parentRoleId: null });

            expect(result.parentRoleId).toBeUndefined();
        });

        it('preserves booleans / other primitives verbatim', () => {
            const result = sanitizeRoleForm({
                ...base,
                canEditUsers: false,
                canEditLayouts: false
            });

            expect(result.canEditUsers).toBe(false);
            expect(result.canEditLayouts).toBe(false);
        });
    });
});
