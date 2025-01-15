import { createFakeContentlet, createFakeRelationshipField } from '@dotcms/utils-testing';

import {
    getSelectionModeByCardinality,
    getRelationshipFromContentlet,
    getContentTypeIdFromRelationship
} from './index';

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

    describe('getRelationshipFromContentlet', () => {
        it('should return empty array when contentlet is null', () => {
            const result = getRelationshipFromContentlet({
                contentlet: null,
                variable: 'testVar'
            });
            expect(result).toEqual([]);
        });

        it('should return empty array when variable is empty', () => {
            const result = getRelationshipFromContentlet({
                contentlet: createFakeContentlet(),
                variable: ''
            });
            expect(result).toEqual([]);
        });

        it('should return array of relationships when contentlet has array relationship', () => {
            const relationships = [createFakeContentlet(), createFakeContentlet()];
            const contentlet = {
                ...createFakeContentlet(),
                testVar: relationships
            };

            const result = getRelationshipFromContentlet({
                contentlet,
                variable: 'testVar'
            });
            expect(result).toEqual(relationships);
        });

        it('should convert single relationship to array', () => {
            const singleRelationship = createFakeContentlet();
            const contentlet = {
                ...createFakeContentlet(),
                testVar: singleRelationship
            };

            const result = getRelationshipFromContentlet({
                contentlet,
                variable: 'testVar'
            });
            expect(result).toEqual([singleRelationship]);
        });

        it('should return empty array when relationship variable is null', () => {
            const contentlet = {
                ...createFakeContentlet(),
                testVar: null
            };

            const result = getRelationshipFromContentlet({
                contentlet,
                variable: 'testVar'
            });
            expect(result).toEqual([]);
        });

        it('should return empty array when relationship variable is undefined', () => {
            const contentlet = {
                ...createFakeContentlet(),
                testVar: undefined
            };

            const result = getRelationshipFromContentlet({
                contentlet,
                variable: 'testVar'
            });
            expect(result).toEqual([]);
        });

        it('should return empty array when relationship is not an array', () => {
            const contentlet = {
                ...createFakeContentlet(),
                testVar: 'not an array'
            };

            const result = getRelationshipFromContentlet({
                contentlet,
                variable: 'testVar'
            });
            expect(result).toEqual([]);
        });

        it('should return empty array when relationship variable does not exist', () => {
            const contentlet = {
                ...createFakeContentlet(),
                otherVar: []
            };

            const result = getRelationshipFromContentlet({
                contentlet,
                variable: 'testVar'
            });
            expect(result).toEqual([]);
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

        it('should throw error when relationships property is missing', () => {
            const field = createFakeRelationshipField({
                contentTypeId: null,
                relationships: null
            });

            expect(() => getContentTypeIdFromRelationship(field)).toThrow(
                'Content type ID not found in relationship field'
            );
        });

        it('should throw error when velocityVar is missing', () => {
            const field = createFakeRelationshipField({
                relationships: null
            });

            expect(() => getContentTypeIdFromRelationship(field)).toThrow(
                'Content type ID not found in relationship field'
            );
        });

        it('should throw error when velocityVar has invalid format', () => {
            const field = createFakeRelationshipField({
                relationships: {
                    cardinality: 0,
                    isParentField: true,
                    velocityVar: ''
                }
            });

            expect(() => getContentTypeIdFromRelationship(field)).toThrow(
                'Content type ID not found in relationship field'
            );
        });
    });
});
