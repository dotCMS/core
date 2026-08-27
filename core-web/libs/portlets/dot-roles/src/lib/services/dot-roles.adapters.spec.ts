import {
    LegacyRoleSearchNode,
    sanitizeRoleForm,
    unwrapLegacySearchNode
} from './dot-roles.adapters';

import { DotRoleFormValue } from '../models/dot-roles.models';

describe('dot-roles.adapters', () => {
    describe('unwrapLegacySearchNode', () => {
        it('restores dashes in the id (Dojo tree DnD artifact)', () => {
            const legacy: LegacyRoleSearchNode = {
                id: 'abc1a2b3_c4d5_e6f7_8899_aabbccddeeff',
                name: 'Editor'
            };

            const result = unwrapLegacySearchNode(legacy);

            expect(result.id).toBe('abc1a2b3-c4d5-e6f7-8899-aabbccddeeff');
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
                id: 'aaaaaaaa_1111_1111_1111_111111111111',
                name: 'Parent',
                children: [
                    {
                        id: 'bbbbbbbb_2222_2222_2222_222222222222',
                        name: 'Child A',
                        children: [{ id: 'cccccccc_3333_3333_3333_333333333333', name: 'Grand A' }]
                    }
                ]
            };

            const result = unwrapLegacySearchNode(legacy);

            expect(result.roleChildren?.[0].id).toBe('bbbbbbbb-2222-2222-2222-222222222222');
            expect(result.roleChildren?.[0].roleChildren?.[0].id).toBe(
                'cccccccc-3333-3333-3333-333333333333'
            );
        });

        it('normalizes missing children to an empty array', () => {
            const result = unwrapLegacySearchNode({ id: 'a', name: 'A' });

            expect(result.roleChildren).toEqual([]);
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
