import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotFilterListItemComponent } from './dot-filter-list-item.component';

describe('DotFilterListItemComponent', () => {
    let spectator: Spectator<DotFilterListItemComponent>;

    const createComponent = createComponentFactory({
        component: DotFilterListItemComponent
    });

    beforeEach(() => {
        spectator = createComponent({ props: { label: 'English' } });
    });

    it('should render the label', () => {
        expect(spectator.element.textContent?.trim()).toBe('English');
    });

    it('should render the secondary text in parentheses when provided', () => {
        spectator.setInput('secondary', 'en-US');
        expect(spectator.element.textContent?.trim()).toContain('English');
        expect(spectator.element.textContent?.trim()).toContain('(en-US)');
    });

    it('should not render parentheses when secondary is not provided', () => {
        expect(spectator.element.textContent?.trim()).toBe('English');
    });
});
