import { expect, it } from '@jest/globals';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { mockProvider } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotCategoryFieldSidebarComponent } from './dot-category-field-sidebar.component';

describe('DotEditContentCategoryFieldSidebarComponent', () => {
    let spectator: Spectator<DotCategoryFieldSidebarComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoryFieldSidebarComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should have `visible` property by default `true`', () => {
        expect(spectator.component.visible).toBe(true);
    });

    it('should have a sidebar', () => {
        expect(spectator.query(byTestId('sidebar'))).not.toBeNull();
    });

    it('should have a clear all button', () => {
        expect(spectator.query(byTestId('clear_all-btn'))).not.toBeNull();
    });

    it('should close the sidebar when you click back', () => {
        const closedSidebarSpy = jest.spyOn(spectator.component.closedSidebar, 'emit');
        const cancelBtn = spectator.query(byTestId('back-btn'));
        expect(cancelBtn).not.toBeNull();

        expect(closedSidebarSpy).not.toHaveBeenCalled();

        spectator.click(cancelBtn);
        expect(closedSidebarSpy).toHaveBeenCalled();
    });
});
