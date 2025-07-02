import { decodeFilters } from './functions';

describe('Utility Functions', () => {
    describe('decodeFilters', () => {
        it('should return an empty object when input is empty string', () => {
            const result = decodeFilters('');
            expect(result).toEqual({});
        });

        it('should return an empty object when input is undefined', () => {
            const result = decodeFilters(undefined as unknown as string);
            expect(result).toEqual({});
        });

        it('should decode a single filter correctly', () => {
            const result = decodeFilters('contentType:Blog');
            expect(result).toEqual({ contentType: 'Blog' });
        });

        it('should decode multiple filters correctly', () => {
            const result = decodeFilters('contentType:Blog;status:published');
            expect(result).toEqual({ contentType: 'Blog', status: 'published' });
        });

        it('should handle filters with spaces correctly', () => {
            const result = decodeFilters('contentType:Blog; status:published');
            expect(result).toEqual({ contentType: 'Blog', status: 'published' });
        });

        it('should ignore empty filter parts - edge case', () => {
            const result = decodeFilters('contentType:Blog;;status:published;');
            expect(result).toEqual({ contentType: 'Blog', status: 'published' });
        });

        it('should overwrite duplicated keys with the last value - edge case', () => {
            const result = decodeFilters('contentType:Blog;contentType:News');
            expect(result).toEqual({ contentType: 'News' });
        });
    });
});
