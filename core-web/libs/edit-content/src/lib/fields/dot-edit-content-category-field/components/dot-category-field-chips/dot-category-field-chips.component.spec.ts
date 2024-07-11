import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotCategoryFieldChipsComponent } from './dot-category-field-chips.component';

import { CATEGORIES_KEY_VALUE } from '../../mocks/category-field.mocks';

describe('DotCategoryFieldChipsComponent', () => {
    let spectator: Spectator<DotCategoryFieldChipsComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoryFieldChipsComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                $max: 8,
                $categories: CATEGORIES_KEY_VALUE
            }
        });

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should be created', () => {
        expect(spectator.component).toBeTruthy();
    });
});
