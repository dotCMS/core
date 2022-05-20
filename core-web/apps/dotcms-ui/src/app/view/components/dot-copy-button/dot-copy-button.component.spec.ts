import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCopyButtonComponent } from './dot-copy-button.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@dotcms/app/test/dot-message-service.mock';
import { DotClipboardUtil } from '@dotcms/app/api/util/clipboard/ClipboardUtil';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { TooltipModule } from 'primeng/tooltip';

const messageServiceMock = new MockDotMessageService({
    Copy: 'Copy',
    Copied: 'Copied'
});

describe('DotCopyButtonComponent', () => {
    let component: DotCopyButtonComponent;
    let fixture: ComponentFixture<DotCopyButtonComponent>;
    let de: DebugElement;
    let dotClipboardUtil: DotClipboardUtil;
    let label: DebugElement;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotCopyButtonComponent],
                providers: [
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    },
                    DotClipboardUtil
                ],
                imports: [UiDotIconButtonModule, TooltipModule]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotCopyButtonComponent);
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
            label = de.query(By.css('.label'));
        });

        it('should show label', () => {
            expect(label.nativeElement.textContent).toBe('Label');
        });

        it('should have pTooltip attributes', () => {
            expect(label.attributes.appendTo).toEqual('body');
            expect(label.attributes.tooltipPosition).toEqual('bottom');
            expect(label.attributes.hideDelay).toEqual('800');
        });

        it('should copy text to clipboard', () => {
            const stopPropagation = jasmine.createSpy('stopPropagation');

            label.triggerEventHandler('click', {
                stopPropagation: stopPropagation
            });

            expect(dotClipboardUtil.copy).toHaveBeenCalledWith('Text to copy');
            expect(stopPropagation).toHaveBeenCalledTimes(1);
        });
    });
});
