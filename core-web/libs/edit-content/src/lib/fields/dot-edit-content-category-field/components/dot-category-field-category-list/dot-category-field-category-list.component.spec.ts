import { expect } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import {
    DotCategoryFieldCategoryListComponent,
    MINIMUM_CATEGORY_COLUMNS
} from './dot-category-field-category-list.component';

import { CATEGORIES_MOCK, SELECTED_MOCK } from '../../mocks/category-field.mocks';

describe('DotCategoryFieldCategoryListComponent', () => {
    let spectator: Spectator<DotCategoryFieldCategoryListComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoryFieldCategoryListComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                categories: CATEGORIES_MOCK,
                selected: SELECTED_MOCK
            }
        });

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should render correct number of category columns', () => {
        expect(spectator.queryAll(byTestId('category-column')).length).toBe(CATEGORIES_MOCK.length);
    });

    it('should render correct number of category items', () => {
        expect(spectator.queryAll(byTestId('category-item')).length).toBe(
            CATEGORIES_MOCK.flat().length
        );
    });

    it('should render correct number of category item labels', () => {
        expect(spectator.queryAll(byTestId('category-item-label')).length).toBe(
            CATEGORIES_MOCK.flat().length
        );
    });

    it('should render correct number of empty columns', () => {
        expect(spectator.queryAll(byTestId('category-column-empty')).length).toBe(
            MINIMUM_CATEGORY_COLUMNS - CATEGORIES_MOCK.length
        );
    });

    it('should render one category item with child indicator', () => {
        expect(spectator.queryAll(byTestId('category-item-with-child')).length).toBe(1);
    });

    it('should emit the correct item when clicked', () => {
        const emitSpy = jest.spyOn(spectator.component.itemClicked, 'emit');
        const items = spectator.queryAll(byTestId('category-item'));
        spectator.click(items[0]);

        expect(emitSpy).toHaveBeenCalledWith({
            index: 0,
            item: CATEGORIES_MOCK[0][0]
        });
    });

    it('should apply selected class to the correct item', () => {
        const items = spectator.queryAll(byTestId('category-item'));
        expect(items[0].className).toContain('category-list__item--selected');
    });

    it('should not render any empty columns when there are enough categories', () => {
        const minColumns = 4;
        const testCategories = Array(minColumns).fill(CATEGORIES_MOCK[0]);

        spectator = createComponent({
            props: {
                categories: testCategories,
                selected: SELECTED_MOCK
            }
        });

        spectator.detectChanges();

        expect(spectator.queryAll(byTestId('category-column-empty')).length).toBe(0);
    });
});
