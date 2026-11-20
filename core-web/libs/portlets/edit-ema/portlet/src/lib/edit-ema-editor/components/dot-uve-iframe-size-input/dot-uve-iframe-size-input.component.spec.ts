import { Spectator, byTestId, createComponentFactory } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { WritableSignal, signal } from '@angular/core';

import { DotUveIframeSizeInputComponent } from './dot-uve-iframe-size-input.component';

import { UVEStore } from '../../../store/dot-uve.store';

describe('DotUveIframeSizeInputComponent', () => {
    let spectator: Spectator<DotUveIframeSizeInputComponent>;
    let viewIframeWidth: WritableSignal<number>;
    let viewIframeHeight: WritableSignal<number>;
    let viewResizeIframe: Mock;

    const createComponent = createComponentFactory({
        component: DotUveIframeSizeInputComponent,
        providers: [
            {
                provide: UVEStore,
                useFactory: () => ({
                    viewIframeWidth,
                    viewIframeHeight,
                    viewResizeIframe
                })
            }
        ],
        detectChanges: false
    });

    /** Types a value and commits it, as the browser does on Enter or when the field loses focus. */
    const commit = (testId: string, value: string) => {
        spectator.typeInElement(value, byTestId(testId));
        spectator.dispatchFakeEvent(byTestId(testId), 'change');
    };

    beforeEach(() => {
        viewIframeWidth = signal(1200);
        viewIframeHeight = signal(800);
        viewResizeIframe = vi.fn();
        spectator = createComponent();
        spectator.detectChanges();
    });

    it('should show the iframe width and height from the store', () => {
        expect(spectator.query(byTestId('iframe-size-width'))).toHaveValue('1200');
        expect(spectator.query(byTestId('iframe-size-height'))).toHaveValue('800');
    });

    it('should resize the iframe width through the store when a width is committed', () => {
        commit('iframe-size-width', '375');

        expect(viewResizeIframe).toHaveBeenCalledWith({ width: 375 });
    });

    it('should resize the iframe height through the store when a height is committed', () => {
        commit('iframe-size-height', '667');

        expect(viewResizeIframe).toHaveBeenCalledWith({ height: 667 });
    });

    it.each([
        { field: 'width', value: '0' },
        { field: 'width', value: '-10' },
        { field: 'width', value: '' },
        { field: 'height', value: '0' },
        { field: 'height', value: '-10' },
        { field: 'height', value: '' }
    ])('should not resize the iframe for a $field of "$value"', ({ field, value }) => {
        commit(`iframe-size-${field}`, value);

        expect(viewResizeIframe).not.toHaveBeenCalled();
    });
});
