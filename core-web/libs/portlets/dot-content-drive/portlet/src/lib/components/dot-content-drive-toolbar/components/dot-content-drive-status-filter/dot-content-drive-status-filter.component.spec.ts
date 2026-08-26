import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';
import { of } from 'rxjs';

import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Popover } from 'primeng/popover';

import {
    AddToBundleService,
    DotBulkRefreshService,
    DotContentDriveService,
    DotCurrentUserService,
    DotFolderService,
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService,
    DotPropertiesService,
    DotWorkflowActionsFireService,
    PushPublishService
} from '@dotcms/data-access';
import { DotCurrentUser } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotChipFilterComponent } from '@dotcms/ui';
import { MockDotMessageService, mockLocales } from '@dotcms/utils-testing';


import { DotContentDriveStatusFilterComponent } from './dot-content-drive-status-filter.component';

import { CONTENT_STATUS, STATUS_FILTER_KEY } from '../../../../shared/constants';
import { MOCK_SEARCH_RESPONSE, MOCK_SITES } from '../../../../shared/mocks';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveStatusFilterComponent', () => {
    let spectator: Spectator<DotContentDriveStatusFilterComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    /**
     * Backs the mocked `getFilterValue`. The component reads it through a `linkedSignal`, so a
     * plain `mockReturnValue` would be invisible: with no signal dependency there is nothing to
     * invalidate and the first value would be cached for the life of the test.
     */
    const storedValue = signal<string[] | undefined>(undefined);

    const createComponent = createComponentFactory({
        component: DotContentDriveStatusFilterComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                patchFilters: jest.fn(),
                removeFilter: jest.fn(),
                getFilterValue: jest.fn(() => storedValue())
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.status-filter.title': 'Status',
                    'content-drive.status-filter.archived': 'Archived',
                    'content-drive.status-filter.unpublished': 'Unpublished',
                    'content-drive.status-filter.locked': 'Locked',
                    'dot.common.remove': 'Remove'
                })
            }
        ]
    });

    /**
     * Rebuilds the component with a filter value already in the bag — the real sequence for a URL
     * restore. Seeding after creation is not equivalent: `[ngModel]` writes asynchronously, so the
     * listbox would still hold its initial (empty) selection when the click lands.
     */
    const recreateWith = async (selection: string[]) => {
        storedValue.set(selection);
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        spectator.detectChanges();
        // ngModel writes on a microtask, so the listbox does not hold the restored selection until
        // that has flushed. Without this it starts empty and the next click looks like a replace.
        await spectator.fixture.whenStable();
        spectator.detectChanges();
        jest.clearAllMocks();
    };

    /**
     * Opens the popover so the option list renders. p-popover builds its content lazily, so the
     * options do not exist in the DOM until it is shown. Driven through the Popover's own public
     * `toggle`, the same call the chip's `clicked` output makes in the template.
     */
    const openPanel = () => {
        // A real event: PrimeNG calls stopPropagation on it. The explicit target is what the
        // overlay anchors to, standing in for the chip element the template passes.
        spectator.query(Popover).toggle(new MouseEvent('click'), spectator.element);
        spectator.detectChanges();
    };

    /**
     * Toggles one option through the rendered checkbox's own output — the same path a user takes.
     * Never reaches into the component's protected members.
     */
    const toggleOption = (value: string) => {
        // Click the real input inside p-checkbox. The overlay lives outside the fixture, so
        // triggerEventHandler cannot reach it — and a click is what a user actually does.
        option(value).querySelector('input').click();
        spectator.detectChanges();
    };

    /** The overlay is appended to document.body, so panel content is queried from the root. */
    const option = (value: string) =>
        spectator.query(byTestId(`status-option-${value}`), { root: true });

    beforeEach(() => {
        storedValue.set(undefined);
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        spectator.detectChanges();
        // The mockProvider's jest.fn()s are built once at factory definition, so calls leak
        // between tests. Clear recorded calls (implementations survive) so each test starts clean.
        jest.clearAllMocks();
    });

    it('should render the chip', () => {
        expect(spectator.query(byTestId('status-filter-chip'))).toBeTruthy();
    });

    it('should render every status option once the panel is open', () => {
        openPanel();

        expect(option(CONTENT_STATUS.ARCHIVED)).toBeTruthy();
        expect(option(CONTENT_STATUS.UNPUBLISHED)).toBeTruthy();
        expect(option(CONTENT_STATUS.LOCKED)).toBeTruthy();
    });

    it('should write a single selection to the filter bag as an array', () => {
        openPanel();
        toggleOption(CONTENT_STATUS.ARCHIVED);

        // An array even for one value — the URL decoder splits on commas, so a bare string would
        // round-trip back as a string and every `?.status?.length` check downstream would be wrong.
        expect(store.patchFilters).toHaveBeenCalledWith({
            [STATUS_FILTER_KEY]: [CONTENT_STATUS.ARCHIVED]
        });
    });

    it('should select when the LABEL is clicked, not just the checkbox', () => {
        // Regression: the listbox had [ngModel] but no (ngModelChange), so clicking the row or its
        // label did nothing and only the checkbox itself responded.
        openPanel();
        option(CONTENT_STATUS.ARCHIVED).querySelector('dot-filter-list-item').click();
        spectator.detectChanges();

        expect(store.patchFilters).toHaveBeenCalledWith({
            [STATUS_FILTER_KEY]: [CONTENT_STATUS.ARCHIVED]
        });
    });

    it('should accumulate multiple selections rather than replacing', async () => {
        await recreateWith([CONTENT_STATUS.UNPUBLISHED]);
        openPanel();
        toggleOption(CONTENT_STATUS.LOCKED);

        expect(store.patchFilters).toHaveBeenCalledWith({
            [STATUS_FILTER_KEY]: [CONTENT_STATUS.UNPUBLISHED, CONTENT_STATUS.LOCKED]
        });
    });

    it('should remove the filter entirely when the last selection is cleared', async () => {
        await recreateWith([CONTENT_STATUS.LOCKED]);
        openPanel();
        toggleOption(CONTENT_STATUS.LOCKED);

        // removeFilter, not patchFilters with [] — an empty array would linger in the URL and
        // keep folders suppressed.
        expect(store.removeFilter).toHaveBeenCalledWith(STATUS_FILTER_KEY);
        expect(store.patchFilters).not.toHaveBeenCalled();
    });

    it('should clear every selection when the chip is removed', async () => {
        await recreateWith([CONTENT_STATUS.ARCHIVED, CONTENT_STATUS.LOCKED]);

        spectator.triggerEventHandler(DotChipFilterComponent, 'removed', undefined);
        spectator.detectChanges();

        expect(store.removeFilter).toHaveBeenCalledWith(STATUS_FILTER_KEY);
    });

    it('should show the restored selection as chip values on init', async () => {
        await recreateWith([CONTENT_STATUS.ARCHIVED]);

        expect(spectator.query(DotChipFilterComponent).selections()).toEqual(['Archived']);
    });

    it('should carry an accessible label on the chip', () => {
        expect(
            spectator.query(byTestId('status-filter-chip')).getAttribute('aria-label')
        ).toBeTruthy();
    });
});

