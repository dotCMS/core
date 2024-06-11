import { expect, it } from '@jest/globals';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { mockProvider } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotEditContentCategoryFieldComponent } from './dot-edit-content-category-field.component';

describe('DotEditContentCategoryFieldComponent', () => {
    let spectator: Spectator<DotEditContentCategoryFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentCategoryFieldComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should have a select categories button', () => {
        expect(spectator.query(byTestId('show-dialog-btn'))).not.toBeNull();
    });

    it('should show the category list wrapper', () => {
        spectator.component.values = [];
        spectator.detectComponentChanges();
        expect(spectator.query(byTestId('category-chip-list'))).toBeNull();

        spectator.component.values = [{ id: 1, value: 'Streetwear' }];
        spectator.detectComponentChanges();
        expect(spectator.query(byTestId('category-chip-list'))).not.toBeNull();
    });
});
