import { expect } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotCategoryFieldSearchComponent } from './dot-category-field-search.component';

describe('DotCategoryFieldSearchComponent', () => {
    let spectator: Spectator<DotCategoryFieldSearchComponent>;

    const createComponent = createComponentFactory({
        component: DotCategoryFieldSearchComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                isLoading: false
            }
        });

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should .....', () => {
        expect(spectator.component).not.toBeNull();
    });
});
