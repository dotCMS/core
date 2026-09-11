import { Spectator, byTestId, createComponentFactory, mockProvider } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotContentDriveService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService
} from '@dotcms/data-access';
import { LoggerService, SiteService, StringUtils } from '@dotcms/dotcms-js';
import { DotContentDriveSearchResponse } from '@dotcms/dotcms-models';
import {
    DotFilterBarComponent,
    DotFolderListViewComponent,
    DotLanguageFilterChipComponent
} from '@dotcms/ui';
import { createFakeContentlet, MockDotMessageService } from '@dotcms/utils-testing';

import { AddRelationshipsComponent } from './add-relationships.component';
import { AddRelationshipsSiteChipComponent } from './components/site-chip/add-relationships-site-chip.component';
import { AddRelationshipsInput } from './models/add-relationships.models';

/**
 * US1 — the dialog presents the shared search-and-filter surface.
 *
 * Scope note: these cases are about the *shell* — which pieces are on screen and which are
 * deliberately not. Selection behaviour is US2's, and the search request shape is US3's.
 */
describe('AddRelationshipsComponent — shared surface (US1)', () => {
    let spectator: Spectator<AddRelationshipsComponent>;

    const messageServiceMock = new MockDotMessageService({
        'dot.relationship.add.dialog.title': 'Add Relationships',
        'dot.relationship.add.dialog.search.placeholder': 'Search by name...',
        'dot.relationship.add.dialog.confirm': 'Add Relationships',
        'dot.common.dialog.reject': 'Cancel',
        'dot.relationship.add.dialog.empty': 'No content found'
    });

    /**
     * Mutable for the same reason `searchMock` is: the TestBed is already instantiated by the time
     * a test body runs, so re-providing `DynamicDialogConfig` per test throws.
     */
    let input: AddRelationshipsInput;

    /**
     * `DotContentDriveService.search` already unwraps `entity` and returns the response itself,
     * whose page of rows is `list` — not `contentlets`. Getting this wrong makes every assertion
     * below pass or fail for reasons unrelated to what it is testing.
     */
    const emptyResponse: DotContentDriveSearchResponse = {
        list: [],
        contentCount: 0,
        folderCount: 0,
        hasMoreContent: false,
        hasMoreFolders: false,
        nextContentCursor: 0,
        nextFolderCursor: 0
    };

    /**
     * One mutable mock rather than a per-test provider override: the TestBed is already
     * instantiated by the time a test body runs, so `createComponent({ providers })` throws
     * "Cannot override provider when the test module has already been instantiated".
     */
    const searchMock = jest.fn();

    const createComponent = createComponentFactory({
        component: AddRelationshipsComponent,
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            {
                provide: DynamicDialogConfig,
                useValue: {
                    get data() {
                        return input;
                    }
                }
            },
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            mockProvider(DotContentDriveService, { search: searchMock }),
            // The store enriches rows with their language; without this the load never resolves
            // and every assertion below sees an empty table.
            mockProvider(DotLanguagesService, { get: jest.fn().mockReturnValue(of([])) }),
            // Without a current site there is no browsable scope, and the store correctly declines
            // to search at all — every assertion here would then see an empty table.
            mockProvider(SiteService, { currentSite: { hostname: 'demo.dotcms.com' } }),
            // `dot-folder-list-view` renders dates, which pulls in
            // DotFormatDateService -> DotcmsConfigService -> LoggerService. Provided here rather
            // than stubbing the list, because "the shared list view is what renders" is the
            // assertion — a stub would make it vacuous.
            provideHttpClient(),
            provideHttpClientTesting(),
            LoggerService,
            StringUtils,
            // The site chip composes the shared site/folder browser, whose store reports failures
            // through this. Provided rather than stubbed away, because "the chip is the shared
            // browser" is one of the things this spec asserts.
            mockProvider(DotHttpErrorManagerService)
        ],
        detectChanges: false
    });

    /** Re-mounts the dialog against a given search response. */
    const mountWith = (response: DotContentDriveSearchResponse) => {
        searchMock.mockReturnValue(of(response));
        spectator = createComponent();
        // Twice: the first pass runs `ngOnInit`, which is what issues the search. The rows only
        // exist for the template on the pass after that.
        spectator.detectChanges();
        spectator.detectChanges();

        return spectator;
    };

    const withResults: DotContentDriveSearchResponse = {
        ...emptyResponse,
        list: [createFakeContentlet({ title: 'Content 1', inode: '1', identifier: 'id-1' })],
        contentCount: 1
    };

    beforeEach(() => {
        input = { contentTypeId: 'target-type-id', selected: [], selectionMode: 'multiple' };
        searchMock.mockReset().mockReturnValue(of(emptyResponse));
        spectator = createComponent();
        spectator.detectChanges();
    });

    describe('the shared surface is what renders', () => {
        it('renders the shared filter bar rather than a relationship-specific filter popover', () => {
            expect(spectator.query(DotFilterBarComponent)).toBeTruthy();
        });

        it('renders a full-width search input', () => {
            expect(spectator.query(byTestId('add-relationships-search'))).toBeTruthy();
        });

        it('renders the shared list view for results, rather than a table of its own', () => {
            mountWith(withResults);

            expect(spectator.query(DotFolderListViewComponent)).toBeTruthy();
        });

        /**
         * The design has no folder tree: content of an arbitrary content type is not folder-scoped,
         * so the folder scope is a chip. A sidebar here means the AssetPicker shell was reused
         * wholesale instead of the pieces.
         */
        it('renders NO folder-tree sidebar', () => {
            expect(spectator.query(byTestId('add-relationships-sidebar'))).toBeNull();
            expect(spectator.query('p-splitter')).toBeNull();
        });
    });

    describe('dialog chrome', () => {
        it('is titled "Add Relationships"', () => {
            expect(spectator.query(byTestId('dialog-title'))?.textContent).toContain(
                'Add Relationships'
            );
        });

        /**
         * `dialog-close-btn` is the shared header's own testid, not one of ours. Asserting it is
         * what proves this dialog wears the shared chrome instead of hand-rolling a header.
         */
        it('offers a full-screen toggle and the shared close control in the header', () => {
            expect(spectator.query(byTestId('add-relationships-fullscreen'))).toBeTruthy();
            expect(spectator.query(byTestId('dialog-close-btn'))).toBeTruthy();
        });

        it('offers Cancel and Add Relationships in the footer', () => {
            expect(spectator.query(byTestId('add-relationships-cancel'))).toBeTruthy();
            expect(spectator.query(byTestId('add-relationships-confirm'))).toBeTruthy();
        });
    });

    describe('the chip row', () => {
        /**
         * Queried by **component class**, not by `data-testid`.
         *
         * A testid sits on the element whether or not Angular ever resolved it to a component, so
         * a testid-only assertion passes against a misspelled selector and the failure only shows
         * up as an NG8001 at build time. That is exactly what happened here once.
         */
        it('offers the site/folder and Locale chips', () => {
            expect(spectator.query(AddRelationshipsSiteChipComponent)).toBeTruthy();
            expect(spectator.query(DotLanguageFilterChipComponent)).toBeTruthy();
        });

        /**
         * FR-004. The target content type is a caller restriction, not a filter — offering the
         * chip would let the editor widen the result set past the relationship's own type, which
         * is the one thing this dialog must never allow.
         */
        it('does NOT offer the content-type chip', () => {
            expect(spectator.query(byTestId('add-relationships-content-type-chip'))).toBeNull();
        });
    });

    describe('empty states', () => {
        /**
         * The **table's** empty state, not one of the dialog's own: replacing the table with a
         * message took the header row and the paging footer with it, and with them the controls
         * that get the editor out of an empty result.
         */
        it('shows the table empty state when a search matches nothing', () => {
            expect(spectator.query(byTestId('empty-state'))).toBeTruthy();
            expect(spectator.query(byTestId('header-row'))).toBeTruthy();
        });

        it('shows an empty state, not an error, when the target content type has no content', () => {
            expect(spectator.query(byTestId('empty-state'))).toBeTruthy();
            expect(spectator.query(byTestId('add-relationships-error'))).toBeNull();
        });

        it('shows results rather than the empty state when the search returns content', () => {
            mountWith(withResults);

            expect(spectator.query(byTestId('empty-state'))).toBeNull();
        });
    });

    /**
     * US2 — what the dialog closes with.
     *
     * The two outcomes are not interchangeable: `[]` means the editor confirmed an empty selection
     * and the relationship is emptied; `undefined` means they cancelled and it is left alone. Read
     * one as the other and either cancel wipes the field or confirming-empty does nothing.
     */
    describe('closing (US2)', () => {
        it('cancels with undefined, never with an empty array', () => {
            const ref = spectator.inject(DynamicDialogRef);

            spectator.click(
                spectator
                    .query(byTestId('dialog-close-btn'))
                    ?.querySelector('button') as HTMLElement
            );

            expect(ref.close).toHaveBeenCalledWith(undefined);
        });

        it('confirms with the whole selection, including items not on the current page', () => {
            const related = createFakeContentlet({
                inode: 'inode-9',
                identifier: 'id-9',
                title: 'Related elsewhere'
            });
            input = { ...input, selected: [related] };
            spectator = createComponent();
            spectator.detectChanges();

            const ref = spectator.inject(DynamicDialogRef);
            const confirm = spectator
                .query(byTestId('add-relationships-confirm'))
                ?.querySelector('button');
            spectator.click(confirm as HTMLElement);

            expect(ref.close).toHaveBeenCalledWith([related]);
        });

        it('confirms with an empty array when the editor unchecked everything', () => {
            const ref = spectator.inject(DynamicDialogRef);
            const confirm = spectator
                .query(byTestId('add-relationships-confirm'))
                ?.querySelector('button');
            spectator.click(confirm as HTMLElement);

            expect(ref.close).toHaveBeenCalledWith([]);
        });
    });

    /**
     * Manual-QA findings, 2026-09-11. Each of these is a defect that a green unit suite let through,
     * so each is asserted on **the thing the browser actually reads** rather than on internal state.
     */
    describe('QA findings', () => {
        /** T099 — the toggle must use the icon set every other dotCMS dialog uses. */
        it('renders the full-screen toggle as a Material Symbol, not a PrimeIcon', () => {
            const toggle = spectator.query(byTestId('add-relationships-fullscreen'));

            expect(toggle?.querySelector('.material-symbols-outlined')?.textContent?.trim()).toBe(
                'open_in_full'
            );
            expect(toggle?.querySelector('[class*="pi-window"]')).toBeNull();
        });

        /**
         * T103 — the store knowing what is selected is not enough.
         *
         * `dot-folder-list-view` is **controlled**: it renders the `selection` input and only
         * reports changes. The dialog seeded its store correctly and never passed it, so
         * already-related rows opened unchecked — FR-009 implemented everywhere except where the
         * editor could see it. Asserted on the list's input for that reason.
         */
        it('hands the already-related rows to the list as its selection', () => {
            const related = createFakeContentlet({
                inode: 'inode-9',
                identifier: 'id-9',
                title: 'Already related'
            });
            input = { ...input, selected: [related] };
            mountWith(withResults);

            const list = spectator.query(DotFolderListViewComponent);

            expect(list?.$selection()).toEqual([related]);
        });

        it('passes an empty selection when the field relates nothing', () => {
            mountWith(withResults);

            expect(spectator.query(DotFolderListViewComponent)?.$selection()).toEqual([]);
        });
    });
});
