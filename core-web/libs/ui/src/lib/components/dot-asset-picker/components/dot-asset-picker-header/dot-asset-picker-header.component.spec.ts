import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAssetPickerHeaderComponent } from './dot-asset-picker-header.component';

import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';

const MESSAGES = {
    'dot.asset.picker.fullscreen.enter.aria': 'Enter full screen',
    'dot.asset.picker.fullscreen.exit.aria': 'Exit full screen',
    'dot.asset.picker.close.aria': 'Close'
};

/** Only the slice of the store the header reads. A signal, so `computed` reacts to the toggle. */
const createMockStore = () => {
    const isFullscreen = signal(false);

    return {
        isFullscreen,
        toggleFullscreen: jest.fn(() => isFullscreen.set(!isFullscreen()))
    };
};

describe('DotAssetPickerHeaderComponent', () => {
    let spectator: Spectator<DotAssetPickerHeaderComponent>;
    let store: ReturnType<typeof createMockStore>;

    const createComponent = createComponentFactory({
        component: DotAssetPickerHeaderComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }],
        detectChanges: false
    });

    const clickButton = (testId: string) => {
        const button = spectator.query(byTestId(testId))?.querySelector('button');
        spectator.click(button as HTMLElement);
    };

    beforeEach(() => {
        store = createMockStore();

        TestBed.overrideComponent(DotAssetPickerHeaderComponent, {
            add: { providers: [{ provide: DotAssetPickerStore, useValue: store }] }
        });

        spectator = createComponent({ props: { title: 'Add Image' } });
        spectator.detectChanges();
    });

    it('should render the title it is given', () => {
        expect(spectator.query(byTestId('asset-picker-title'))?.textContent?.trim()).toBe(
            'Add Image'
        );
    });

    it('should render the title the host passes on a later change', () => {
        spectator.setInput('title', 'Add File');
        spectator.detectChanges();

        expect(spectator.query(byTestId('asset-picker-title'))?.textContent?.trim()).toBe(
            'Add File'
        );
    });

    describe('fullscreen toggle', () => {
        it('should ask the store to toggle when clicked', () => {
            clickButton('asset-picker-fullscreen-btn');

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
            clickButton('asset-picker-fullscreen-btn');
            spectator.detectChanges();

            const button = spectator.query(byTestId('asset-picker-fullscreen-btn'));

            expect(button?.querySelector('i')?.textContent?.trim()).toBe('close_fullscreen');
            expect(button?.getAttribute('aria-pressed')).toBe('true');
        });
    });

    it('should emit close when the ✕ button is clicked', () => {
        const spy = jest.spyOn(spectator.component.close, 'emit');

        clickButton('asset-picker-close-btn');

        expect(spy).toHaveBeenCalledTimes(1);
    });
});
