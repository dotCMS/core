import { createFakeContentlet } from '@dotcms/utils-testing';

import { getRelationshipFromContentlet } from './relationshipFromContentlet';

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
