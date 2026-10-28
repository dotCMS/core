import { describe, expect, it, vi } from 'vitest';

import {
    browserStorage,
    parseViewType,
    readStoredViewType,
    resolveViewType,
    viewTypeStorageKey,
    writeStoredViewType
} from './view-type';

describe('view type (#37930, US4)', () => {
    describe('parseViewType', () => {
        it.each([
            ['list', 'list'],
            ['card', 'card'],
            ['Card', 'card'],
            [' LIST ', 'list']
        ])('should read %j as %j, whatever the letter case', (value, expected) => {
            expect(parseViewType(value)).toBe(expected);
        });

        it.each([undefined, null, '', 'grid', 'table', 42, ['card']])(
            'should not accept %j',
            (value) => {
                expect(parseViewType(value)).toBeNull();
            }
        );
    });

    describe('resolveViewType (FR-002a)', () => {
        it('should let the URL win over everything (US4 #3)', () => {
            expect(
                resolveViewType({
                    urlViewType: 'list',
                    storedViewType: 'card',
                    defaultViewType: 'card'
                })
            ).toBe('list');
        });

        it('should let the remembered view win over the tool default (US4 #1, #5)', () => {
            expect(
                resolveViewType({
                    urlViewType: undefined,
                    storedViewType: 'list',
                    defaultViewType: 'card'
                })
            ).toBe('list');
        });

        it('should use the tool default when nothing is remembered (US4 #5)', () => {
            expect(
                resolveViewType({
                    urlViewType: undefined,
                    storedViewType: null,
                    defaultViewType: 'CARD'
                })
            ).toBe('card');
        });

        it('should open the table when nothing says otherwise (US4 #6)', () => {
            expect(
                resolveViewType({
                    urlViewType: undefined,
                    storedViewType: null,
                    defaultViewType: undefined
                })
            ).toBe('list');
        });

        it('should skip a value it cannot read and ask the next one (US4 #8)', () => {
            expect(
                resolveViewType({
                    urlViewType: 'mosaic',
                    storedViewType: 'garbage',
                    defaultViewType: 'card'
                })
            ).toBe('card');
        });
    });

    describe('viewTypeStorageKey', () => {
        it('should keep one remembered view per screen (US4 #4)', () => {
            expect(viewTypeStorageKey('content-drive')).toBe(
                'dotcms.content-drive.view-type.content-drive'
            );
            expect(viewTypeStorageKey('c_blogs')).toBe('dotcms.content-drive.view-type.c_blogs');
        });

        it('should fall back to the Drive when the screen is unknown', () => {
            expect(viewTypeStorageKey('')).toBe('dotcms.content-drive.view-type.content-drive');
        });
    });

    describe('storage (FR-024)', () => {
        const key = viewTypeStorageKey('content-drive');

        it('should read a remembered view', () => {
            const storage = { getItem: vi.fn().mockReturnValue('card') };

            expect(readStoredViewType(storage, key)).toBe('card');
            expect(storage.getItem).toHaveBeenCalledWith(key);
        });

        it('should treat storage that throws as empty', () => {
            const storage = {
                getItem: vi.fn(() => {
                    throw new Error('SecurityError');
                })
            };

            expect(readStoredViewType(storage, key)).toBeNull();
        });

        it('should treat missing storage as empty', () => {
            expect(readStoredViewType(null, key)).toBeNull();
        });

        it('should remember a view', () => {
            const storage = { setItem: vi.fn() };

            writeStoredViewType(storage, key, 'card');

            expect(storage.setItem).toHaveBeenCalledWith(key, 'card');
        });

        it('should give up quietly when storage refuses the write (US4 #7)', () => {
            const storage = {
                setItem: vi.fn(() => {
                    throw new Error('QuotaExceededError');
                })
            };

            expect(() => writeStoredViewType(storage, key, 'card')).not.toThrow();
        });

        it('should hand back no storage when the browser blocks reaching it', () => {
            const spy = vi.spyOn(globalThis, 'localStorage', 'get').mockImplementation(() => {
                throw new Error('SecurityError');
            });

            expect(browserStorage()).toBeNull();

            spy.mockRestore();
        });
    });
});
