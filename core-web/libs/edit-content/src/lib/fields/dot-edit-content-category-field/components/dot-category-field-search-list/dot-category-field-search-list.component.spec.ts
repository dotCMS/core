import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotCategoryFieldSearchListComponent } from './dot-category-field-search-list.component';

import { MockResizeObserver } from '../../../../utils/mocks';
import { CATEGORY_MOCK_TRANSFORMED, SELECTED_LIST_MOCK } from '../../mocks/category-field.mocks';

describe('DotCategoryFieldSearchListComponent', () => {
    let spectator: Spectator<DotCategoryFieldSearchListComponent>;
    const createComponent = createComponentFactory({
        component: DotCategoryFieldSearchListComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                categories: CATEGORY_MOCK_TRANSFORMED,
                selected: SELECTED_LIST_MOCK,
                isLoading: false
            }
        });

        spectator.detectChanges();
    });

    beforeAll(() => {
        global.ResizeObserver = MockResizeObserver;
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should show the skeleton if the component is loading', () => {
        spectator.setInput('isLoading', true);
        spectator.detectChanges();
        expect(spectator.query(byTestId('categories-skeleton'))).not.toBeNull();
        expect(spectator.query(byTestId('categories-table'))).toBeNull();
    });

    it('should show the table if the component is not loading', () => {
        spectator.setInput('isLoading', false);
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
});
