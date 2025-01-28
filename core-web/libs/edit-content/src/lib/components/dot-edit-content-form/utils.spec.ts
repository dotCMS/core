import {
    createFakeCategoryField,
    createFakeContentlet,
    createFakeHostFolderField,
    createFakeLineDividerField,
    createFakeRelationshipField
} from '@dotcms/utils-testing';

import { resolutionValue } from './utils';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

describe('Utils', () => {
    describe('resolutionValue', () => {
        // Host Folder Tests
        describe('Host Folder Resolution', () => {
            it('should return hostName when hostName and url exist', () => {
                const contentlet = createFakeContentlet({
                    hostName: 'demo.dotcms.com',
                    url: '/content/generic/index'
                });
                const field = createFakeHostFolderField({ defaultValue: '' });

                expect(resolutionValue[FIELD_TYPES.HOST_FOLDER](contentlet, field)).toBe(
                    'demo.dotcms.com'
                );
            });

            it('should return default value when hostName or url is missing', () => {
                const contentlet = createFakeContentlet({ hostName: null, url: null });
                const field = createFakeHostFolderField({ defaultValue: 'default' });

                expect(resolutionValue[FIELD_TYPES.HOST_FOLDER](contentlet, field)).toBe('default');
            });

            it('should return empty string when no default value and no path', () => {
                const contentlet = createFakeContentlet({
                    hostName: null,
                    url: null
                });
                const field = createFakeHostFolderField({ defaultValue: null });

                expect(resolutionValue[FIELD_TYPES.HOST_FOLDER](contentlet, field)).toBe('');
            });
        });

        // Category Tests
        describe('Category Resolution', () => {
            it('should return array of category keys', () => {
                const contentlet = createFakeContentlet({
                    categories: [{ key1: 'value1' }, { key2: 'value2' }]
                });
                const field = createFakeCategoryField({ variable: 'categories' });

                expect(resolutionValue[FIELD_TYPES.CATEGORY](contentlet, field)).toEqual([
                    'key1',
                    'key2'
                ]);
            });

            it('should return default value when no categories exist', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeCategoryField({ defaultValue: '[]' });

                expect(resolutionValue[FIELD_TYPES.CATEGORY](contentlet, field)).toEqual('[]');
            });

            it('should handle non-array values gracefully', () => {
                const contentlet = createFakeContentlet({
                    categories: 'invalid'
                });
                const field = createFakeCategoryField({
                    variable: 'categories',
                    defaultValue: null
                });

                expect(resolutionValue[FIELD_TYPES.CATEGORY](contentlet, field)).toEqual([]);
            });
        });

        // Relationship Tests
        describe('Relationship Resolution', () => {
            it('should return comma-separated identifiers', () => {
                const contentlet = createFakeContentlet({
                    relationship: [{ identifier: 'id1' }, { identifier: 'id2' }]
                });
                const field = createFakeRelationshipField({ variable: 'relationship' });

                expect(resolutionValue[FIELD_TYPES.RELATIONSHIP](contentlet, field)).toBe(
                    'id1,id2'
                );
            });

            it('should handle empty relationships', () => {
                const contentlet = createFakeContentlet({
                    relationship: []
                });
                const field = createFakeRelationshipField({ variable: 'relationship' });

                expect(resolutionValue[FIELD_TYPES.RELATIONSHIP](contentlet, field)).toBe('');
            });
        });

        // Line Divider Tests
        describe('Line Divider Resolution', () => {
            it('should always return empty string', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeLineDividerField();

                expect(resolutionValue[FIELD_TYPES.LINE_DIVIDER](contentlet, field)).toBe('');
            });
        });
    });
});
