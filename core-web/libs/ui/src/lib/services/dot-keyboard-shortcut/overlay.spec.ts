import { vi } from 'vitest';

import { ZIndexUtils } from 'primeng/utils';

import { hasOverlayAbove } from './overlay';

describe('hasOverlayAbove', () => {
    afterEach(() => vi.restoreAllMocks());

    const stackTop = (value: number) =>
        vi.spyOn(ZIndexUtils, 'getCurrent').mockReturnValue(value);

    const elementAt = (zIndex: number) => {
        const element = document.createElement('div');
        element.style.zIndex = String(zIndex);

        return element;
    };

    describe('for a base-layer surface (no container)', () => {
        it('should report nothing above when the overlay stack is empty', () => {
            stackTop(0);

            expect(hasOverlayAbove()).toBe(false);
        });

        it('should report an overlay above as soon as one is open', () => {
            stackTop(1101);

            expect(hasOverlayAbove()).toBe(true);
        });
    });

    describe('for a surface that is itself an overlay', () => {
        it('should report nothing above when it is the topmost overlay', () => {
            stackTop(1101);

            expect(hasOverlayAbove(elementAt(1101))).toBe(false);
        });

        it('should report an overlay above when something is stacked on it', () => {
            stackTop(1102);

            expect(hasOverlayAbove(elementAt(1101))).toBe(true);
        });

        it('should report nothing above when it outranks the stack', () => {
            stackTop(1000);

            expect(hasOverlayAbove(elementAt(1101))).toBe(false);
        });

        // The element comes from a viewChild, which is undefined before the first render.
        it('should treat a missing container as the base layer', () => {
            stackTop(0);

            expect(hasOverlayAbove(null)).toBe(false);
        });
    });
});
