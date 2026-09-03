import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAssetPickerFullscreenToggleComponent } from './dot-asset-picker-fullscreen-toggle.component';

import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';

const MESSAGES = {
    'dot.asset.picker.fullscreen.enter.aria': 'Enter full screen',
    'dot.asset.picker.fullscreen.exit.aria': 'Exit full screen'
};

/** Only the slice of the store the toggle reads. A signal, so `computed` reacts to the toggle. */
const createMockStore = () => {
    const isFullscreen = signal(false);

    return {
        isFullscreen,
        toggleFullscreen: jest.fn(() => isFullscreen.set(!isFullscreen()))
    };
};

describe('DotAssetPickerFullscreenToggleComponent', () => {
    let spectator: Spectator<DotAssetPickerFullscreenToggleComponent>;
    let store: ReturnType<typeof createMockStore>;

    const createComponent = createComponentFactory({
        component: DotAssetPickerFullscreenToggleComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }],
        detectChanges: false
    });

    const clickToggle = () => {
        const button = spectator
            .query(byTestId('asset-picker-fullscreen-btn'))
            ?.querySelector('button');
        spectator.click(button as HTMLElement);
    };

    beforeEach(() => {
        store = createMockStore();

        TestBed.overrideComponent(DotAssetPickerFullscreenToggleComponent, {
            add: { providers: [{ provide: DotAssetPickerStore, useValue: store }] }
        });

        spectator = createComponent();
        spectator.detectChanges();
    });

    it('should ask the store to toggle when clicked', () => {
        clickToggle();

        expect(store.toggleFullscreen).toHaveBeenCalledTimes(1);
    });

    // `[attr.aria-pressed]` is bound on `<p-button>`, so it lands on that host element — the
    // same element carrying the testid — not on the inner `<button>` PrimeNG renders.
    it('should show the "enter" icon and aria state while windowed', () => {
        const button = spectator.query(byTestId('asset-picker-fullscreen-btn'));

        expect(button?.querySelector('i')?.textContent?.trim()).toBe('open_in_full');
        expect(button?.getAttribute('aria-pressed')).toBe('false');
    });

    it('should swap to the "exit" icon and aria state once fullscreen', () => {
        clickToggle();
        spectator.detectChanges();

        const button = spectator.query(byTestId('asset-picker-fullscreen-btn'));

        expect(button?.querySelector('i')?.textContent?.trim()).toBe('close_fullscreen');
        expect(button?.getAttribute('aria-pressed')).toBe('true');
    });
});
