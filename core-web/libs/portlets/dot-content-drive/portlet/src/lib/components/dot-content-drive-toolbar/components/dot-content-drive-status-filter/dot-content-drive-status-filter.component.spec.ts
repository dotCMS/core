import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';

import { signal } from '@angular/core';

import { Popover } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';
import { DotChipFilterComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveStatusFilterComponent } from './dot-content-drive-status-filter.component';

import { CONTENT_STATUS, STATUS_FILTER_KEY } from '../../../../shared/constants';
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
