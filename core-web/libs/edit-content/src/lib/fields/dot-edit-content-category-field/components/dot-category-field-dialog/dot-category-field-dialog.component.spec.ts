import { expect, it } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Dialog } from 'primeng/dialog';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';

import { DotCategoryFieldDialogComponent } from './dot-category-field-dialog.component';

import { CATEGORY_LIST_MOCK } from '../../mocks/category-field.mocks';
import { CategoriesService } from '../../services/categories.service';
import { CategoryFieldStore } from '../../store/content-category-field.store';
import { DotCategoryFieldCategoryListComponent } from '../dot-category-field-category-list/dot-category-field-category-list.component';
import { DotCategoryFieldSelectedComponent } from '../dot-category-field-selected/dot-category-field-selected.component';

describe('DotCategoryFieldDialogComponent', () => {
    let spectator: Spectator<DotCategoryFieldDialogComponent>;
    let store: InstanceType<typeof CategoryFieldStore>;

    const createComponent = createComponentFactory({
        component: DotCategoryFieldDialogComponent,
        providers: [mockProvider(DotMessageService), CategoryFieldStore]
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [
                mockProvider(CategoriesService, {
                    getChildren: jest.fn().mockReturnValue(of(CATEGORY_LIST_MOCK))
                }),
                mockProvider(DotHttpErrorManagerService)
            ]
        });
        spectator.setInput('isVisible', true);

        store = spectator.inject(CategoryFieldStore, true);

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should have `visible` property set to `true` by default', () => {
        expect(spectator.component.$isVisible()).toBe(true);
        expect(spectator.query(Dialog)).not.toBeNull();
    });

    it('should render the selected categories list when there are selected categories', () => {
        store.addSelected({ key: '1234', value: 'test' });

        spectator.detectChanges();
        expect(spectator.query(byTestId('clear_all-btn'))).not.toBeNull();
        expect(spectator.query(DotCategoryFieldSelectedComponent)).not.toBeNull();
    });

    it('should render the empty state when there are no selected categories', () => {
        spectator.detectChanges();
        expect(spectator.query(byTestId('clear_all-btn'))).toBeNull();
        expect(spectator.query(byTestId('category-field__empty-state'))).not.toBeNull();
    });

    it('should have the correct configuration for the dialog.', () => {
        const closedDialogSpy = jest.spyOn(spectator.component.closedDialog, 'emit');
        const dialog = spectator.query(Dialog);

        expect(dialog.draggable).toBe(false);
        expect(dialog.resizable).toBe(false);
        expect(dialog.modal).toBe(true);
        expect(dialog.modal).toBe(true);

        dialog.onHide.emit();

        expect(closedDialogSpy).toHaveBeenCalled();
    });

    it('should close the dialog when the close button is clicked', () => {
        const closedDialogSpy = jest.spyOn(spectator.component.closedDialog, 'emit');
        spectator.click(byTestId('dialog-cancel'));

        expect(closedDialogSpy).toHaveBeenCalled();
    });

    it('should save the changes and apply the categories when the apply button is clicked', () => {
        const closedDialogSpy = jest.spyOn(spectator.component.closedDialog, 'emit');
        const addConfirmedCategoriesSky = jest.spyOn(store, 'applyDialogSelection');
        spectator.detectChanges();

        spectator.click(byTestId('dialog-apply'));

        expect(closedDialogSpy).toHaveBeenCalled();
        expect(addConfirmedCategoriesSky).toHaveBeenCalled();
    });

    it('should render the CategoryFieldCategoryList component', () => {
        expect(spectator.query(DotCategoryFieldCategoryListComponent)).not.toBeNull();
    });
});
