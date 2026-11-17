import { vi } from 'vitest';

import { ZIndexUtils } from 'primeng/utils';

import { hasOverlayAbove } from './overlay';

describe('hasOverlayAbove', () => {
    afterEach(() => vi.restoreAllMocks());

    const stackTop = (value: number) => vi.spyOn(ZIndexUtils, 'getCurrent').mockReturnValue(value);

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

    // Toasts and tooltips join the same stack as dialogs, under the same key, so the stack alone
    // cannot tell a notification from a modal (#37884).
    describe('with a passive layer stacked', () => {
        const rendered: HTMLElement[] = [];

        const render = (className: string, zIndex: number) => {
            const element = elementAt(zIndex);
            element.className = className;
            document.body.appendChild(element);
            rendered.push(element);

            return element;
        };

        afterEach(() => rendered.splice(0).forEach((element) => element.remove()));

        it('should report nothing above when only a toast is stacked', () => {
            stackTop(1101);
            render('p-toast p-component', 1101);

            expect(hasOverlayAbove()).toBe(false);
        });

        it('should report nothing above when only a tooltip is stacked', () => {
            stackTop(1101);
            render('p-tooltip p-component', 1101);

            expect(hasOverlayAbove()).toBe(false);
        });

        // PrimeNG puts `z-index: 2` on an input's icon. That is layout, not something open above.
        it('should not read a layout z-index as an overlay beside a toast', () => {
            stackTop(1101);
            render('p-inputicon', 2);
            render('p-toast p-component', 1101);

            expect(hasOverlayAbove()).toBe(false);
        });

        // The top of the stack is held by something not on screen, a dialog the toast does not
        // account for. Seen in the Content Drive shell suite: a toast left at 1102 by an earlier
        // test while the test under way stands in a dialog at 1101.
        it('should trust the stack when no passive layer holds its top', () => {
            stackTop(1101);
            render('p-toast p-component', 1102);

            expect(hasOverlayAbove()).toBe(true);
        });

        it('should still report a dialog underneath a toast', () => {
            stackTop(1103);
            render('p-dialog-mask', 1101);
            render('p-toast p-component', 1103);

            expect(hasOverlayAbove()).toBe(true);
        });

        it('should ignore a toast for an overlay that is itself above the toast', () => {
            stackTop(1103);
            render('p-toast p-component', 1101);

            expect(hasOverlayAbove(elementAt(1103))).toBe(false);
        });

        it('should report nothing above an overlay when only a toast is stacked on it', () => {
            stackTop(1103);
            render('p-toast p-component', 1103);

            expect(hasOverlayAbove(elementAt(1101))).toBe(false);
        });
    });
});
