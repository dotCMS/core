import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCopyLinkComponent } from './dot-copy-link.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@dotcms/app/test/dot-message-service.mock';
import { DotClipboardUtil } from '@dotcms/app/api/util/clipboard/ClipboardUtil';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { TooltipModule } from 'primeng/tooltip';
import { DotIconModule } from '@dotcms/ui';

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

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotCopyLinkComponent],
                providers: [
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    },
                    DotClipboardUtil
                ],
                imports: [UiDotIconButtonModule, TooltipModule, DotIconModule]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotCopyLinkComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;

        dotClipboardUtil = de.injector.get(DotClipboardUtil);

        spyOn(dotClipboardUtil, 'copy').and.callFake(() => {
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
            expect(button.nativeElement.textContent).toBe('Label content_copy');
        });

        it('should show copy icon', () => {
            const icon = de.query(By.css('[data-testId="icon"]'));
            expect(icon).not.toBeNull()
        });

        it('should have pTooltip attributes', () => {
            expect(button.attributes.appendTo).toEqual('body');
            expect(button.attributes.tooltipPosition).toEqual('bottom');
            expect(button.attributes.hideDelay).toEqual('300');
        });

        it('should copy text to clipboard', () => {
            const stopPropagation = jasmine.createSpy('stopPropagation');

            button.triggerEventHandler('click', {
                stopPropagation: stopPropagation
            });

            expect(dotClipboardUtil.copy).toHaveBeenCalledWith('Text to copy');
            expect(stopPropagation).toHaveBeenCalledTimes(1);
        });
    });
});
