import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { HttpClient } from '@angular/common/http';
import { fakeAsync } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';

import { DotCategoryFieldSidebarComponent } from './components/dot-category-field-sidebar/dot-category-field-sidebar.component';
import { DotEditContentCategoryFieldComponent } from './dot-edit-content-category-field.component';
import { CLOSE_SIDEBAR_CSS_DELAY_MS } from './dot-edit-content-category-field.const';
import { CATEGORY_FIELD_CONTENTLET_MOCK, CATEGORY_FIELD_MOCK } from './mocks/category-field.mocks';
import { CategoriesService } from './services/categories.service';
import { CategoryFieldStore } from './store/content-category-field.store';

describe('DotEditContentCategoryFieldComponent', () => {
    let spectator: Spectator<DotEditContentCategoryFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentCategoryFieldComponent,
        imports: [MockComponent(DotCategoryFieldSidebarComponent)],
        componentViewProviders: [CategoryFieldStore],
        providers: [
            mockProvider(DotMessageService),
            mockProvider(CategoriesService),
            mockProvider(HttpClient)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentlet: CATEGORY_FIELD_CONTENTLET_MOCK,
                field: CATEGORY_FIELD_MOCK
            }
        });

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    describe('Elements', () => {
        it('should render a button for selecting categories', () => {
            expect(spectator.query(byTestId('show-sidebar-btn'))).not.toBeNull();
        });

        it('should not display the category list with chips when there are no categories', () => {
            spectator = createComponent({
                props: {
                    contentlet: [],
                    field: CATEGORY_FIELD_MOCK
                }
            });

            spectator.detectChanges();

            expect(spectator.query(byTestId('category-chip-list'))).toBeNull();
        });

        it('should display the category list with chips when there are categories', () => {
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

            spectator.detectChanges();

            expect(selectBtn.disabled).toBe(true);
        });

        it('should create a DotEditContentCategoryFieldSidebarComponent instance when the `Select` button is clicked', () => {
            const selectBtn = spectator.query(byTestId('show-sidebar-btn')) as HTMLButtonElement;
            expect(selectBtn).not.toBeNull();
            expect(spectator.query(DotCategoryFieldSidebarComponent)).toBeNull();

            spectator.click(selectBtn);

            expect(spectator.query(DotCategoryFieldSidebarComponent)).not.toBeNull();
        });

        it('should remove DotEditContentCategoryFieldSidebarComponent when `closedSidebar` emit', fakeAsync(() => {
            const selectBtn = spectator.query(byTestId('show-sidebar-btn')) as HTMLButtonElement;
            expect(selectBtn).not.toBeNull();
            spectator.click(selectBtn);

            const sidebarComponentRef = spectator.query(DotCategoryFieldSidebarComponent);
            expect(sidebarComponentRef).not.toBeNull();

            sidebarComponentRef.closedSidebar.emit();

            spectator.detectComponentChanges();

            // Due to a delay in the pipe of the subscription
            spectator.tick(CLOSE_SIDEBAR_CSS_DELAY_MS + 100);

            expect(spectator.query(DotCategoryFieldSidebarComponent)).toBeNull();

            expect(selectBtn.disabled).toBe(false);
        }));
    });
});
