import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotClipboardUtil, DotIconModule } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCopyLinkComponent } from './dot-copy-link.component';

const messageServiceMock = new MockDotMessageService({
    Copy: 'Copy',
    Copied: 'Copied'
});

describe('DotCopyLinkComponent', () => {
    let component: DotCopyLinkComponent;
    let fixture: ComponentFixture<DotCopyLinkComponent>;
    let de: DebugElement;
    let dotClipboardUtil: DotClipboardUtil;
    let button: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotCopyLinkComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                DotClipboardUtil
            ],
            imports: [ButtonModule, TooltipModule, DotIconModule]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotCopyLinkComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;

        dotClipboardUtil = de.injector.get(DotClipboardUtil);

        jest.spyOn(dotClipboardUtil, 'copy').mockImplementation(() => {
            return new Promise((resolve) => {
                resolve(true);
            });
        });

        component.copy = 'Text to copy';
    });

    describe('with label', () => {
        beforeEach(() => {
            component.label = 'Label';
            fixture.detectChanges();
            button = de.query(By.css('[data-testId="button"]'));
        });

        it('should show label', () => {
            expect(button.nativeElement.textContent.trim()).toBe('Label');
        });

        it('should show copy icon', () => {
            const icon = de.query(By.css('[data-testId="icon"]'));
            expect(icon).not.toBeNull();
        });

        it('should have pTooltip attributes', () => {
            expect(button.attributes.appendTo).toEqual('body');
            expect(button.attributes.tooltipPosition).toEqual('bottom');
            expect(button.attributes.hideDelay).toEqual('300');
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
});
