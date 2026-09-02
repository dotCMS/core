import { createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotColorIconComponent } from './dot-color-icon.component';

describe('DotColorIconComponent', () => {
    let spectator: Spectator<DotColorIconComponent>;

    const createComponent = createComponentFactory({
        component: DotColorIconComponent
    });

    const getColorCustomProperty = () =>
        spectator.element.style.getPropertyValue('--dot-color-icon-color').trim();

    describe('color input', () => {
        it('resolves a PrimeNG token to its CSS variable', () => {
            spectator = createComponent({ props: { color: 'blue' } });
            spectator.detectChanges();

            expect(getColorCustomProperty()).toBe('var(--p-blue-500)');
        });

        it('passes a 6-digit hex value through as-is', () => {
            spectator = createComponent({ props: { color: '#3b82f6' } });
            spectator.detectChanges();

            expect(getColorCustomProperty()).toBe('#3b82f6');
        });

        it('passes a 3-digit hex value through as-is', () => {
            spectator = createComponent({ props: { color: '#abc' } });
            spectator.detectChanges();

            expect(getColorCustomProperty()).toBe('#abc');
        });

        it('passes an 8-digit hex (with alpha) through as-is', () => {
            spectator = createComponent({ props: { color: '#3b82f680' } });
            spectator.detectChanges();

            expect(getColorCustomProperty()).toBe('#3b82f680');
        });

        it('treats a hex-like string without leading # as a token', () => {
            spectator = createComponent({ props: { color: '3b82f6' } });
            spectator.detectChanges();

            expect(getColorCustomProperty()).toBe('var(--p-3b82f6-500)');
        });
    });

    describe('variant attribute', () => {
        it('defaults to light variant', () => {
            spectator = createComponent({ props: { color: 'blue' } });
            spectator.detectChanges();

            expect(spectator.element.getAttribute('data-variant')).toBe('light');
        });

        it('reflects solid variant on the host element', () => {
            spectator = createComponent({ props: { color: 'blue', variant: 'solid' } });
            spectator.detectChanges();

            expect(spectator.element.getAttribute('data-variant')).toBe('solid');
        });
    });

    describe('size', () => {
        it('applies medium size classes by default', () => {
            spectator = createComponent({ props: { color: 'blue' } });
            spectator.detectChanges();

            expect(spectator.element.classList).toContain('w-14');
            expect(spectator.element.classList).toContain('h-14');
        });

        it('applies small size classes when size is sm', () => {
            spectator = createComponent({ props: { color: 'blue', size: 'sm' } });
            spectator.detectChanges();

            expect(spectator.element.classList).toContain('w-12');
            expect(spectator.element.classList).toContain('h-12');
        });
    });
});
