import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCopyButtonComponent } from './dot-copy-button.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from 'src/app/test/dot-message-service.mock';
import { DotClipboardUtil } from 'src/app/api/util/clipboard/ClipboardUtil';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { TooltipModule } from 'primeng/primeng';

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
    let label: DebugElement;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [DotCopyButtonComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                DotClipboardUtil
            ],
            imports: [DotIconButtonModule, TooltipModule]
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

    describe('with button', () => {
        beforeEach(() => {
            fixture.detectChanges();
            button = de.query(By.css('dot-icon-button'));
        });

        it('should have dot-icon-button', () => {
            expect(button.componentInstance.icon).toBe('file_copy');
        });

        it('should not have label', () => {
            label = de.query(By.css('.label'));
            expect(label).toBeNull();
        });

        it('should copy text to clipboard', () => {
            const stopPropagation = jasmine.createSpy('stopPropagation');

            button.triggerEventHandler('click', {
                stopPropagation: stopPropagation
            });

            expect(dotClipboardUtil.copy).toHaveBeenCalledWith('Text to copy');
            expect(stopPropagation).toHaveBeenCalledTimes(1);
        });

        it('should update tooltip text when copy', async(() => {
            const spyTooltipText = spyOnProperty(component, 'tooltipText', 'set');

            button.triggerEventHandler('click', {
                stopPropagation: () => {}
            });

            fixture.whenStable().then(() => {
                setTimeout(() => {
                    expect([].concat.apply([], spyTooltipText.calls.allArgs())).toEqual([
                        'Copied',
                        'Copy'
                    ]);
                }, 1000);
            });
        }));
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

        it('should not show button', () => {
            button = de.query(By.css('dot-icon-button'));
            expect(button).toBeNull();
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
