import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

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

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });
});
