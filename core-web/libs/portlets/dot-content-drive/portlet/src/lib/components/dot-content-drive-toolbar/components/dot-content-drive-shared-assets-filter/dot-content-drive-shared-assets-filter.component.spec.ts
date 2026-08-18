import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotChipFilterComponent } from '@dotcms/portlets/content-drive/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveSharedAssetsFilterComponent } from './dot-content-drive-shared-assets-filter.component';

import {
    SHARED_ASSETS_DISABLED_VALUE,
    SHARED_ASSETS_ENABLED_VALUE,
    SHARED_ASSETS_FILTER_KEY
} from '../../../../shared/constants';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveSharedAssetsFilterComponent', () => {
    let spectator: Spectator<DotContentDriveSharedAssetsFilterComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    /**
     * Backs the mocked `getFilterValue`. The component reads it through a computed, so a plain
     * `mockReturnValue` would be invisible to it: without a signal dependency there is nothing to
     * invalidate, and the first value would be cached for the life of the test.
     */
    const storedValue = signal<string | undefined>(SHARED_ASSETS_ENABLED_VALUE);

    const createComponent = createComponentFactory({
        component: DotContentDriveSharedAssetsFilterComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                patchFilters: jest.fn(),
                removeFilter: jest.fn(),
                getFilterValue: jest.fn(() => storedValue())
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.shared-assets-filter.title': 'Show Shared Assets',
                    'dot.common.remove': 'Remove'
                })
            }
        ]
    });

    const chip = () => spectator.query(DotChipFilterComponent);

    /** The chip is the whole control, so every interaction goes through its outputs. */
    const clickChip = () => {
        spectator.triggerEventHandler(DotChipFilterComponent, 'clicked', new Event('click'));
        spectator.detectChanges();
    };

    const removeChip = () => {
        spectator.triggerEventHandler(DotChipFilterComponent, 'removed', undefined);
        spectator.detectChanges();
    };

    const setStoredValue = (value: string | undefined) => {
        storedValue.set(value);
        spectator.detectChanges();
    };

    beforeEach(() => {
        storedValue.set(SHARED_ASSETS_ENABLED_VALUE);
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        spectator.detectChanges();
    });

    it('should render a toggle chip labelled from the message bundle', () => {
        expect(spectator.query(byTestId('shared-assets-filter-chip'))).toBeTruthy();
        expect(chip()?.mode()).toBe('toggle');
        expect(chip()?.title()).toBe('Show Shared Assets');
    });

    describe('state', () => {
        it('should read as on when the filter holds the enabled value', () => {
            setStoredValue(SHARED_ASSETS_ENABLED_VALUE);

            expect(chip()?.toggled()).toBe(true);
        });

        it('should read as on when the filter is absent, which is the default', () => {
            setStoredValue(undefined);

            expect(chip()?.toggled()).toBe(true);
        });

        it('should read as off only when the filter explicitly disables it', () => {
            setStoredValue(SHARED_ASSETS_DISABLED_VALUE);

            expect(chip()?.toggled()).toBe(false);
        });

        it('should read the state from the shared-assets filter key', () => {
            expect(store.getFilterValue).toHaveBeenCalledWith(SHARED_ASSETS_FILTER_KEY);
        });
    });

    describe('toggling', () => {
        it('should turn shared assets off when clicked while on', () => {
            setStoredValue(SHARED_ASSETS_ENABLED_VALUE);

            clickChip();

            expect(store.patchFilters).toHaveBeenCalledWith({
                [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_DISABLED_VALUE
            });
        });

        it('should turn shared assets back on when clicked while off', () => {
            setStoredValue(SHARED_ASSETS_DISABLED_VALUE);

            clickChip();

            expect(store.patchFilters).toHaveBeenCalledWith({
                [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_ENABLED_VALUE
            });
        });

        it('should write the on value rather than dropping the key, so the URL always states it', () => {
            setStoredValue(SHARED_ASSETS_DISABLED_VALUE);

            clickChip();

            expect(store.removeFilter).not.toHaveBeenCalled();
        });

        it('should turn shared assets off when the chip is removed', () => {
            setStoredValue(SHARED_ASSETS_ENABLED_VALUE);

            removeChip();

            expect(store.patchFilters).toHaveBeenCalledWith({
                [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_DISABLED_VALUE
            });
        });
    });
});
