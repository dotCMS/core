import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement, Component, ViewChild, ElementRef } from '@angular/core';
import { DotWhatsChangedComponent, SHOW_DIFF_STYLES } from './dot-whats-changed.component';
import { IframeComponent } from '@components/_common/iframe/iframe-component';
import { DotEditPageService } from '@services/dot-edit-page/dot-edit-page.service';
import { of } from 'rxjs';
import { DotDOMHtmlUtilService } from '@portlets/dot-edit-page/content/services/html/dot-dom-html-util.service';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

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
    let component: DotWhatsChangedComponent;
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
            imports: [DotMessagePipeModule]
        });

        fixture = TestBed.createComponent(TestHostComponent);

        de = fixture.debugElement.query(By.css('dot-whats-changed'));
        component = de.componentInstance;
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
