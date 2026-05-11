import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotUveZoomControlsComponent } from './dot-uve-zoom-controls.component';

import { UVEStore } from '../../../store/dot-uve.store';

describe('DotUveZoomControlsComponent', () => {
    let spectator: Spectator<DotUveZoomControlsComponent>;
    let viewZoomLevel: ReturnType<typeof signal<number>>;
    let viewZoomSetLevel: jest.Mock;

    const createComponent = createComponentFactory({
        component: DotUveZoomControlsComponent,
        providers: [
            {
                provide: UVEStore,
                useFactory: () => ({
                    viewZoomLevel,
                    viewZoomSetLevel
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        viewZoomLevel = signal(100);
        viewZoomSetLevel = jest.fn();
    });

    describe('$zoomOptions', () => {
        it('exposes the standard presets when the current zoom matches one', () => {
            viewZoomLevel.set(150);
            spectator = createComponent();

            expect(spectator.component.$zoomOptions()).toEqual([
                { label: '50%', value: 50 },
                { label: '75%', value: 75 },
                { label: '100%', value: 100 },
                { label: '150%', value: 150 },
                { label: '200%', value: 200 }
            ]);
        });

        it('appends the current zoom (sorted) when it is off-preset', () => {
            // Simulate auto-fit from a device preset like 67%.
            viewZoomLevel.set(67);
            spectator = createComponent();

            expect(spectator.component.$zoomOptions()).toEqual([
                { label: '50%', value: 50 },
                { label: '67%', value: 67 },
                { label: '75%', value: 75 },
                { label: '100%', value: 100 },
                { label: '150%', value: 150 },
                { label: '200%', value: 200 }
            ]);
        });

        it('reacts when the zoom signal changes', () => {
            viewZoomLevel.set(100);
            spectator = createComponent();

            expect(spectator.component.$zoomOptions().some((o) => o.value === 33)).toBe(false);

            viewZoomLevel.set(33);
            expect(spectator.component.$zoomOptions().some((o) => o.value === 33)).toBe(true);
        });
    });

    describe('onZoomChange', () => {
        it('forwards the new value to viewZoomSetLevel', () => {
            spectator = createComponent();
            spectator.component.onZoomChange(150);

            expect(viewZoomSetLevel).toHaveBeenCalledWith(150);
        });
    });

    describe('$viewZoomLevelPct', () => {
        it('aliases the store signal directly (not multiplied)', () => {
            viewZoomLevel.set(175);
            spectator = createComponent();
            expect(spectator.component.$viewZoomLevelPct()).toBe(175);
        });
    });
});
