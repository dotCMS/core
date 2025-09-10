import { DebugElement } from '@angular/core';
import {
    ComponentFixture,
    discardPeriodicTasks,
    fakeAsync,
    flush,
    TestBed,
    tick,
    waitForAsync
} from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCopyButtonComponent } from './dot-copy-button.component';

import { DotClipboardUtil } from '../../services/clipboard/ClipboardUtil';

const messageServiceMock = new MockDotMessageService({
    Copy: 'Copy',
    Copied: 'Copied'
});

describe('DotCopyButtonComponent', () => {
    let fixture: ComponentFixture<DotCopyButtonComponent>;
    let de: DebugElement;
    let dotClipboardUtil: DotClipboardUtil;
    let button: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                DotClipboardUtil
            ],
            imports: [TooltipModule, ButtonModule, DotCopyButtonComponent]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotCopyButtonComponent);
        de = fixture.debugElement;
        dotClipboardUtil = de.injector.get(DotClipboardUtil);

        jest.spyOn(dotClipboardUtil, 'copy').mockImplementation(() => {
            return Promise.resolve(true);
        });
        fixture.componentRef.setInput('copy', 'Text to copy');
    });

    describe('with label', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('label', 'Label');
            button = de.query(By.css('button'));
        });

        it('should show label', () => {
            fixture.detectChanges();
            expect(button.nativeElement.textContent.trim()).toBe('Label');
        });

        it('should not show label', () => {
            fixture.componentRef.setInput('label', null);
            fixture.detectChanges();
            expect(button.nativeElement.textContent.trim()).toBe('');
        });

        it('should have pTooltip attributes', () => {
            expect(button.attributes.appendTo).toEqual('body');
            expect(button.attributes.tooltipPosition).toEqual('bottom');
            expect(button.attributes.hideDelay).toEqual('800');
        });

        it('should copy text to clipboard', () => {
            const stopPropagation = jest.fn();

            button.triggerEventHandler('click', {
                stopPropagation: stopPropagation
            });

            expect(dotClipboardUtil.copy).toHaveBeenCalledWith('Text to copy');
            expect(stopPropagation).toHaveBeenCalledTimes(1);
        });
    });

    describe('with tooltip', () => {
        beforeEach(() => {
            fixture.componentRef.setInput('tooltipText', 'Tooltip text');
            button = de.query(By.css('button'));
            fixture.detectChanges();
        });

        it('should show tooltip', fakeAsync(() => {
            const nativeButton = button.nativeElement;
            nativeButton.dispatchEvent(new Event('mouseenter'));
            fixture.detectChanges();

            tick(100);
            fixture.detectChanges();

            const tooltipElement = document.querySelector('[data-testid="tooltip-content"]');
            expect(tooltipElement).toBeTruthy();
            expect(tooltipElement.textContent.trim()).toBe('Tooltip text');
            discardPeriodicTasks();
        }));

        it('should show "Copied" in tooltip after clicking the button', fakeAsync(() => {
            const nativeButton = button.nativeElement;

            nativeButton.dispatchEvent(new Event('mouseenter'));
            fixture.detectChanges();
            tick(100);
            fixture.detectChanges();

            nativeButton.dispatchEvent(new Event('click'));
            fixture.detectChanges();
            tick(100);
            fixture.detectChanges();

            const tooltipElement = document.querySelector('[data-testid="tooltip-content"]');
            expect(tooltipElement).toBeTruthy();
            expect(tooltipElement.textContent.trim()).toBe('Copied');
            flush();
        }));
    });
});
