import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCopyButtonComponent } from './dot-copy-button.component';
import { DotMessageService } from '@services/dot-messages-service';
import { MockDotMessageService } from 'src/app/test/dot-message-service.mock';
import { DotClipboardUtil } from 'src/app/api/util/clipboard/ClipboardUtil';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

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
            imports: [DotIconButtonModule]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotCopyButtonComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;

        component.copy = 'Text to copy';
        fixture.detectChanges();

        dotClipboardUtil = de.injector.get(DotClipboardUtil);
        button = de.query(By.css('dot-icon-button'));

        spyOn(dotClipboardUtil, 'copy').and.callFake(() => {
            return new Promise((resolve) => {
                resolve(true);
            });
        });
    });

    it('should have dot-icon-button', () => {
        expect(button.componentInstance.icon).toBe('file_copy');
    });

    it('should have tooltip', () => {
        const tooltip: DebugElement = de.query(By.css('span'));
        expect(tooltip.nativeElement.textContent).toBe('Copy');
    });

    it('should copy text to clipboard', () => {
        button.triggerEventHandler('click', {});

        expect(dotClipboardUtil.copy).toHaveBeenCalledWith('Text to copy');
    });

    it('should update tooltip text when copy', async(() => {
        const spyTooltipText = spyOnProperty(component, 'tooltipText', 'set');

        button.triggerEventHandler('click', {});

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
