import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { createFakeLineDividerField } from '@dotcms/utils-testing';

import { DotEditContentLineDividerFieldComponent } from './dot-edit-content-line-divider-field.component';

import type { InferInputSignals } from '@openng/spectator';

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
            // Keyed by the public aliases, which is what Spectator applies at runtime.
            // `InferInputSignals<C>` maps over `keyof C` — the declared member names
            // (`$field`) — so it cannot see an `alias`; renaming the keys type-checks and
            // then fails at runtime.
            props: {
                field
            } as unknown as InferInputSignals<DotEditContentLineDividerFieldComponent>
        });
        spectator.detectChanges();

        const divider = spectator.query(byTestId('line-divider'));
        const title = spectator.query(byTestId('line-divider-title'));

        expect(divider).toBeTruthy();
        expect(divider).toHaveClass('bg-surface-100');
        expect(title?.textContent?.trim()).toBe(field.name);
    });

    it('should not render the line divider bar when the field name is empty', () => {
        const field = createFakeLineDividerField({
            name: '',
            variable: 'openGraph'
        });

        spectator = createComponent({
            // Keyed by the public aliases, which is what Spectator applies at runtime.
            // `InferInputSignals<C>` maps over `keyof C` — the declared member names
            // (`$field`) — so it cannot see an `alias`; renaming the keys type-checks and
            // then fails at runtime.
            props: {
                field
            } as unknown as InferInputSignals<DotEditContentLineDividerFieldComponent>
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('line-divider'))).toBeNull();
        expect(spectator.query(byTestId('line-divider-title'))).toBeNull();
    });
});
