import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@openng/spectator/jest';

import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { DotSearchInputComponent } from '@dotcms/ui';
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
                selectRootNode: jest.fn()
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
        expect(store.selectRootNode).toHaveBeenCalled();
    });

    it('should clear the search in the store when an empty term is emitted', () => {
        store.getFilterValue.mockReturnValue('blog');
        spectator.detectChanges();

        spectator.triggerEventHandler(searchInput(), 'search', '');

        expect(store.setGlobalSearch).toHaveBeenCalledWith('');
        expect(store.selectRootNode).toHaveBeenCalled();
    });

    // The claim lives here rather than in the shell because this component is the one holding the
    // search box; the shell would have to reach three levels down to find it.
    describe('search shortcut', () => {
        const press = () =>
            document.dispatchEvent(
                new KeyboardEvent('keydown', {
                    key: 'k',
                    metaKey: true,
                    bubbles: true,
                    cancelable: true
                })
            );

        const input = () => spectator.query('[data-testid="search-input-field"]');

        it('should focus the search field', () => {
            spectator.detectChanges();

            press();

            expect(document.activeElement).toBe(input());
        });

        it('should preserve a term already entered', () => {
            store.getFilterValue.mockReturnValue('blog');
            spectator.detectChanges();

            press();

            expect((input() as HTMLInputElement).value).toBe('blog');
        });

        it('should suppress the browser default so its own search affordance never opens', () => {
            spectator.detectChanges();

            const event = new KeyboardEvent('keydown', {
                key: 'k',
                metaKey: true,
                bubbles: true,
                cancelable: true
            });
            document.dispatchEvent(event);

            expect(event.defaultPrevented).toBe(true);
        });

        it('should release the claim when the component is destroyed', () => {
            spectator.detectChanges();
            const field = input();

            spectator.fixture.destroy();
            press();

            expect(document.activeElement).not.toBe(field);
        });
    });
});
