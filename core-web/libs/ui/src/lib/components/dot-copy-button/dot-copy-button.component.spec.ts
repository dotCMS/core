import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { discardPeriodicTasks, fakeAsync, flush, tick } from '@angular/core/testing';

import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCopyButtonComponent } from './dot-copy-button.component';

import { DotClipboardUtil } from '../../services/clipboard/ClipboardUtil';

const messageServiceMock = new MockDotMessageService({
    Copy: 'Copy',
    Copied: 'Copied'
});

describe('DotCopyButtonComponent', () => {
    let spectator: Spectator<DotCopyButtonComponent>;
    let dotClipboardUtil: DotClipboardUtil;

    const createComponent = createComponentFactory({
        component: DotCopyButtonComponent,
        imports: [Tooltip, Button],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }, DotClipboardUtil]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: { copy: 'Text to copy' }
        });
        // Component provides its own DotClipboardUtil; spy on that instance so copy() is used
        dotClipboardUtil = spectator.fixture.debugElement.injector.get(DotClipboardUtil);
        jest.spyOn(dotClipboardUtil, 'copy').mockResolvedValue(true);
        spectator.detectChanges();
    });

    describe('with label', () => {
        beforeEach(() => {
            spectator.setInput('label', 'Label');
            spectator.detectChanges();
        });

        it('should show label', () => {
            const button = spectator.query(byTestId('copy-to-clipboard'));
            expect(button?.textContent?.trim()).toBe('Label');
        });

        it('should not show label', () => {
            spectator.setInput('label', null);
            spectator.detectChanges();
            const button = spectator.query(byTestId('copy-to-clipboard'));
            expect(button?.textContent?.trim()).toBe('');
        });

        it('should copy text to clipboard', fakeAsync(() => {
            const stopPropagation = jest.fn();
            const event = { stopPropagation } as unknown as MouseEvent;
            spectator.component.copyUrlToClipboard(event);
            spectator.detectChanges();
            flush(); // flush copy() promise

            expect(dotClipboardUtil.copy).toHaveBeenCalledWith('Text to copy');
            expect(stopPropagation).toHaveBeenCalledTimes(1);
        }));
    });

    describe('with tooltip', () => {
        const TOOLTIP_SHOW_DELAY = 800;

        beforeEach(() => {
            spectator.setInput('tooltipText', 'Tooltip text');
            spectator.detectChanges();
        });

        it('should show tooltip', fakeAsync(() => {
            const button = spectator.query(byTestId('copy-to-clipboard'));
            button?.dispatchEvent(new Event('mouseenter'));
            spectator.detectChanges();

            tick(TOOLTIP_SHOW_DELAY);
            spectator.detectChanges();

            const tooltipElement = document.querySelector('[data-testid="tooltip-content"]');
            expect(tooltipElement).toBeTruthy();
            expect(tooltipElement?.textContent?.trim()).toBe('Tooltip text');
            discardPeriodicTasks();
        }));

        it('should show "Copied" in tooltip after clicking the button', fakeAsync(() => {
            const event = { stopPropagation: jest.fn() } as unknown as MouseEvent;
            spectator.component.copyUrlToClipboard(event);
            tick(0); // run promise microtask so .then() runs and sets $tempTooltipText
            spectator.detectChanges();

            expect(spectator.component.$tooltipText()).toBe('Copied');
            discardPeriodicTasks();
        }));
    });
});
