import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';

import { DotFormatDateService, DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';
import { DotcmsConfigServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveActionPreviewComponent } from './dot-content-drive-action-preview.component';

const contentlet = (overrides: Partial<DotCMSContentlet> & { inode: string }): DotCMSContentlet =>
    ({
        identifier: `id-${overrides.inode}`,
        title: `Title ${overrides.inode}`,
        contentType: 'Blog',
        baseType: 'CONTENT',
        live: false,
        working: true,
        archived: false,
        locked: false,
        hasTitleImage: false,
        languageId: 1,
        ...overrides
    }) as DotCMSContentlet;

/** Builds `count` contentlets, used to cross the pagination threshold. */
const contentlets = (count: number): DotCMSContentlet[] =>
    Array.from({ length: count }, (_, index) => contentlet({ inode: `inode-${index}` }));

/**
 * Rows the grid fits on a page. Mirrors `DotFolderListViewComponent.MIN_ROWS_PER_PAGE`, which owns
 * the page size now that the preview delegates to it.
 */
const ROWS_PER_PAGE = 20;

describe('DotContentDriveActionPreviewComponent', () => {
    let spectator: Spectator<DotContentDriveActionPreviewComponent>;

    const createComponent = createComponentFactory({
        component: DotContentDriveActionPreviewComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.list-view.locked-by-another-user': 'Locked by another user'
                })
            },
            // Required by the grid this component renders for real.
            mockProvider(DotLanguagesService, { get: jest.fn(() => of([])) }),
            mockProvider(DotcmsConfigService, new DotcmsConfigServiceMock()),
            mockProvider(DotFormatDateService),
            provideHttpClient()
        ],
        detectChanges: false
    });

    const ITEMS = [
        contentlet({ inode: 'inode-1', title: 'Destination Guide', contentType: 'Web Page' }),
        contentlet({ inode: 'inode-2', title: 'Costa Rica', live: true, hasLiveVersion: true })
    ];

    /** The row checkbox input, which is what a user actually clicks. */
    const rowCheckbox = (index: number): HTMLInputElement =>
        // Every rendered row has a checkbox; a missing one is a failure, not a case to handle.
        spectator.queryAll(byTestId('item-row'))[index].querySelector('input')!;

    beforeEach(() => {
        spectator = createComponent({
            props: { items: ITEMS, selection: ITEMS, disabled: false }
        });
        spectator.detectChanges();
    });

    /**
     * The preview is a configuration of the Content Drive grid, not a table of its own. Two
     * near-identical tables drifted apart every time one was touched; the settings below are the
     * whole difference between browsing content and confirming an action on it.
     */
    describe('delegation to the grid', () => {
        it('should render the Content Drive grid', () => {
            expect(spectator.query(DotFolderListViewComponent)).toBeTruthy();
        });

        it('should key rows on inode so language variants stay distinct', () => {
            // Two language versions of one contentlet share an identifier — the grid's default —
            // and inodes are what gets fired.
            expect(spectator.query(DotFolderListViewComponent)!.$dataKey()).toBe('inode');
        });

        it('should page in memory, since the whole selection is already here', () => {
            expect(spectator.query(DotFolderListViewComponent)!.$lazy()).toBe(false);
        });

        it('should strip the grid affordances that make no sense in a dialog', () => {
            expect(spectator.query(DotFolderListViewComponent)!.$readOnly()).toBe(true);
        });

        it('should forward the caller-provided selection', () => {
            expect(spectator.query(DotFolderListViewComponent)!.$selection()).toEqual(ITEMS);
        });
    });

    describe('rendering', () => {
        it('should render a row per item', () => {
            expect(spectator.queryAll(byTestId('item-row')).length).toBe(2);
        });

        it('should show only what is needed to recognise a row', () => {
            // The dialog is far narrower than the portlet. The grid's full column set overflows it,
            // squeezing the title to an ellipsis and pushing the rest behind the dialog edge — so
            // the preview keeps the checkbox, the title with its thumbnail, the status and the
            // content type, and drops locale, edited-by, last-edited and the actions column.
            const [firstRow] = spectator.queryAll(byTestId('item-row'));

            expect(
                firstRow.querySelector('[data-testid="item-title-text"]')!.textContent
            ).toContain('Destination Guide');
            expect(firstRow.querySelector('[data-testid="contentlet-thumbnail"]')).toBeTruthy();
            expect(firstRow.querySelector('[data-testid="item-status"]')).toBeTruthy();
            expect(
                firstRow.querySelector('[data-testid="item-content-type"]')!.textContent
            ).toContain('Web Page');

            expect(firstRow.querySelector('[data-testid="item-language"]')).toBeFalsy();
            expect(firstRow.querySelector('[data-testid="item-mod-user-name"]')).toBeFalsy();
            expect(firstRow.querySelector('[data-testid="item-mod-date"]')).toBeFalsy();
            expect(firstRow.querySelector('[data-testid="item-actions"]')).toBeFalsy();
        });

        it('should not offer column sorting', () => {
            // Re-ordering a list the user is checking off only loses their place.
            expect(spectator.queryAll(byTestId('sort-icon')).length).toBe(0);
        });

        it('should render nothing but the header for an empty list', () => {
            spectator.setInput('items', []);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('item-row')).length).toBe(0);
            expect(spectator.query(byTestId('header-row'))).toBeTruthy();
        });
    });

    describe('selection', () => {
        it('should emit the remaining items when a row is unchecked', () => {
            const emitted = jest.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.click(rowCheckbox(0));
            spectator.detectChanges();

            expect(emitted).toHaveBeenCalledWith([ITEMS[1]]);
        });

        it('should emit the added item when an unchecked row is checked again', () => {
            spectator.setInput('selection', [ITEMS[1]]);
            spectator.detectChanges();

            const emitted = jest.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.click(rowCheckbox(0));
            spectator.detectChanges();

            expect(emitted).toHaveBeenCalledWith([ITEMS[1], ITEMS[0]]);
        });

        it('should emit an empty list when the header checkbox clears everything', () => {
            const emitted = jest.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.click(spectator.query(byTestId('header-checkbox'))!.querySelector('input')!);
            spectator.detectChanges();

            expect(emitted).toHaveBeenCalledWith([]);
        });

        it('should keep same-identifier language variants as distinct rows', () => {
            const variants = [
                contentlet({ inode: 'inode-en', identifier: 'shared', languageId: 1 }),
                contentlet({ inode: 'inode-es', identifier: 'shared', languageId: 2 })
            ];

            spectator.setInput('items', variants);
            spectator.setInput('selection', variants);
            spectator.detectChanges();

            const emitted = jest.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.click(rowCheckbox(0));
            spectator.detectChanges();

            expect(emitted).toHaveBeenCalledWith([variants[1]]);
        });
    });

    /**
     * The capability the preview exists to provide: a bulk Unlock may be refused on a lock the user
     * does not hold, and until the rows say which, the warning on the action row is unactionable.
     */
    describe('locks held by another user', () => {
        const lockedItems = [
            contentlet({ inode: 'mine', locked: true, contentEditable: true }),
            contentlet({ inode: 'theirs', locked: true, contentEditable: false })
        ];

        beforeEach(() => {
            spectator.setInput('items', lockedItems);
            spectator.setInput('selection', lockedItems);
        });

        it('should mark the rows the caller flagged', () => {
            spectator.setInput('lockedByOthers', ['theirs']);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('lock-foreign-icon')).length).toBe(1);
            expect(spectator.queryAll(byTestId('lock-icon')).length).toBe(1);
        });

        it('should mark nothing when the caller flags nothing', () => {
            // An administrator releases every lock, so their caller flags none of them.
            spectator.setInput('lockedByOthers', []);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('lock-foreign-icon')).length).toBe(0);
            expect(spectator.queryAll(byTestId('lock-icon')).length).toBe(2);
        });

        it('should keep a marked row checked, and let the user drop it', () => {
            // Marked but not pre-unchecked: the fired count keeps matching what the action row
            // advertised, and dropping the row stays the user's decision.
            spectator.setInput('lockedByOthers', ['theirs']);
            spectator.detectChanges();

            expect(spectator.query(DotFolderListViewComponent)!.$selection()).toEqual(lockedItems);

            const emitted = jest.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.click(rowCheckbox(1));
            spectator.detectChanges();

            expect(emitted).toHaveBeenCalledWith([lockedItems[0]]);
        });
    });

    describe('disabled', () => {
        it('should freeze the checkboxes', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            expect(spectator.query(DotFolderListViewComponent)!.$disabled()).toBe(true);
            expect(rowCheckbox(0).disabled).toBe(true);
        });

        it('should not emit when a disabled row is clicked', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            const emitted = jest.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.click(rowCheckbox(0));
            spectator.detectChanges();

            expect(emitted).not.toHaveBeenCalled();
        });
    });

    describe('pagination', () => {
        it('should not paginate a selection that fits on one page', () => {
            spectator.setInput('items', contentlets(ROWS_PER_PAGE));
            spectator.detectChanges();

            expect(spectator.query('.p-paginator')).toBeNull();
        });

        it('should paginate once the selection outgrows a page', () => {
            spectator.setInput('items', contentlets(ROWS_PER_PAGE + 1));
            spectator.detectChanges();

            expect(spectator.query('.p-paginator')).toBeTruthy();
            expect(spectator.queryAll(byTestId('item-row')).length).toBe(ROWS_PER_PAGE);
        });
    });
});
