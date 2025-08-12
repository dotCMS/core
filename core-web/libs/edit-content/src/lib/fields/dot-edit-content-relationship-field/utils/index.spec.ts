import { createFakeRelationshipField } from '@dotcms/utils-testing';

import { getSelectionModeByCardinality, getContentTypeIdFromRelationship } from './index';

import { RELATIONSHIP_OPTIONS } from '../dot-edit-content-relationship-field.constants';
import { RelationshipTypes } from '../models/relationship.models';

describe('Relationship Field Utils', () => {
    describe('getSelectionModeByCardinality', () => {
        it('should return "single" for ONE_TO_ONE relationship', () => {
            const oneToOneCardinality = Object.entries(RELATIONSHIP_OPTIONS).find(
                ([_, value]) => value === RelationshipTypes.ONE_TO_ONE
            )?.[0];

            const result = getSelectionModeByCardinality(Number(oneToOneCardinality));
            expect(result).toBe('single');
        });

        it('should return "single" for MANY_TO_ONE relationship', () => {
            const manyToOneCardinality = Object.entries(RELATIONSHIP_OPTIONS).find(
                ([_, value]) => value === RelationshipTypes.MANY_TO_ONE
            )?.[0];

            const result = getSelectionModeByCardinality(Number(manyToOneCardinality));
            expect(result).toBe('single');
        });

        it('should return "multiple" for ONE_TO_MANY relationship', () => {
            const oneToManyCardinality = Object.entries(RELATIONSHIP_OPTIONS).find(
                ([_, value]) => value === RelationshipTypes.ONE_TO_MANY
            )?.[0];

            const result = getSelectionModeByCardinality(Number(oneToManyCardinality));
            expect(result).toBe('multiple');
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
});
