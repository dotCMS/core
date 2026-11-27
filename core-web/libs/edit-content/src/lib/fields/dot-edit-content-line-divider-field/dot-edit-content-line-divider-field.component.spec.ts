import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { createFakeLineDividerField } from '@dotcms/utils-testing';

import { DotEditContentLineDividerFieldComponent } from './dot-edit-content-line-divider-field.component';

describe('DotEditContentLineDividerFieldComponent', () => {
    let spectator: Spectator<DotEditContentLineDividerFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentLineDividerFieldComponent,
        detectChanges: false
    });

    it('should render the line divider bar with the field name', () => {
        const field = createFakeLineDividerField({
            name: 'Open Graph (OG Meta Tags)',
            variable: 'openGraph'
        });

        spectator = createComponent({
            props: {
                field
            }
        });
        spectator.detectChanges();

        const divider = spectator.query(byTestId('line-divider'));
        const title = spectator.query(byTestId('line-divider-title'));

        expect(divider).toBeTruthy();
        expect(divider).toHaveClass('bg-surface-100');
        expect(divider).toHaveClass('mt-10');
        // Capitals come from CSS: the text stays as the user typed it, so screen readers, copy and
        // locale-specific casing (Turkish "i", German "ß") are left to the browser.
        expect(title).toHaveClass('uppercase');
        expect(title?.textContent?.trim()).toBe('Open Graph (OG Meta Tags)');
    });

    it('should not render the line divider bar when the field name is empty', () => {
        const field = createFakeLineDividerField({
            name: '',
            variable: 'openGraph'
        });

        spectator = createComponent({
            props: {
                field
            }
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('line-divider'))).toBeNull();
        expect(spectator.query(byTestId('line-divider-title'))).toBeNull();
    });
});
