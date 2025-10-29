import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { DotLocalstorageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

import { DotPageFavoriteContentTypeService } from './dot-page-favorite-contentType.service';

const FAVORITE_CONTENT_TYPES_KEY = 'dot-favorite-content-types';

const MOCK_CONTENT_TYPE_1: DotCMSContentType = {
    id: 'content-type-1',
    name: 'Blog Post',
    variable: 'blogPost',
    description: 'A blog post content type',
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: 'default',
    owner: 'admin',
    system: false
} as DotCMSContentType;

const MOCK_CONTENT_TYPE_2: DotCMSContentType = {
    id: 'content-type-2',
    name: 'News Article',
    variable: 'newsArticle',
    description: 'A news article content type',
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: 'default',
    owner: 'admin',
    system: false
} as DotCMSContentType;

const MOCK_CONTENT_TYPE_3: DotCMSContentType = {
    id: 'content-type-3',
    name: 'Product',
    variable: 'product',
    description: 'A product content type',
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: 'default',
    owner: 'admin',
    system: false
} as DotCMSContentType;

describe('DotPageFavoriteContentTypeService', () => {
    let spectator: SpectatorService<DotPageFavoriteContentTypeService>;
    let localStorageService: jest.Mocked<DotLocalstorageService>;

    const createService = createServiceFactory({
        service: DotPageFavoriteContentTypeService,
        mocks: [DotLocalstorageService]
    });

    beforeEach(() => {
        spectator = createService();
        localStorageService = spectator.inject(DotLocalstorageService);
    });

    it('should be created', () => {
        expect(spectator.service).toBeTruthy();
    });

    describe('add()', () => {
        it('should add a single content type to empty favorites', () => {
            localStorageService.getItem.mockReturnValue(null);

            const result = spectator.service.add(MOCK_CONTENT_TYPE_1);

            expect(localStorageService.getItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY);
            expect(localStorageService.setItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY, [
                MOCK_CONTENT_TYPE_1
            ]);
            expect(result).toEqual([MOCK_CONTENT_TYPE_1]);
        });

        it('should add a single content type to existing favorites', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1]);

            const result = spectator.service.add(MOCK_CONTENT_TYPE_2);

            expect(localStorageService.setItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY, [
                MOCK_CONTENT_TYPE_1,
                MOCK_CONTENT_TYPE_2
            ]);
            expect(result).toEqual([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2]);
        });

        it('should add multiple content types at once', () => {
            localStorageService.getItem.mockReturnValue([]);

            const result = spectator.service.add([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2]);

            expect(localStorageService.setItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY, [
                MOCK_CONTENT_TYPE_1,
                MOCK_CONTENT_TYPE_2
            ]);
            expect(result).toEqual([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2]);
        });

        it('should not add duplicate content types by id', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1]);

            const duplicateContentType = { ...MOCK_CONTENT_TYPE_1, name: 'Updated Name' };
            const result = spectator.service.add(duplicateContentType);

            expect(localStorageService.setItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY, [
                MOCK_CONTENT_TYPE_1
            ]);
            expect(result).toEqual([MOCK_CONTENT_TYPE_1]);
            expect(result).toHaveLength(1);
        });

        it('should skip duplicate content types when adding multiple', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1]);

            const result = spectator.service.add([
                MOCK_CONTENT_TYPE_1, // duplicate
                MOCK_CONTENT_TYPE_2, // new
                MOCK_CONTENT_TYPE_3 // new
            ]);

            expect(localStorageService.setItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY, [
                MOCK_CONTENT_TYPE_1,
                MOCK_CONTENT_TYPE_2,
                MOCK_CONTENT_TYPE_3
            ]);
            expect(result).toHaveLength(3);
        });

        it('should handle empty array when adding multiple', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1]);

            const result = spectator.service.add([]);

            expect(localStorageService.setItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY, [
                MOCK_CONTENT_TYPE_1
            ]);
            expect(result).toEqual([MOCK_CONTENT_TYPE_1]);
        });

        it('should return updated array after adding', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2]);

            const result = spectator.service.add(MOCK_CONTENT_TYPE_3);

            expect(result).toEqual([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2, MOCK_CONTENT_TYPE_3]);
        });
    });

    describe('remove()', () => {
        it('should remove a content type by id', () => {
            localStorageService.getItem.mockReturnValue([
                MOCK_CONTENT_TYPE_1,
                MOCK_CONTENT_TYPE_2,
                MOCK_CONTENT_TYPE_3
            ]);

            const result = spectator.service.remove('content-type-2');

            expect(localStorageService.setItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY, [
                MOCK_CONTENT_TYPE_1,
                MOCK_CONTENT_TYPE_3
            ]);
            expect(result).toEqual([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_3]);
            expect(result).toHaveLength(2);
        });

        it('should handle removing non-existent content type', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2]);

            const result = spectator.service.remove('non-existent-id');

            expect(localStorageService.setItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY, [
                MOCK_CONTENT_TYPE_1,
                MOCK_CONTENT_TYPE_2
            ]);
            expect(result).toEqual([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2]);
            expect(result).toHaveLength(2);
        });

        it('should handle removing from empty favorites', () => {
            localStorageService.getItem.mockReturnValue([]);

            const result = spectator.service.remove('content-type-1');

            expect(localStorageService.setItem).toHaveBeenCalledWith(
                FAVORITE_CONTENT_TYPES_KEY,
                []
            );
            expect(result).toEqual([]);
        });

        it('should return empty array after removing last favorite', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1]);

            const result = spectator.service.remove('content-type-1');

            expect(localStorageService.setItem).toHaveBeenCalledWith(
                FAVORITE_CONTENT_TYPES_KEY,
                []
            );
            expect(result).toEqual([]);
        });

        it('should call localStorage.setItem with correct parameters', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2]);

            spectator.service.remove('content-type-1');

            expect(localStorageService.setItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY, [
                MOCK_CONTENT_TYPE_2
            ]);
            expect(localStorageService.setItem).toHaveBeenCalledTimes(1);
        });
    });

    describe('set()', () => {
        it('should replace all favorites with new array', () => {
            const newFavorites = [MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2];

            const result = spectator.service.set(newFavorites);

            expect(localStorageService.setItem).toHaveBeenCalledWith(
                FAVORITE_CONTENT_TYPES_KEY,
                newFavorites
            );
            expect(result).toEqual(newFavorites);
        });

        it('should set empty array', () => {
            const result = spectator.service.set([]);

            expect(localStorageService.setItem).toHaveBeenCalledWith(
                FAVORITE_CONTENT_TYPES_KEY,
                []
            );
            expect(result).toEqual([]);
        });

        it('should set single content type in array', () => {
            const favorites = [MOCK_CONTENT_TYPE_1];

            const result = spectator.service.set(favorites);

            expect(localStorageService.setItem).toHaveBeenCalledWith(
                FAVORITE_CONTENT_TYPES_KEY,
                favorites
            );
            expect(result).toEqual(favorites);
        });

        it('should overwrite existing favorites', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2]);

            const newFavorites = [MOCK_CONTENT_TYPE_3];
            const result = spectator.service.set(newFavorites);

            expect(localStorageService.setItem).toHaveBeenCalledWith(
                FAVORITE_CONTENT_TYPES_KEY,
                newFavorites
            );
            expect(result).toEqual(newFavorites);
        });

        it('should return the same array that was set', () => {
            const favorites = [MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2, MOCK_CONTENT_TYPE_3];

            const result = spectator.service.set(favorites);

            expect(result).toBe(favorites);
        });
    });

    describe('getAll()', () => {
        it('should return all favorites from localStorage', () => {
            const favorites = [MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2];
            localStorageService.getItem.mockReturnValue(favorites);

            const result = spectator.service.getAll();

            expect(localStorageService.getItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY);
            expect(result).toEqual(favorites);
        });

        it('should return empty array when no favorites exist', () => {
            localStorageService.getItem.mockReturnValue(null);

            const result = spectator.service.getAll();

            expect(result).toEqual([]);
        });

        it('should return empty array when localStorage returns undefined', () => {
            localStorageService.getItem.mockReturnValue(undefined);

            const result = spectator.service.getAll();

            expect(result).toEqual([]);
        });

        it('should return empty array when localStorage returns empty array', () => {
            localStorageService.getItem.mockReturnValue([]);

            const result = spectator.service.getAll();

            expect(result).toEqual([]);
        });

        it('should call localStorage.getItem with correct key', () => {
            localStorageService.getItem.mockReturnValue([]);

            spectator.service.getAll();

            expect(localStorageService.getItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY);
            expect(localStorageService.getItem).toHaveBeenCalledTimes(1);
        });

        it('should return array with multiple content types', () => {
            const favorites = [MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2, MOCK_CONTENT_TYPE_3];
            localStorageService.getItem.mockReturnValue(favorites);

            const result = spectator.service.getAll();

            expect(result).toEqual(favorites);
            expect(result).toHaveLength(3);
        });
    });

    describe('isFavorite()', () => {
        it('should return true when content type is in favorites', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2]);

            const result = spectator.service.isFavorite('content-type-1');

            expect(result).toBe(true);
        });

        it('should return false when content type is not in favorites', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1]);

            const result = spectator.service.isFavorite('content-type-2');

            expect(result).toBe(false);
        });

        it('should return false when favorites is empty', () => {
            localStorageService.getItem.mockReturnValue([]);

            const result = spectator.service.isFavorite('content-type-1');

            expect(result).toBe(false);
        });

        it('should return false when localStorage returns null', () => {
            localStorageService.getItem.mockReturnValue(null);

            const result = spectator.service.isFavorite('content-type-1');

            expect(result).toBe(false);
        });

        it('should check correct content type id', () => {
            localStorageService.getItem.mockReturnValue([
                MOCK_CONTENT_TYPE_1,
                MOCK_CONTENT_TYPE_2,
                MOCK_CONTENT_TYPE_3
            ]);

            expect(spectator.service.isFavorite('content-type-1')).toBe(true);
            expect(spectator.service.isFavorite('content-type-2')).toBe(true);
            expect(spectator.service.isFavorite('content-type-3')).toBe(true);
            expect(spectator.service.isFavorite('content-type-4')).toBe(false);
        });

        it('should handle id matching case sensitivity', () => {
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1]);

            expect(spectator.service.isFavorite('content-type-1')).toBe(true);
            expect(spectator.service.isFavorite('CONTENT-TYPE-1')).toBe(false);
            expect(spectator.service.isFavorite('Content-Type-1')).toBe(false);
        });

        it('should call getAll() internally', () => {
            const getAllSpy = jest.spyOn(spectator.service, 'getAll');
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1]);

            spectator.service.isFavorite('content-type-1');

            expect(getAllSpy).toHaveBeenCalled();
        });
    });

    describe('clear()', () => {
        it('should remove favorites from localStorage', () => {
            spectator.service.clear();

            expect(localStorageService.removeItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY);
        });

        it('should call localStorage.removeItem exactly once', () => {
            spectator.service.clear();

            expect(localStorageService.removeItem).toHaveBeenCalledTimes(1);
        });

        it('should clear all favorites regardless of content', () => {
            localStorageService.getItem.mockReturnValue([
                MOCK_CONTENT_TYPE_1,
                MOCK_CONTENT_TYPE_2,
                MOCK_CONTENT_TYPE_3
            ]);

            spectator.service.clear();

            expect(localStorageService.removeItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY);
        });

        it('should work when favorites is already empty', () => {
            localStorageService.getItem.mockReturnValue([]);

            spectator.service.clear();

            expect(localStorageService.removeItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY);
        });
    });

    describe('Integration Tests', () => {
        it('should add, check, and remove favorites in sequence', () => {
            // Start with empty
            localStorageService.getItem.mockReturnValue(null);

            // Add favorite
            localStorageService.getItem.mockReturnValue([]);
            spectator.service.add(MOCK_CONTENT_TYPE_1);

            // Check if favorite
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1]);
            expect(spectator.service.isFavorite('content-type-1')).toBe(true);

            // Remove favorite
            spectator.service.remove('content-type-1');

            // Check if removed
            localStorageService.getItem.mockReturnValue([]);
            expect(spectator.service.isFavorite('content-type-1')).toBe(false);
        });

        it('should handle complex workflow with multiple operations', () => {
            // Add multiple favorites
            localStorageService.getItem.mockReturnValue([]);
            spectator.service.add([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2]);

            // Verify they exist
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_2]);
            expect(spectator.service.isFavorite('content-type-1')).toBe(true);
            expect(spectator.service.isFavorite('content-type-2')).toBe(true);

            // Add another
            spectator.service.add(MOCK_CONTENT_TYPE_3);

            // Verify all exist
            localStorageService.getItem.mockReturnValue([
                MOCK_CONTENT_TYPE_1,
                MOCK_CONTENT_TYPE_2,
                MOCK_CONTENT_TYPE_3
            ]);
            const all = spectator.service.getAll();
            expect(all).toHaveLength(3);

            // Remove one
            spectator.service.remove('content-type-2');

            // Verify remaining
            localStorageService.getItem.mockReturnValue([MOCK_CONTENT_TYPE_1, MOCK_CONTENT_TYPE_3]);
            expect(spectator.service.isFavorite('content-type-1')).toBe(true);
            expect(spectator.service.isFavorite('content-type-2')).toBe(false);
            expect(spectator.service.isFavorite('content-type-3')).toBe(true);
        });

        it('should replace favorites using set() then verify with getAll()', () => {
            const newFavorites = [MOCK_CONTENT_TYPE_2, MOCK_CONTENT_TYPE_3];

            spectator.service.set(newFavorites);

            localStorageService.getItem.mockReturnValue(newFavorites);
            const result = spectator.service.getAll();

            expect(result).toEqual(newFavorites);
            expect(result).toHaveLength(2);
        });
    });

    describe('Edge Cases', () => {
        it('should handle content type with special characters in id', () => {
            const specialContentType = {
                ...MOCK_CONTENT_TYPE_1,
                id: 'content-type-!@#$%^&*()'
            };
            localStorageService.getItem.mockReturnValue([]);

            spectator.service.add(specialContentType);

            expect(localStorageService.setItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY, [
                specialContentType
            ]);
        });

        it('should handle content type with empty id', () => {
            const emptyIdContentType = {
                ...MOCK_CONTENT_TYPE_1,
                id: ''
            };
            localStorageService.getItem.mockReturnValue([]);

            spectator.service.add(emptyIdContentType);

            expect(localStorageService.setItem).toHaveBeenCalledWith(FAVORITE_CONTENT_TYPES_KEY, [
                emptyIdContentType
            ]);
        });

        it('should handle very large arrays', () => {
            const largeArray = Array.from({ length: 100 }, (_, i) => ({
                ...MOCK_CONTENT_TYPE_1,
                id: `content-type-${i}`
            }));

            localStorageService.getItem.mockReturnValue([]);

            spectator.service.set(largeArray);

            expect(localStorageService.setItem).toHaveBeenCalledWith(
                FAVORITE_CONTENT_TYPES_KEY,
                largeArray
            );
        });

        it('should handle adding the same content type multiple times in single call', () => {
            localStorageService.getItem.mockReturnValue([]);

            const result = spectator.service.add([
                MOCK_CONTENT_TYPE_1,
                MOCK_CONTENT_TYPE_1,
                MOCK_CONTENT_TYPE_1
            ]);

            expect(result).toHaveLength(1);
            expect(result).toEqual([MOCK_CONTENT_TYPE_1]);
        });
    });

    describe('localStorage Key Consistency', () => {
        it('should use consistent key across all methods', () => {
            localStorageService.getItem.mockReturnValue([]);

            spectator.service.add(MOCK_CONTENT_TYPE_1);
            spectator.service.getAll();
            spectator.service.isFavorite('content-type-1');
            spectator.service.remove('content-type-1');
            spectator.service.set([MOCK_CONTENT_TYPE_2]);
            spectator.service.clear();

            const getAllCalls = localStorageService.getItem.mock.calls;
            const setItemCalls = localStorageService.setItem.mock.calls;
            const removeItemCalls = localStorageService.removeItem.mock.calls;

            getAllCalls.forEach((call) => {
                expect(call[0]).toBe(FAVORITE_CONTENT_TYPES_KEY);
            });

            setItemCalls.forEach((call) => {
                expect(call[0]).toBe(FAVORITE_CONTENT_TYPES_KEY);
            });

            removeItemCalls.forEach((call) => {
                expect(call[0]).toBe(FAVORITE_CONTENT_TYPES_KEY);
            });
        });
    });
});
