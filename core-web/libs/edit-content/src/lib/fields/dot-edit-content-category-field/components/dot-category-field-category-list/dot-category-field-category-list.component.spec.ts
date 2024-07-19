import { expect } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import {
    DotCategoryFieldCategoryListComponent,
    MINIMUM_CATEGORY_COLUMNS
} from './dot-category-field-category-list.component';

import {
    CATEGORY_LIST_MOCK,
    CATEGORY_LIST_MOCK_TRANSFORMED_MATRIX,
    CATEGORY_MOCK_TRANSFORMED,
    SELECTED_LIST_MOCK
} from '../../mocks/category-field.mocks';
import { DotCategoryFieldListSkeletonComponent } from '../dot-category-field-list-skeleton/dot-category-field-list-skeleton.component';

describe('DotCategoryFieldCategoryListComponent', () => {
    let spectator: Spectator<DotCategoryFieldCategoryListComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoryFieldCategoryListComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('categories', CATEGORY_LIST_MOCK_TRANSFORMED_MATRIX);
        spectator.setInput('selected', SELECTED_LIST_MOCK);
        spectator.setInput('isLoading', false);

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should render correct number of category columns', () => {
        expect(spectator.queryAll(byTestId('category-column')).length).toBe(
            CATEGORY_LIST_MOCK.length
        );
    });

    it('should render correct number of category items', () => {
        expect(spectator.queryAll(byTestId('category-item')).length).toBe(
            CATEGORY_LIST_MOCK.flat().length
        );
    });

    it('should render correct number of category item labels', () => {
        expect(spectator.queryAll(byTestId('category-item-label')).length).toBe(
            CATEGORY_LIST_MOCK.flat().length
        );
    });

    it('should render correct number of empty columns', () => {
        expect(spectator.queryAll(byTestId('category-column-empty')).length).toBe(
            MINIMUM_CATEGORY_COLUMNS - CATEGORY_LIST_MOCK.length
        );
    });

    it('should render one category item with child indicator', () => {
        expect(spectator.queryAll(byTestId('category-item-with-child')).length).toBe(1);
    });

    it('should emit the correct item when clicked', () => {
        const emitSpy = jest.spyOn(spectator.component.rowClicked, 'emit');
        const items = spectator.queryAll(byTestId('category-item'));
        spectator.click(items[0]);

        expect(emitSpy).toHaveBeenCalledWith({
            index: 0,
            item: CATEGORY_LIST_MOCK_TRANSFORMED_MATRIX[0][0]
        });
    });

    it('should apply selected class to the correct item', () => {
        // spectator = createComponent();

        spectator.setInput('categories', [CATEGORY_MOCK_TRANSFORMED]);
        spectator.setInput('selected', SELECTED_LIST_MOCK);
        spectator.setInput('isLoading', false);

        spectator.detectChanges();

        const items = spectator.queryAll(byTestId('category-item'));

        expect(items[0].className).toContain('category-list__item--selected');
        expect(items[1].className).not.toContain('category-list__item--selected');
    });

    it('should not render any empty columns when there are enough categories', () => {
        const minColumns = 4;
        const testCategories = Array(minColumns).fill(CATEGORY_LIST_MOCK_TRANSFORMED_MATRIX[0]);

        // spectator = createComponent();

        spectator.setInput('categories', testCategories);
        spectator.setInput('selected', SELECTED_LIST_MOCK);
        spectator.setInput('isLoading', false);

        spectator.detectChanges();

        expect(spectator.queryAll(byTestId('category-column-empty')).length).toBe(0);
    });

    it('should render the skeleton component if is loading', () => {
        spectator.setInput('categories', [CATEGORY_MOCK_TRANSFORMED]);
        spectator.setInput('selected', SELECTED_LIST_MOCK);
        spectator.setInput('isLoading', true);

        spectator.detectChanges();

        expect(spectator.query(DotCategoryFieldListSkeletonComponent)).not.toBeNull();
    });
});
