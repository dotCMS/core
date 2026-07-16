import { appendMappedParams } from './utils';

describe('appendMappedParams', () => {
    let searchParams: URLSearchParams;

    beforeEach(() => {
        searchParams = new URLSearchParams();
    });

    it('should append mapped parameters with correct keys', () => {
        const sourceObject = {
            limit: 10,
            offset: 0,
            siteId: 'default'
        };

        const mapping: Array<[string, string]> = [
            ['searchLimit', 'limit'],
            ['searchOffset', 'offset'],
            ['site', 'siteId']
        ];

        appendMappedParams(searchParams, sourceObject, mapping);

        expect(searchParams.get('searchLimit')).toBe('10');
        expect(searchParams.get('searchOffset')).toBe('0');
        expect(searchParams.get('site')).toBe('default');
    });

    it('should skip undefined values', () => {
        const sourceObject = {
            limit: 10,
            offset: undefined,
            siteId: 'default'
        };

        const mapping: Array<[string, string]> = [
            ['searchLimit', 'limit'],
            ['searchOffset', 'offset'],
            ['site', 'siteId']
        ];

        appendMappedParams(searchParams, sourceObject, mapping);

        expect(searchParams.get('searchLimit')).toBe('10');
        expect(searchParams.get('searchOffset')).toBeNull();
        expect(searchParams.get('site')).toBe('default');
    });

    it('should handle different data types correctly', () => {
        const sourceObject = {
            stringValue: 'hello',
            numberValue: 42,
            booleanValue: true,
            zeroValue: 0,
            falseValue: false
        };

        const mapping: Array<[string, string]> = [
            ['str', 'stringValue'],
            ['num', 'numberValue'],
            ['bool', 'booleanValue'],
            ['zero', 'zeroValue'],
            ['false', 'falseValue']
        ];

        appendMappedParams(searchParams, sourceObject, mapping);

        expect(searchParams.get('str')).toBe('hello');
        expect(searchParams.get('num')).toBe('42');
        expect(searchParams.get('bool')).toBe('true');
        expect(searchParams.get('zero')).toBe('0');
        expect(searchParams.get('false')).toBe('false');
    });

    it('should handle empty mapping array', () => {
        const sourceObject = {
            limit: 10,
            offset: 0
        };

        appendMappedParams(searchParams, sourceObject, []);

        expect(searchParams.toString()).toBe('');
    });

    it('should handle empty source object', () => {
        const sourceObject = {};

        const mapping: Array<[string, string]> = [
            ['searchLimit', 'limit'],
            ['searchOffset', 'offset']
        ];

        appendMappedParams(searchParams, sourceObject, mapping);

        expect(searchParams.toString()).toBe('');
    });

    it('should skip mappings for non-existent source keys', () => {
        const sourceObject = {
            limit: 10
        };

        const mapping: Array<[string, string]> = [
            ['searchLimit', 'limit'],
            ['searchOffset', 'offset'],
            ['site', 'siteId']
        ];

        appendMappedParams(searchParams, sourceObject, mapping);

        expect(searchParams.get('searchLimit')).toBe('10');
        expect(searchParams.get('searchOffset')).toBeNull();
        expect(searchParams.get('site')).toBeNull();
    });

    it('should append to existing URLSearchParams without overwriting', () => {
        searchParams.append('existing', 'value');

        const sourceObject = {
            limit: 10
        };

        const mapping: Array<[string, string]> = [['searchLimit', 'limit']];

        appendMappedParams(searchParams, sourceObject, mapping);

        expect(searchParams.get('existing')).toBe('value');
        expect(searchParams.get('searchLimit')).toBe('10');
    });

    it('should handle null values by skipping them', () => {
        const sourceObject = {
            limit: 10,
            offset: null,
            siteId: 'default'
        };

        const mapping: Array<[string, string]> = [
            ['searchLimit', 'limit'],
            ['searchOffset', 'offset'],
            ['site', 'siteId']
        ];

        appendMappedParams(searchParams, sourceObject, mapping);

        expect(searchParams.get('searchLimit')).toBe('10');
        expect(searchParams.get('searchOffset')).toBe('null');
        expect(searchParams.get('site')).toBe('default');
    });

    it('should handle complex object values by converting to string', () => {
        const sourceObject = {
            arrayValue: [1, 2, 3],
            objectValue: { key: 'value' }
        };

        const mapping: Array<[string, string]> = [
            ['arr', 'arrayValue'],
            ['obj', 'objectValue']
        ];

        appendMappedParams(searchParams, sourceObject, mapping);

        expect(searchParams.get('arr')).toBe('1,2,3');
        expect(searchParams.get('obj')).toBe('[object Object]');
    });

    it('should handle mapping with same target key multiple times', () => {
        const sourceObject = {
            value1: 'first',
            value2: 'second'
        };

        const mapping: Array<[string, string]> = [
            ['key', 'value1'],
            ['key', 'value2']
        ];

        appendMappedParams(searchParams, sourceObject, mapping);

        expect(searchParams.getAll('key')).toEqual(['first', 'second']);
    });

    it('should work with real-world AI search query example', () => {
        const query = {
            limit: 20,
            offset: 0,
            siteId: 'default',
            languageId: 1,
            contentType: 'BlogPost'
        };

        const mapping: Array<[string, string]> = [
            ['searchLimit', 'limit'],
            ['searchOffset', 'offset'],
            ['site', 'siteId'],
            ['language', 'languageId'],
            ['contentType', 'contentType']
        ];

        appendMappedParams(searchParams, query, mapping);

        expect(searchParams.toString()).toBe(
            'searchLimit=20&searchOffset=0&site=default&language=1&contentType=BlogPost'
        );
    });

    describe('same key mapping (single-element array)', () => {
        it('should use same key for source and target when single-element array is provided', () => {
            const sourceObject = {
                indexName: 'content_index',
                query: 'test query',
                threshold: 0.7
            };

            const mapping: Array<[string, string] | [string]> = [
                ['indexName'],
                ['query'],
                ['threshold']
            ];

            appendMappedParams(searchParams, sourceObject, mapping);

            expect(searchParams.get('indexName')).toBe('content_index');
            expect(searchParams.get('query')).toBe('test query');
            expect(searchParams.get('threshold')).toBe('0.7');
        });

        it('should mix two-element and single-element array mapping', () => {
            const sourceObject = {
                limit: 10,
                offset: 0,
                siteId: 'default',
                indexName: 'content_index',
                query: 'test query'
            };

            const mapping: Array<[string, string] | [string]> = [
                ['searchLimit', 'limit'],
                ['searchOffset', 'offset'],
                ['site', 'siteId'],
                ['indexName'],
                ['query']
            ];

            appendMappedParams(searchParams, sourceObject, mapping);

            expect(searchParams.get('searchLimit')).toBe('10');
            expect(searchParams.get('searchOffset')).toBe('0');
            expect(searchParams.get('site')).toBe('default');
            expect(searchParams.get('indexName')).toBe('content_index');
            expect(searchParams.get('query')).toBe('test query');
        });

        it('should skip undefined values with single-element array mapping', () => {
            const sourceObject = {
                indexName: 'content_index',
                query: undefined,
                threshold: 0.7
            };

            const mapping: Array<[string]> = [['indexName'], ['query'], ['threshold']];

            appendMappedParams(searchParams, sourceObject, mapping);

            expect(searchParams.get('indexName')).toBe('content_index');
            expect(searchParams.get('query')).toBeNull();
            expect(searchParams.get('threshold')).toBe('0.7');
        });

        it('should skip non-existent keys with single-element array mapping', () => {
            const sourceObject = {
                indexName: 'content_index'
            };

            const mapping: Array<[string]> = [['indexName'], ['query'], ['threshold']];

            appendMappedParams(searchParams, sourceObject, mapping);

            expect(searchParams.get('indexName')).toBe('content_index');
            expect(searchParams.get('query')).toBeNull();
            expect(searchParams.get('threshold')).toBeNull();
        });

        it('should work with real-world mixed mapping example', () => {
            const params = {
                limit: 20,
                offset: 0,
                siteId: 'default',
                indexName: 'content_index',
                query: 'machine learning',
                threshold: 0.7,
                model: 'text-embedding-ada-002'
            };

            const mapping: Array<[string, string] | [string]> = [
                ['searchLimit', 'limit'],
                ['searchOffset', 'offset'],
                ['site', 'siteId'],
                ['indexName'],
                ['query'],
                ['threshold'],
                ['model']
            ];

            appendMappedParams(searchParams, params, mapping);

            expect(searchParams.toString()).toBe(
                'searchLimit=20&searchOffset=0&site=default&indexName=content_index&query=machine+learning&threshold=0.7&model=text-embedding-ada-002'
            );
        });
    });
});
