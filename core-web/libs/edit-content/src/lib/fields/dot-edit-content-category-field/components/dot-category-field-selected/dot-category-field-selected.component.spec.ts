import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/utils-testing';

import { DotCategoryFieldSelectedComponent } from './dot-category-field-selected.component';

import { CATEGORY_MOCK_TRANSFORMED } from '../../mocks/category-field.mocks';

describe('DotCategoryFieldSelectedComponent', () => {
    let spectator: Spectator<DotCategoryFieldSelectedComponent>;
    const createComponent = createComponentFactory({
        component: DotCategoryFieldSelectedComponent,
        imports: [DotMessagePipe],
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('categories', CATEGORY_MOCK_TRANSFORMED);
        spectator.detectChanges();
    });

    it('should render the list of categories', () => {
        expect(spectator.queryAll(byTestId('category-item')).length).toBe(
            CATEGORY_MOCK_TRANSFORMED.length
        );
    });

    it('should display category name and path', () => {
        const items = spectator.queryAll(byTestId('category-item'));

        items.forEach((item, index) => {
            const title = item.querySelector('[data-testId="category-title"]');
            const path = item.querySelector('[data-testId="category-path"]');

            expect(title).toContainText(CATEGORY_MOCK_TRANSFORMED[index].value);
            expect(path?.getAttribute('ng-reflect-text')).toBe(
                CATEGORY_MOCK_TRANSFORMED[index].path
            );
        });
    });

    it('should display remove button', () => {
        const buttons = spectator.queryAll(byTestId('category-remove-btn'));
        expect(buttons.length).toBe(CATEGORY_MOCK_TRANSFORMED.length);
    });

    it('should emit an event when remove button is clicked', () => {
        const removeSpy = jest.spyOn(spectator.component.removeItem, 'emit');
        const button = spectator.query(byTestId('category-remove-btn'));
        spectator.click(button);
        expect(removeSpy).toHaveBeenCalledWith(CATEGORY_MOCK_TRANSFORMED[0].key);
    });
});