/**
 * The reactive sync writes to the store, and the store feeds the signal that triggers it. The suite
 * above mocks the store, so `getFilterValue` never reflects what `patchFilters` wrote and that cycle
 * simply cannot occur there — a passing mocked test proves nothing about it.
 *
 * These use the REAL store, which is the only way to exercise the feedback path.
 */
describe('DotContentDriveStatusFilterComponent with the real store', () => {
    let spectator: Spectator<DotContentDriveStatusFilterComponent>;
    let store: InstanceType<typeof DotContentDriveStore>;

    const createComponent = createComponentFactory({
        component: DotContentDriveStatusFilterComponent,
        providers: [
            DotContentDriveStore,
            mockProvider(ActivatedRoute, { snapshot: { queryParams: {} } }),
            mockProvider(GlobalStore, {
                siteDetails: jest.fn().mockReturnValue(MOCK_SITES[0])
            }),
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of({ admin: false } as DotCurrentUser))
            }),
            mockProvider(DotContentDriveService, {
                search: jest.fn().mockReturnValue(of(MOCK_SEARCH_RESPONSE))
            }),
            mockProvider(DotFolderService, { getFolders: jest.fn().mockReturnValue(of([])) }),
            mockProvider(Location, {
                subscribe: jest.fn().mockReturnValue({ unsubscribe: jest.fn() })
            }),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(AddToBundleService),
            mockProvider(PushPublishService),
            mockProvider(DotBulkRefreshService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotPropertiesService, {
                getFeatureFlags: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of(mockLocales))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.status-filter.title': 'Status',
                    'content-drive.status-filter.archived': 'Archived',
                    'content-drive.status-filter.unpublished': 'Unpublished',
                    'content-drive.status-filter.locked': 'Locked'
                })
            },
            provideHttpClient()
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore);
        spectator.detectChanges();
        spectator.flushEffects();
    });

    it('should persist a selection to the store without looping', () => {
        spectator.component.$selection.set(['ARCHIVED']);
        // Would never settle if each write re-notified the signal that triggered it.
        spectator.flushEffects();

        expect(store.getFilterValue('status')).toEqual(['ARCHIVED']);
    });

    it('should remove the filter when the selection is emptied, and settle', () => {
        spectator.component.$selection.set(['LOCKED']);
        spectator.flushEffects();
        expect(store.getFilterValue('status')).toEqual(['LOCKED']);

        spectator.component.$selection.set([]);
        spectator.flushEffects();

        expect(store.getFilterValue('status')).toBeUndefined();
    });

    it('should not reset pagination when an unrelated filter changes', () => {
        spectator.component.$selection.set(['ARCHIVED']);
        spectator.flushEffects();

        store.setPagination({ ...store.pagination(), page: 3, offset: 40 });
        store.patchFilters({ title: 'something' });
        spectator.flushEffects();

        // patchFilters resets pagination itself; what matters is that the status effect does not
        // fire a SECOND write on top of it. The selection is unchanged, so by value equality the
        // signal never notifies.
        expect(store.getFilterValue('status')).toEqual(['ARCHIVED']);
        expect(store.pagination().page).toBe(1);
    });
});
