import { createFakeEvent } from '@ngneat/spectator';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Table, TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent } from '@dotcms/ui';

import { DotCategoryFieldSearchListComponent } from './dot-category-field-search-list.component';

import { CATEGORY_FIELD_EMPTY_MESSAGES } from '../../../../models/dot-edit-content-field.constant';
import { CATEGORY_MOCK_TRANSFORMED } from '../../mocks/category-field.mocks';

const mockMessageService = {
    get: jest.fn((key: string) => `${key}`)
};

describe('DotCategoryFieldSearchListComponent', () => {
    let spectator: Spectator<DotCategoryFieldSearchListComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoryFieldSearchListComponent,
        providers: [{ provide: DotMessageService, useValue: mockMessageService }],
        imports: [TableModule]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false,
            props: {
                selected: CATEGORY_MOCK_TRANSFORMED,
                categories: CATEGORY_MOCK_TRANSFORMED,
                state: ComponentStatus.LOADED
            } as unknown
        });
    });

    it('should show the skeleton if the component is loading', () => {
        spectator.setInput('state', ComponentStatus.LOADING);
        spectator.detectChanges();
        expect(spectator.query(byTestId('categories-skeleton'))).not.toBeNull();
        expect(spectator.query(byTestId('categories-table'))).toBeNull();
    });

    it('should show the table if the component is not loading', () => {
        spectator.setInput('state', ComponentStatus.LOADED);
        spectator.detectChanges();
        expect(spectator.query(byTestId('categories-table'))).not.toBeNull();
        expect(spectator.query(byTestId('categories-skeleton'))).toBeNull();
    });

    it('should render table header', () => {
        spectator.detectChanges();
        const rows = spectator.queryAll(byTestId('table-header'));
        expect(rows.length).toBe(1);
    });

    it('should render table header with 3 columns, checkbox, name of  category and parent path', () => {
        spectator.detectChanges();
        expect(spectator.query(byTestId('table-header-checkbox'))).not.toBeNull();
        expect(spectator.query(byTestId('table-header-category-name'))).not.toBeNull();
        expect(spectator.query(byTestId('table-header-parents'))).not.toBeNull();
    });

    it('should render table with categories', () => {
        spectator.detectChanges();
        const rows = spectator.queryAll(byTestId('table-row'));
        expect(rows.length).toBe(CATEGORY_MOCK_TRANSFORMED.length);
    });

    it('should render `dot-empty-container` with `no results` configuration ', () => {
        const expectedConfig = CATEGORY_FIELD_EMPTY_MESSAGES.noResults;
        spectator.setInput('state', ComponentStatus.LOADED);
        spectator.setInput('categories', []);
        spectator.detectChanges();

        expect(spectator.query(DotEmptyContainerComponent)).not.toBeNull();
        expect(spectator.component.$emptyOrErrorMessage()).toEqual(expectedConfig);
    });

    it('should render `dot-empty-container` with `error` configuration ', () => {
        const expectedConfig = CATEGORY_FIELD_EMPTY_MESSAGES[ComponentStatus.ERROR];
        spectator.setInput('state', ComponentStatus.ERROR);
        spectator.setInput('categories', []);

        spectator.detectChanges();
        expect(spectator.query(DotEmptyContainerComponent)).not.toBeNull();
        expect(spectator.component.$emptyOrErrorMessage()).toEqual(expectedConfig);
    });

    it('should emit $itemChecked event when an item is selected', async () => {
        const itemCheckedSpy = jest.spyOn(spectator.component.itemChecked, 'emit');
        spectator.detectChanges();
        spectator.triggerEventHandler(Table, 'onRowSelect', { data: CATEGORY_MOCK_TRANSFORMED[0] });
        expect(itemCheckedSpy).toHaveBeenCalledWith(CATEGORY_MOCK_TRANSFORMED[0]);
    });

    it('should emit $removeItem event when an item is unselected', () => {
        const removeItemSpy = jest.spyOn(spectator.component.removeItem, 'emit');
        spectator.detectChanges();
        spectator.triggerEventHandler(Table, 'onRowUnselect', {
            data: CATEGORY_MOCK_TRANSFORMED[0]
        });
        expect(removeItemSpy).toHaveBeenCalledWith(CATEGORY_MOCK_TRANSFORMED[0].key);
    });

    it('should emit $itemChecked event with all items when header checkbox is selected', () => {
        const itemCheckedSpy = jest.spyOn(spectator.component.itemChecked, 'emit');

        spectator.detectChanges();
        spectator.triggerEventHandler(Table, 'onHeaderCheckboxToggle', {
            originalEvent: createFakeEvent('click'),
            checked: true
        });

        expect(itemCheckedSpy).toHaveBeenCalledWith(CATEGORY_MOCK_TRANSFORMED);
    });

    it('should emit $removeItem event with all keys when header checkbox is unselected', () => {
        const removeItemSpy = jest.spyOn(spectator.component.removeItem, 'emit');
        spectator.detectChanges();
        spectator.triggerEventHandler(Table, 'onHeaderCheckboxToggle', {
            originalEvent: createFakeEvent('click'),
            checked: true
        });
        spectator.triggerEventHandler(Table, 'onHeaderCheckboxToggle', {
            originalEvent: createFakeEvent('click'),
            checked: false
        });

        const allKeys = CATEGORY_MOCK_TRANSFORMED.map((category) => category.key);
        expect(removeItemSpy).toHaveBeenCalledWith(allKeys);
    });
});
