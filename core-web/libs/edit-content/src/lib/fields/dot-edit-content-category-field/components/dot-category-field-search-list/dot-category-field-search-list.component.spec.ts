import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent } from '@dotcms/ui';

import { DotCategoryFieldSearchListComponent } from './dot-category-field-search-list.component';

import { CATEGORY_FIELD_EMPTY_MESSAGES } from '../../../../models/dot-edit-content-field.constant';
import { MockResizeObserver } from '../../../../utils/mocks';
import { CATEGORY_MOCK_TRANSFORMED } from '../../mocks/category-field.mocks';

const mockMessageService = {
    get: jest.fn((key: string) => `${key}`)
};

describe('DotCategoryFieldSearchListComponent', () => {
    let spectator: Spectator<DotCategoryFieldSearchListComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoryFieldSearchListComponent,
        providers: [{ provide: DotMessageService, useValue: mockMessageService }]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false,
            props: {
                categories: CATEGORY_MOCK_TRANSFORMED,
                selected: CATEGORY_MOCK_TRANSFORMED,
                status: ComponentStatus.LOADED
            }
        });

        spectator.detectChanges();
    });

    beforeAll(() => {
        global.ResizeObserver = MockResizeObserver;
    });

    it('should show the skeleton if the component is loading', () => {
        spectator.setInput('status', ComponentStatus.LOADING);
        spectator.detectChanges();
        expect(spectator.query(byTestId('categories-skeleton'))).not.toBeNull();
        expect(spectator.query(byTestId('categories-table'))).toBeNull();
    });

    it('should show the table if the component is not loading', () => {
        spectator.setInput('status', ComponentStatus.LOADED);
        spectator.detectChanges();
        expect(spectator.query(byTestId('categories-table'))).not.toBeNull();
        expect(spectator.query(byTestId('categories-skeleton'))).toBeNull();
    });

    it('should render table header', () => {
        const rows = spectator.queryAll(byTestId('table-header'));
        expect(rows.length).toBe(1);
    });

    it('should render table header with 3 columns, checkbox, name of  category and parent path', () => {
        expect(spectator.query(byTestId('table-header-checkbox'))).not.toBeNull();
        expect(spectator.query(byTestId('table-header-category-name'))).not.toBeNull();
        expect(spectator.query(byTestId('table-header-parents'))).not.toBeNull();
    });

    it('should render table with categories', () => {
        const rows = spectator.queryAll(byTestId('table-row'));
        expect(rows.length).toBe(CATEGORY_MOCK_TRANSFORMED.length);
    });

    it('should render `dot-empty-container` with `empty` configuration ', () => {
        const expectedConfig = CATEGORY_FIELD_EMPTY_MESSAGES.empty;
        spectator.setInput('status', ComponentStatus.LOADED);
        spectator.setInput('categories', []);
        spectator.detectChanges();

        expect(spectator.query(DotEmptyContainerComponent)).not.toBeNull();
        expect(spectator.component.$emptyOrErrorMessage()).toEqual(expectedConfig);
    });

    it('should render `dot-empty-container` with `error` configuration ', () => {
        const expectedConfig = CATEGORY_FIELD_EMPTY_MESSAGES[ComponentStatus.ERROR];
        spectator.setInput('status', ComponentStatus.ERROR);
        spectator.setInput('categories', []);

        spectator.detectChanges();
        expect(spectator.query(DotEmptyContainerComponent)).not.toBeNull();
        expect(spectator.component.$emptyOrErrorMessage()).toEqual(expectedConfig);
    });

    it('should emit $itemChecked event when an item is selected', () => {
        const itemCheckedSpy = jest.spyOn(spectator.component.$itemChecked, 'emit');
        const firstCategoryRow = spectator.query(byTestId('table-row'));
        const checkboxInput = firstCategoryRow.querySelector(
            'input[type="checkbox"]'
        ) as HTMLInputElement;

        spectator.click(checkboxInput);
        spectator.detectChanges();

        expect(itemCheckedSpy).toHaveBeenCalledWith(CATEGORY_MOCK_TRANSFORMED[0]);
    });

    it('should emit $removeItem event when an item is unselected', () => {
        const removeItemSpy = jest.spyOn(spectator.component.$removeItem, 'emit');
        const firstCategoryRow = spectator.query(byTestId('table-row'));
        const checkboxInput = firstCategoryRow.querySelector(
            'input[type="checkbox"]'
        ) as HTMLInputElement;

        spectator.click(checkboxInput); // Select
        spectator.click(checkboxInput); // Unselect
        spectator.detectChanges();

        expect(removeItemSpy).toHaveBeenCalledWith(CATEGORY_MOCK_TRANSFORMED[0].key);
    });

    it('should emit $itemChecked event with all items when header checkbox is selected', () => {
        const itemCheckedSpy = jest.spyOn(spectator.component.$itemChecked, 'emit');
        const headerCheckbox = spectator
            .query(byTestId('table-header-checkbox'))
            .querySelector('input[type="checkbox"]') as HTMLInputElement;

        spectator.click(headerCheckbox);
        spectator.detectChanges();

        expect(itemCheckedSpy).toHaveBeenCalledWith(CATEGORY_MOCK_TRANSFORMED);
    });

    it('should emit $removeItem event with all keys when header checkbox is unselected', () => {
        const removeItemSpy = jest.spyOn(spectator.component.$removeItem, 'emit');
        const headerCheckbox = spectator
            .query(byTestId('table-header-checkbox'))
            .querySelector('input[type="checkbox"]') as HTMLInputElement;

        spectator.click(headerCheckbox); // select all
        spectator.click(headerCheckbox); // unselect all
        spectator.detectChanges();

        const allKeys = CATEGORY_MOCK_TRANSFORMED.map((category) => category.key);
        expect(removeItemSpy).toHaveBeenCalledWith(allKeys);
    });
});
