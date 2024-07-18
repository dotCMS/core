import { expect, it } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Sidebar } from 'primeng/sidebar';

import { DotMessageService } from '@dotcms/data-access';

import { DotCategoryFieldSidebarComponent } from './dot-category-field-sidebar.component';

import { CATEGORY_LIST_MOCK } from '../../mocks/category-field.mocks';
import { CategoriesService } from '../../services/categories.service';
import { CategoryFieldStore } from '../../store/content-category-field.store';
import { DotCategoryFieldCategoryListComponent } from '../dot-category-field-category-list/dot-category-field-category-list.component';

describe('DotEditContentCategoryFieldSidebarComponent', () => {
    let spectator: Spectator<DotCategoryFieldSidebarComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoryFieldSidebarComponent,
        providers: [mockProvider(DotMessageService), CategoryFieldStore]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                visible: true
            },
            providers: [
                mockProvider(CategoriesService, {
                    getChildren: jest.fn().mockReturnValue(of(CATEGORY_LIST_MOCK))
                })
            ]
        });

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should have `visible` property set to `true` by default', () => {
        expect(spectator.component.visible).toBe(true);
        expect(spectator.query(Sidebar)).not.toBeNull();
    });

    it('should render a "clear all" button', () => {
        spectator.detectChanges();
        expect(spectator.query(byTestId('clear_all-btn'))).not.toBeNull();
    });

    it('should emit event to close sidebar when "back" button is clicked', () => {
        const closedSidebarSpy = jest.spyOn(spectator.component.closedSidebar, 'emit');
        const cancelBtn = spectator.query(byTestId('back-btn'));
        expect(cancelBtn).not.toBeNull();

        expect(closedSidebarSpy).not.toHaveBeenCalled();

        spectator.click(cancelBtn);
        expect(closedSidebarSpy).toHaveBeenCalled();
    });

    it('should render the CategoryFieldCategoryList component', () => {
        expect(spectator.query(DotCategoryFieldCategoryListComponent)).not.toBeNull();
    });
});
