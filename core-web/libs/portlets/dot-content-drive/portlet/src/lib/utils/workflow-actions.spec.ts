import { DotContentDriveItem } from '@dotcms/dotcms-models';

import { getActionConditions } from './workflow-actions';

describe('workflow-actions', () => {
    describe('getActionConditions', () => {
        describe('empty selection', () => {
            it('should return all false conditions when no items are selected', () => {
                const result = getActionConditions([]);

                expect(result).toEqual({
                    hasSelection: false,
                    isSingleSelection: false,
                    allArchived: false,
                    allLive: false,
                    allWorking: false,
                    allLocked: false,
                    noneArchived: false,
                    noneLive: false,
                    noneWorking: false,
                    noneLocked: false,
                    allAreAssets: false,
                    isPage: false,
                    isContentlet: false
                });
            });
        });

        describe('single selection', () => {
            it('should identify a single archived contentlet', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: true,
                        live: false,
                        working: false,
                        locked: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result).toEqual({
                    hasSelection: true,
                    isSingleSelection: true,
                    allArchived: true,
                    allLive: false,
                    allWorking: false,
                    allLocked: false,
                    noneArchived: false,
                    noneLive: true,
                    noneWorking: true,
                    noneLocked: true,
                    allAreAssets: false,
                    isPage: false,
                    isContentlet: true
                });
            });

            it('should identify a single live and working page', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: true,
                        locked: false,
                        baseType: 'HTMLPAGE'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result).toEqual({
                    hasSelection: true,
                    isSingleSelection: true,
                    allArchived: false,
                    allLive: true,
                    allWorking: true,
                    allLocked: false,
                    noneArchived: true,
                    noneLive: false,
                    noneWorking: false,
                    noneLocked: true,
                    allAreAssets: false,
                    isPage: true,
                    isContentlet: false
                });
            });

            it('should identify a single FILEASSET', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        locked: false,
                        baseType: 'FILEASSET'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result).toEqual({
                    hasSelection: true,
                    isSingleSelection: true,
                    allArchived: false,
                    allLive: true,
                    allWorking: false,
                    allLocked: false,
                    noneArchived: true,
                    noneLive: false,
                    noneWorking: true,
                    noneLocked: true,
                    allAreAssets: true,
                    isPage: false,
                    isContentlet: false
                });
            });

            it('should identify a single DOTASSET', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: false,
                        working: true,
                        locked: false,
                        baseType: 'DOTASSET'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result).toEqual({
                    hasSelection: true,
                    isSingleSelection: true,
                    allArchived: false,
                    allLive: false,
                    allWorking: true,
                    allLocked: false,
                    noneArchived: true,
                    noneLive: true,
                    noneWorking: false,
                    noneLocked: true,
                    allAreAssets: true,
                    isPage: false,
                    isContentlet: false
                });
            });
        });

        describe('multiple selection - all same type', () => {
            it('should identify all archived contentlets', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: true,
                        live: false,
                        working: false,
                        locked: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: true,
                        live: false,
                        working: false,
                        locked: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result).toEqual({
                    hasSelection: true,
                    isSingleSelection: false,
                    allArchived: true,
                    allLive: false,
                    allWorking: false,
                    allLocked: false,
                    noneArchived: false,
                    noneLive: true,
                    noneWorking: true,
                    noneLocked: true,
                    allAreAssets: false,
                    isPage: false,
                    isContentlet: true
                });
            });

            it('should identify all live pages', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        locked: false,
                        baseType: 'HTMLPAGE'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: true,
                        working: true,
                        locked: false,
                        baseType: 'HTMLPAGE'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: true,
                        working: false,
                        locked: false,
                        baseType: 'HTMLPAGE'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result).toEqual({
                    hasSelection: true,
                    isSingleSelection: false,
                    allArchived: false,
                    allLive: true,
                    allWorking: false,
                    allLocked: false,
                    noneArchived: true,
                    noneLive: false,
                    noneWorking: false,
                    noneLocked: true,
                    allAreAssets: false,
                    isPage: true,
                    isContentlet: false
                });
            });

            it('should identify all working contentlets', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: false,
                        working: true,
                        locked: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        locked: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result).toEqual({
                    hasSelection: true,
                    isSingleSelection: false,
                    allArchived: false,
                    allLive: false,
                    allWorking: true,
                    allLocked: false,
                    noneArchived: true,
                    noneLive: true,
                    noneWorking: false,
                    noneLocked: true,
                    allAreAssets: false,
                    isPage: false,
                    isContentlet: true
                });
            });

            it('should identify all assets (FILEASSET)', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'FILEASSET'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'FILEASSET'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allAreAssets).toBe(true);
                expect(result.hasSelection).toBe(true);
                expect(result.isSingleSelection).toBe(false);
            });

            it('should identify all assets (DOTASSET)', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'DOTASSET'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'DOTASSET'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allAreAssets).toBe(true);
                expect(result.hasSelection).toBe(true);
                expect(result.isSingleSelection).toBe(false);
            });

            it('should identify mixed asset types as all assets', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'FILEASSET'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'DOTASSET'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allAreAssets).toBe(true);
                expect(result.hasSelection).toBe(true);
                expect(result.isSingleSelection).toBe(false);
            });
        });

        describe('multiple selection - mixed types', () => {
            it('should identify mixed content types (pages and contentlets)', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'HTMLPAGE'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.isPage).toBe(false);
                expect(result.isContentlet).toBe(false);
                expect(result.allAreAssets).toBe(false);
                expect(result.hasSelection).toBe(true);
                expect(result.isSingleSelection).toBe(false);
            });

            it('should handle mixed archived and non-archived items', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: true,
                        live: false,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allArchived).toBe(false);
                expect(result.noneArchived).toBe(false);
                expect(result.hasSelection).toBe(true);
            });

            it('should handle mixed live and non-live items', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allLive).toBe(false);
                expect(result.noneLive).toBe(false);
                expect(result.hasSelection).toBe(true);
            });

            it('should handle mixed working and non-working items', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allWorking).toBe(false);
                expect(result.noneWorking).toBe(false);
                expect(result.hasSelection).toBe(true);
            });
        });

        describe('edge cases and complex scenarios', () => {
            it('should handle items with all properties set to false', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: false,
                        working: false,
                        locked: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result).toEqual({
                    hasSelection: true,
                    isSingleSelection: true,
                    allArchived: false,
                    allLive: false,
                    allWorking: false,
                    allLocked: false,
                    noneArchived: true,
                    noneLive: true,
                    noneWorking: true,
                    noneLocked: true,
                    allAreAssets: false,
                    isPage: false,
                    isContentlet: true
                });
            });

            it('should handle items with all properties set to true', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: true,
                        live: true,
                        working: true,
                        locked: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result).toEqual({
                    hasSelection: true,
                    isSingleSelection: true,
                    allArchived: true,
                    allLive: true,
                    allWorking: true,
                    allLocked: true,
                    noneArchived: false,
                    noneLive: false,
                    noneWorking: false,
                    noneLocked: false,
                    allAreAssets: false,
                    isPage: false,
                    isContentlet: true
                });
            });

            it('should handle a large selection of the same type', () => {
                const items: DotContentDriveItem[] = Array.from({ length: 100 }, () => ({
                    archived: false,
                    live: true,
                    working: false,
                    baseType: 'CONTENT'
                })) as DotContentDriveItem[];

                const result = getActionConditions(items);

                expect(result.hasSelection).toBe(true);
                expect(result.isSingleSelection).toBe(false);
                expect(result.allLive).toBe(true);
                expect(result.noneArchived).toBe(true);
                expect(result.isContentlet).toBe(true);
            });

            it('should handle mixed selection with assets and content', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'FILEASSET'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allAreAssets).toBe(false);
                expect(result.isContentlet).toBe(false);
                expect(result.isPage).toBe(false);
            });

            it('should correctly count items with only some archived', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: true,
                        live: false,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: true,
                        live: false,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allArchived).toBe(false);
                expect(result.noneArchived).toBe(false);
            });

            it('should identify noneArchived when all items are not archived', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: true,
                        working: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.noneArchived).toBe(true);
                expect(result.allArchived).toBe(false);
            });

            it('should identify noneLive when all items are not live', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.noneLive).toBe(true);
                expect(result.allLive).toBe(false);
            });

            it('should identify noneWorking when all items are not working', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: true,
                        live: false,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.noneWorking).toBe(true);
                expect(result.allWorking).toBe(false);
            });

            it('should handle unknown base types', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'UNKNOWN_TYPE'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.isContentlet).toBe(false);
                expect(result.isPage).toBe(false);
                expect(result.allAreAssets).toBe(false);
                expect(result.hasSelection).toBe(true);
            });
        });

        describe('locked state tracking', () => {
            it('should identify all locked items', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        locked: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        locked: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allLocked).toBe(true);
                expect(result.noneLocked).toBe(false);
            });

            it('should identify none locked items', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        locked: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        locked: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allLocked).toBe(false);
                expect(result.noneLocked).toBe(true);
            });

            it('should handle mixed locked and unlocked items', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        locked: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        locked: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allLocked).toBe(false);
                expect(result.noneLocked).toBe(false);
            });

            it('should identify a single locked item', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        locked: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allLocked).toBe(true);
                expect(result.noneLocked).toBe(false);
                expect(result.isSingleSelection).toBe(true);
            });
        });

        describe('real-world scenarios', () => {
            it('should support "Edit Content" action visibility (single non-archived contentlet)', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.isSingleSelection).toBe(true);
                expect(result.noneArchived).toBe(true);
                expect(result.isContentlet).toBe(true);
            });

            it('should support "Edit Page" action visibility (single non-archived page)', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'HTMLPAGE'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.isSingleSelection).toBe(true);
                expect(result.noneArchived).toBe(true);
                expect(result.isPage).toBe(true);
            });

            it('should support "Publish" action visibility (non-archived and non-live items)', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.noneArchived).toBe(true);
                expect(result.noneLive).toBe(true);
            });

            it('should support "Unpublish" action visibility (non-archived and all live items)', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: true,
                        working: true,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.noneArchived).toBe(true);
                expect(result.allLive).toBe(true);
            });

            it('should support "Archive" action visibility (none archived items)', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.noneArchived).toBe(true);
            });

            it('should support "Unarchive" action visibility (all archived items)', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: true,
                        live: false,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem,
                    {
                        archived: true,
                        live: false,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allArchived).toBe(true);
            });

            it('should support "Download" action visibility (all assets)', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'FILEASSET'
                    } as DotContentDriveItem,
                    {
                        archived: false,
                        live: false,
                        working: true,
                        baseType: 'DOTASSET'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.allAreAssets).toBe(true);
            });

            it('should support "Rename" action visibility (single non-archived item)', () => {
                const items: DotContentDriveItem[] = [
                    {
                        archived: false,
                        live: true,
                        working: false,
                        baseType: 'CONTENT'
                    } as DotContentDriveItem
                ];

                const result = getActionConditions(items);

                expect(result.isSingleSelection).toBe(true);
                expect(result.noneArchived).toBe(true);
            });
        });
    });
});
