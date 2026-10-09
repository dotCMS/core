import { createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { Component, signal } from '@angular/core';

import { Drawer } from 'primeng/drawer';

import { injectSidePanelChrome } from './dot-side-panel-chrome';
import { DotSidePanelNavController } from './dot-side-panel-nav.service';

const EXPANDED_STORAGE_KEY = 'dot-edit-content-side-panel-expanded';

/** A side panel reduced to what the chrome needs: a drawer with a mask, and a close intent. */
@Component({
    selector: 'dot-test-side-panel',
    template: ''
})
class TestSidePanelComponent {
    readonly mask = document.createElement('div');
    readonly drawer = signal({ mask: this.mask, container: null } as unknown as Drawer);
    readonly requestClose = vi.fn();
    readonly chrome = injectSidePanelChrome({
        panel: this,
        drawer: this.drawer,
        requestClose: () => this.requestClose(),
        escapeLabel: 'test.side-panel.close'
    });
}

describe('injectSidePanelChrome', () => {
    let spectator: Spectator<TestSidePanelComponent>;
    let navController: { acquire: Mock; release: Mock; isTop: Mock };

    const createComponent = createComponentFactory({
        component: TestSidePanelComponent,
        // Read per creation, so each test's controller reaches the panel.
        providers: [{ provide: DotSidePanelNavController, useFactory: () => navController }],
        detectChanges: false
    });

    beforeEach(() => {
        localStorage.clear();
        navController = {
            acquire: vi.fn(),
            release: vi.fn(),
            isTop: vi.fn().mockReturnValue(true)
        };
        spectator = createComponent();
        spectator.detectChanges();
    });

    const pressEscape = (): KeyboardEvent => {
        const event = new KeyboardEvent('keydown', {
            key: 'Escape',
            bubbles: true,
            cancelable: true
        });
        document.dispatchEvent(event);

        return event;
    };

    const clickOn = (element: HTMLElement): void => {
        document.body.appendChild(element);
        element.dispatchEvent(new MouseEvent('click', { bubbles: true }));
        element.remove();
    };

    describe('Escape', () => {
        it('asks the frontmost panel to close, and consumes the key', () => {
            const event = pressEscape();

            expect(spectator.component.requestClose).toHaveBeenCalledTimes(1);
            expect(event.defaultPrevented).toBe(true);
        });

        it('consumes the key but leaves a panel that is not frontmost open', () => {
            navController.isTop.mockReturnValue(false);

            const event = pressEscape();

            expect(spectator.component.requestClose).not.toHaveBeenCalled();
            expect(event.defaultPrevented).toBe(true);
        });

        it('is the same handling when a panel forwards an Escape the registry cannot see', () => {
            expect(spectator.component.chrome.escape()).toBe(true);
            expect(spectator.component.requestClose).toHaveBeenCalledTimes(1);
        });
    });

    describe('mask click', () => {
        it('asks the panel to close on a click on its own mask', () => {
            clickOn(spectator.component.mask);

            expect(spectator.component.requestClose).toHaveBeenCalledTimes(1);
        });

        it("ignores a click on another drawer's mask", () => {
            const foreignMask = document.createElement('div');
            foreignMask.classList.add('p-drawer-mask');

            clickOn(foreignMask);

            expect(spectator.component.requestClose).not.toHaveBeenCalled();
        });

        it('ignores a click on its mask when another panel is in front', () => {
            navController.isTop.mockReturnValue(false);

            clickOn(spectator.component.mask);

            expect(spectator.component.requestClose).not.toHaveBeenCalled();
        });
    });

    describe('side-panel stack', () => {
        it('joins the stack after the first render', () => {
            expect(navController.acquire).toHaveBeenCalledWith(spectator.component);
        });

        it('leaves the stack, and gives Escape and the mask back, when the panel is destroyed', () => {
            const { component } = spectator;

            spectator.fixture.destroy();

            expect(navController.release).toHaveBeenCalledWith(component);
            expect(pressEscape().defaultPrevented).toBe(false);
            clickOn(component.mask);
            expect(component.requestClose).not.toHaveBeenCalled();
        });
    });

    describe('full width', () => {
        it('starts narrow when nothing is stored', () => {
            expect(spectator.component.chrome.expanded()).toBe(false);
        });

        it('remembers the choice for the next panel', () => {
            spectator.component.chrome.toggleExpanded();

            expect(spectator.component.chrome.expanded()).toBe(true);
            expect(localStorage.getItem(EXPANDED_STORAGE_KEY)).toBe('true');

            expect(createComponent().component.chrome.expanded()).toBe(true);
        });
    });
});
