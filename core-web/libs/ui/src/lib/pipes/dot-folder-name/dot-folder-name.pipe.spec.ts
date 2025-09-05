import { FolderNamePipe } from './dot-folder-name.pipe';

describe('FolderNamePipe', () => {
    let pipe: FolderNamePipe;

    beforeEach(() => {
        pipe = new FolderNamePipe();
    });

    describe('transform', () => {
        it('should extract folder name from simple path', () => {
            const result = pipe.transform('/application/content');
            expect(result).toBe('content');
        });

        it('should extract folder name from path with double slashes', () => {
            const result = pipe.transform('//application/content');
            expect(result).toBe('content');
        });

        it('should extract folder name from nested path', () => {
            const result = pipe.transform('/application/content/images/2024');
            expect(result).toBe('2024');
        });

        it('should extract folder name from path without leading slash', () => {
            const result = pipe.transform('application/content/images');
            expect(result).toBe('images');
        });

        it('should handle single folder name without slashes', () => {
            const result = pipe.transform('documents');
            expect(result).toBe('documents');
        });

        it('should handle path with trailing slash', () => {
            const result = pipe.transform('/application/content/');
            expect(result).toBe('content');
        });

        it('should handle path with multiple trailing slashes', () => {
            const result = pipe.transform('/application/content///');
            expect(result).toBe('content');
        });

        it('should return empty string for empty path', () => {
            const result = pipe.transform('');
            expect(result).toBe('');
        });

        it('should return empty string for root path with single slash', () => {
            const result = pipe.transform('/');
            expect(result).toBe('');
        });

        it('should return empty string for root path with double slashes', () => {
            const result = pipe.transform('//');
            expect(result).toBe('');
        });

        it('should return empty string for path with only slashes', () => {
            const result = pipe.transform('///');
            expect(result).toBe('');
        });

        it('should handle folder names with special characters', () => {
            const result = pipe.transform('/application/test-folder_2024');
            expect(result).toBe('test-folder_2024');
        });

        it('should handle folder names with dots', () => {
            const result = pipe.transform('/application/content/v1.2.3');
            expect(result).toBe('v1.2.3');
        });

        it('should handle folder names with spaces (URL encoded)', () => {
            const result = pipe.transform('/application/My Documents');
            expect(result).toBe('My Documents');
        });

        it('should handle complex nested path with mixed slashes', () => {
            const result = pipe.transform('//application//content///images//final');
            expect(result).toBe('final');
        });

        it('should handle path with only one segment after slashes', () => {
            const result = pipe.transform('//folder');
            expect(result).toBe('folder');
        });

        it('should handle very deep nested path', () => {
            const result = pipe.transform('/level1/level2/level3/level4/level5/final-folder');
            expect(result).toBe('final-folder');
        });
    });
});
