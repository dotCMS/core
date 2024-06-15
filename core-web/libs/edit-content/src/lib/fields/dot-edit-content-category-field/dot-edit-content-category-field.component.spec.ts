import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { fakeAsync } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';

import { DotEditContentCategoryFieldSidebarComponent } from './components/dot-edit-content-category-field-sidebar/dot-edit-content-category-field-sidebar.component';
import { DotEditContentCategoryFieldComponent } from './dot-edit-content-category-field.component';
import { CLOSE_SIDEBAR_CSS_DELAY_MS } from './dot-edit-content-category-field.const';

describe('DotEditContentCategoryFieldComponent', () => {
    let spectator: Spectator<DotEditContentCategoryFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentCategoryFieldComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    describe('Elements', () => {
        it('should render a button for selecting categories', () => {
            expect(spectator.query(byTestId('show-sidebar-btn'))).not.toBeNull();
        });

        it('should display the category list with chips when there are categories', () => {
            spectator.component.values = [];
            spectator.detectComponentChanges();
            expect(spectator.query(byTestId('category-chip-list'))).toBeNull();

            spectator.component.values = [{ id: 1, value: 'Streetwear' }];
            spectator.detectComponentChanges();
            expect(spectator.query(byTestId('category-chip-list'))).not.toBeNull();
        });
    });

    describe('Interactions', () => {
        it('should invoke `showCategoriesSidebar` method when the select button is clicked', () => {
            const selectBtn = spectator.query(byTestId('show-sidebar-btn'));
            const showCategoriesSidebarSpy = jest.spyOn(
                spectator.component,
                'showCategoriesSidebar'
            );
            expect(selectBtn).not.toBeNull();

            spectator.click(selectBtn);

            expect(showCategoriesSidebarSpy).toHaveBeenCalled();
        });

        it('should disable the `Select` button after `showCategoriesSidebar` method is invoked', () => {
            const selectBtn = spectator.query(byTestId('show-sidebar-btn')) as HTMLButtonElement;
            expect(selectBtn).not.toBeNull();

            spectator.click(selectBtn);

            expect(selectBtn.disabled).toBe(true);
        });

        it('should create a DotEditContentCategoryFieldSidebarComponent instance when the `Select` button is clicked', () => {
            const selectBtn = spectator.query(byTestId('show-sidebar-btn')) as HTMLButtonElement;
            expect(selectBtn).not.toBeNull();
            expect(spectator.query(DotEditContentCategoryFieldSidebarComponent)).toBeNull();

            spectator.click(selectBtn);
            expect(spectator.query(DotEditContentCategoryFieldSidebarComponent)).not.toBeNull();
        });

        it('should remove DotEditContentCategoryFieldSidebarComponent when `closedSidebar` emit', fakeAsync(() => {
            const selectBtn = spectator.query(byTestId('show-sidebar-btn')) as HTMLButtonElement;
            expect(selectBtn).not.toBeNull();
            spectator.click(selectBtn);

            const sidebarComponentRef = spectator.query(
                DotEditContentCategoryFieldSidebarComponent
            );
            expect(sidebarComponentRef).not.toBeNull();

            sidebarComponentRef.closedSidebar.emit();

            spectator.detectComponentChanges();

            // Due to a delay in the pipe of the subscription
            spectator.tick(CLOSE_SIDEBAR_CSS_DELAY_MS + 100);

            expect(spectator.query(DotEditContentCategoryFieldSidebarComponent)).toBeNull();

            expect(selectBtn.disabled).toBe(false);
        }));
    });
});
