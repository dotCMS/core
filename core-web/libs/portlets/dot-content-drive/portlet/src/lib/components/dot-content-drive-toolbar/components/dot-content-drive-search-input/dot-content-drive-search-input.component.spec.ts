import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@openng/spectator/jest';

import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { ALL_FOLDER, DotSearchInputComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveSearchInputComponent } from './dot-content-drive-search-input.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveSearchInputComponent', () => {
    let spectator: Spectator<DotContentDriveSearchInputComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    const createComponent = createComponentFactory({
        component: DotContentDriveSearchInputComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                getFilterValue: jest.fn().mockReturnValue(undefined),
                setGlobalSearch: jest.fn(),
                setSelectedNode: jest.fn()
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ search: 'Search' })
            }
        ],
        detectChanges: false
    });

    const searchInput = () =>
        spectator.fixture.debugElement.query(By.directive(DotSearchInputComponent));

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        store.getFilterValue.mockReset().mockReturnValue(undefined);
    });

    afterEach(() => jest.clearAllMocks());

    it('should render the shared search input', () => {
        spectator.detectChanges();

        expect(searchInput()).toBeTruthy();
    });

    it('should bind the store title filter as the value', () => {
        store.getFilterValue.mockReturnValue('blog');
        spectator.detectChanges();

        expect(searchInput().componentInstance.$value()).toBe('blog');
        expect(store.getFilterValue).toHaveBeenCalledWith('title');
    });

    it('should bind an empty value when no title filter is set', () => {
        spectator.detectChanges();

        expect(searchInput().componentInstance.$value()).toBe('');
    });

    it('should push the emitted term to the store and reset the folder scope', () => {
        spectator.detectChanges();

        spectator.triggerEventHandler(searchInput(), 'search', 'blog');

        expect(store.setGlobalSearch).toHaveBeenCalledWith('blog');
        expect(store.setSelectedNode).toHaveBeenCalledWith(ALL_FOLDER);
    });

    it('should clear the search in the store when an empty term is emitted', () => {
        store.getFilterValue.mockReturnValue('blog');
        spectator.detectChanges();

        spectator.triggerEventHandler(searchInput(), 'search', '');

        expect(store.setGlobalSearch).toHaveBeenCalledWith('');
        expect(store.setSelectedNode).toHaveBeenCalledWith(ALL_FOLDER);
    });
});
