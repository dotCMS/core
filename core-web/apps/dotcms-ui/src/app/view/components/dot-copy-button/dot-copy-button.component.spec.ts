import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotClipboardUtil } from '@dotcms/app/api/util/clipboard/ClipboardUtil';
import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCopyButtonComponent } from './dot-copy-button.component';

const messageServiceMock = new MockDotMessageService({
    Copy: 'Copy',
    Copied: 'Copied'
});

describe('DotCopyButtonComponent', () => {
    let component: DotCopyButtonComponent;
    let fixture: ComponentFixture<DotCopyButtonComponent>;
    let de: DebugElement;
    let dotClipboardUtil: DotClipboardUtil;
    let button: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotCopyButtonComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                DotClipboardUtil
            ],
            imports: [UiDotIconButtonModule, TooltipModule, ButtonModule]
        }).compileComponents();
    }));

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
            button = de.query(By.css('button'));
        });

        it('should show label', () => {
            fixture.detectChanges();
            expect(button.nativeElement.textContent.trim()).toBe('Label');
        });

        it('should not show label', () => {
            component.showLabel = false;
            fixture.detectChanges();
            expect(button.nativeElement.textContent.trim()).toBe('');
        });

        it('should have pTooltip attributes', () => {
            expect(button.attributes.appendTo).toEqual('body');
            expect(button.attributes.tooltipPosition).toEqual('bottom');
            expect(button.attributes.hideDelay).toEqual('800');
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
