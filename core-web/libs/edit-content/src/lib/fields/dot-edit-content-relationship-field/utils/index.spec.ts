import { createFakeRelationshipField } from '@dotcms/utils-testing';

import {
    getSelectionModeByCardinality,
    getContentTypeIdFromRelationship,
    needsCardinalityConstraintCheck
} from './index';

describe('Relationship Field Utils', () => {
    describe('getSelectionModeByCardinality', () => {
        describe('parent side (isParentField=true)', () => {
            it('should return "single" for ONE_TO_ONE', () => {
                expect(getSelectionModeByCardinality(2, true)).toBe('single');
            });

            it('should return "multiple" for ONE_TO_MANY', () => {
                expect(getSelectionModeByCardinality(0, true)).toBe('multiple');
            });

            it('should return "multiple" for MANY_TO_MANY', () => {
                expect(getSelectionModeByCardinality(1, true)).toBe('multiple');
            });

            it('should return "single" for MANY_TO_ONE', () => {
                expect(getSelectionModeByCardinality(3, true)).toBe('single');
            });
        });

        describe('child side (isParentField=false)', () => {
            it('should return "single" for ONE_TO_ONE', () => {
                expect(getSelectionModeByCardinality(2, false)).toBe('single');
            });

            it('should return "single" for ONE_TO_MANY', () => {
                expect(getSelectionModeByCardinality(0, false)).toBe('single');
            });

            it('should return "multiple" for MANY_TO_MANY', () => {
                expect(getSelectionModeByCardinality(1, false)).toBe('multiple');
            });

            it('should return "single" for MANY_TO_ONE', () => {
                expect(getSelectionModeByCardinality(3, false)).toBe('single');
            });
        });

        describe('backward compatible (no isParentField)', () => {
            it('should return "single" for ONE_TO_ONE', () => {
                expect(getSelectionModeByCardinality(2)).toBe('single');
            });

            it('should return "multiple" for ONE_TO_MANY', () => {
                expect(getSelectionModeByCardinality(0)).toBe('multiple');
            });
        });

        it('should throw error for invalid cardinality', () => {
            const invalidCardinality = 999;
            expect(() => getSelectionModeByCardinality(invalidCardinality)).toThrow(
                `Invalid relationship type for cardinality: ${invalidCardinality}`
            );
        });
    });

    describe('getContentTypeIdFromRelationship', () => {
        it('should extract content type ID from relationship field with dot notation', () => {
            const field = createFakeRelationshipField({
                relationships: {
                    cardinality: 0,
                    isParentField: true,
                    velocityVar: 'contentTypeId.fieldName'
                }
            });

            const result = getContentTypeIdFromRelationship(field);
            expect(result).toBe('contentTypeId');
        });

        it('should extract content type ID from relationship field', () => {
            const field = createFakeRelationshipField({
                relationships: {
                    cardinality: 0,
                    isParentField: true,
                    velocityVar: 'contentTypeId'
                }
            });

            const result = getContentTypeIdFromRelationship(field);
            expect(result).toBe('contentTypeId');
        });

        it('should return null when relationships property is missing', () => {
            const field = createFakeRelationshipField({
                contentTypeId: null,
                relationships: null
            });

            const result = getContentTypeIdFromRelationship(field);
            expect(result).toBeNull();
        });

        it('should return null when velocityVar is missing', () => {
            const field = createFakeRelationshipField({
                relationships: null
            });

            const result = getContentTypeIdFromRelationship(field);
            expect(result).toBeNull();
        });

        it('should return null when velocityVar has invalid format', () => {
            const field = createFakeRelationshipField({
                relationships: {
                    cardinality: 0,
                    isParentField: true,
                    velocityVar: ''
                }
            });

            const result = getContentTypeIdFromRelationship(field);
            expect(result).toBeNull();
        });

        it('should return null when velocityVar starts with dot', () => {
            const field = createFakeRelationshipField({
                relationships: {
                    cardinality: 0,
                    isParentField: true,
                    velocityVar: '.fieldName'
                }
            });

            const result = getContentTypeIdFromRelationship(field);
            expect(result).toBeNull();
        });
    });

    describe('needsCardinalityConstraintCheck', () => {
        it('should return true for ONE_TO_ONE when isParentField is true', () => {
            expect(needsCardinalityConstraintCheck(2, true)).toBe(true);
        });

        it('should return true for ONE_TO_MANY when isParentField is true', () => {
            expect(needsCardinalityConstraintCheck(0, true)).toBe(true);
        });

        it('should return false for ONE_TO_ONE when isParentField is false', () => {
            expect(needsCardinalityConstraintCheck(2, false)).toBe(false);
        });

        it('should return false for ONE_TO_MANY when isParentField is false', () => {
            expect(needsCardinalityConstraintCheck(0, false)).toBe(false);
        });

        it('should return false for MANY_TO_MANY regardless of isParentField', () => {
            expect(needsCardinalityConstraintCheck(1, true)).toBe(false);
            expect(needsCardinalityConstraintCheck(1, false)).toBe(false);
        });

        it('should return false for MANY_TO_ONE regardless of isParentField', () => {
            expect(needsCardinalityConstraintCheck(3, true)).toBe(false);
            expect(needsCardinalityConstraintCheck(3, false)).toBe(false);
        });

        it('should return false for invalid cardinality', () => {
            expect(needsCardinalityConstraintCheck(999, true)).toBe(false);
        });
    });
});
