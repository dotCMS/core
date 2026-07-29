import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

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
        expect(divider).toHaveClass('bg-surface-50', 'border-surface-200');
        expect(title?.textContent?.trim()).toBe(field.name);
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
