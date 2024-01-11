import { of } from 'rxjs';

import { Component, DebugElement, ElementRef, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { IframeComponent } from '@components/_common/iframe/iframe-component';
import { DotEditPageService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { DotDOMHtmlUtilService } from '@portlets/dot-edit-page/content/services/html/dot-dom-html-util.service';

import { DotWhatsChangedComponent, SHOW_DIFF_STYLES } from './dot-whats-changed.component';

@Component({
    selector: 'dot-test',
    template: '<dot-whats-changed [pageId]="pageId" [languageId]="languageId"></dot-whats-changed>'
})
class TestHostComponent {
    languageId: string;
    pageId: string;
}

@Component({
    selector: 'dot-iframe',
    template: '<iframe #iframeElement></iframe>'
})
class TestDotIframeComponent {
    @ViewChild('iframeElement') iframeElement: ElementRef;
}

describe('DotWhatsChangedComponent', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dotIframe: IframeComponent;
    let dotEditPageService: DotEditPageService;
    let dotDOMHtmlUtilService: DotDOMHtmlUtilService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotWhatsChangedComponent, TestDotIframeComponent, TestHostComponent],
            providers: [
                {
                    provide: DotEditPageService,
                    useValue: {
                        whatChange: jasmine
                            .createSpy()
                            .and.returnValue(
                                of({ diff: true, renderLive: 'ABC', renderWorking: 'ABC DEF' })
                            )
                    }
                },
                {
                    provide: DotDOMHtmlUtilService,
                    useValue: {
                        createStyleElement: jasmine
                            .createSpy()
                            .and.returnValue(document.createElement('style'))
                    }
                },
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle: jasmine.createSpy()
                    }
                }
            ],
            imports: [DotMessagePipe]
        });

        fixture = TestBed.createComponent(TestHostComponent);

        de = fixture.debugElement.query(By.css('dot-whats-changed'));
        dotEditPageService = TestBed.inject(DotEditPageService);
        dotDOMHtmlUtilService = TestBed.inject(DotDOMHtmlUtilService);
        fixture.detectChanges();
        dotIframe = de.query(By.css('dot-iframe')).componentInstance;

        fixture.componentInstance.pageId = '123';
        fixture.componentInstance.languageId = '1';
        fixture.detectChanges();
    });

    it('should load content based on the pageId and URL', () => {
        expect(dotDOMHtmlUtilService.createStyleElement).toHaveBeenCalledOnceWith(SHOW_DIFF_STYLES);
        expect(dotEditPageService.whatChange).toHaveBeenCalledWith('123', '1');
        expect(dotIframe.iframeElement.nativeElement.contentDocument.body.innerHTML).toContain(
            'ABC<ins class="diffins">&nbsp;DEF</ins>'
        );
    });

    it('should load content when languageId is change', () => {
        fixture.componentInstance.languageId = '2';
        fixture.detectChanges();

        expect(dotEditPageService.whatChange).toHaveBeenCalledWith('123', '2');
    });

    it('should load content when pageId is change', () => {
        fixture.componentInstance.pageId = 'abc-123';
        fixture.detectChanges();

        expect(dotEditPageService.whatChange).toHaveBeenCalledWith('abc-123', '1');
    });
});
