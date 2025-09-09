import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotDialogModule } from '@dotcms/ui';
import { DotcmsEventsServiceMock } from '@dotcms/utils-testing';

import { DotLargeMessageDisplayComponent } from './dot-large-message-display.component';

import { DotParseHtmlService } from '../../../api/services/dot-parse-html/dot-parse-html.service';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-large-message-display></dot-large-message-display>
    `,
    standalone: false
})
class TestHostComponent {}

describe('DotLargeMessageDisplayComponent', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let dialog: DebugElement;
    let dotcmsEventsServiceMock;

    beforeEach(waitForAsync(() =>
        TestBed.configureTestingModule({
            imports: [DotDialogModule],
            declarations: [DotLargeMessageDisplayComponent, TestHostComponent],
            providers: [
                {
                    provide: DotcmsEventsService,
                    useClass: DotcmsEventsServiceMock
                },
                DotParseHtmlService
            ]
        }).compileComponents()));

    beforeEach(() => {
        fixture = TestBed.createComponent(TestHostComponent);
        dotcmsEventsServiceMock = fixture.debugElement.injector.get(DotcmsEventsService);
        jest.spyOn(dotcmsEventsServiceMock, 'subscribeTo');

        fixture.detectChanges();
    });

    it('should create DotLargeMessageDisplayComponent', (done) => {
        dotcmsEventsServiceMock.triggerSubscribeTo('LARGE_MESSAGE', {
            title: 'title Test',
            height: '200',
            width: '1000',
            body: 'Hello World',
            code: { lang: 'eng', content: 'codeTest' }
        });

        fixture.detectChanges();
        dialog = fixture.debugElement.query(By.css('dot-dialog'));

        const bodyElem = fixture.debugElement.query(By.css('.dialog-message__body'));
        const codeElem = fixture.debugElement.query(By.css('.dialog-message__code'));
        expect(dialog.componentInstance.visible).toBe(true);
        expect(dialog.componentInstance.header).toBe('title Test');
        expect(dialog.componentInstance.width).toBe('1000');
        expect(dialog.componentInstance.height).toBe('200');
        expect(codeElem.nativeElement.innerHTML.trim()).toBe('codeTest');
        expect(dotcmsEventsServiceMock.subscribeTo).toHaveBeenCalledTimes(1);

        setTimeout(() => {
            expect(bodyElem.nativeElement.innerHTML.trim()).toBe('Hello World');
            done();
        }, 0);
    });

    it('should render script tag from body', (done) => {
        dotcmsEventsServiceMock.triggerSubscribeTo('LARGE_MESSAGE', {
            title: 'title Test',
            body: '<h1>Hello World</h1><script>console.log("abc")</script>'
        });
        fixture.detectChanges();

        setTimeout(() => {
            const bodyElem = fixture.debugElement.query(By.css('.dialog-message__body'));
            const h1 = bodyElem.nativeElement.querySelector('h1');
            const script = bodyElem.nativeElement.querySelector('script');
            expect(h1.innerText).toBe('Hello World');
            expect(script.getAttribute('type')).toBe('text/javascript');
            expect(script.innerHTML).toBe('console.log("abc")');
            done();
        }, 0);
    });

    it('should render script tag from script property', (done) => {
        dotcmsEventsServiceMock.triggerSubscribeTo('LARGE_MESSAGE', {
            title: 'title Test',
            body: '<h1>Hello World</h1><script>console.log("abc")</script>',
            script: 'console.log("script from prop")'
        });
        fixture.detectChanges();

        setTimeout(() => {
            const bodyElem = fixture.debugElement.query(By.css('.dialog-message__body'));
            const scripts = bodyElem.nativeElement.querySelectorAll('script');
            expect(scripts.length).toBe(2);
            scripts.forEach((script, index) => {
                expect(script.getAttribute('type')).toBe('text/javascript');
                expect(script.innerHTML).toBe(
                    index ? 'console.log("script from prop")' : 'console.log("abc")'
                );
            });
            done();
        }, 0);
    });

    it('should remove dialog when it is close', () => {
        dotcmsEventsServiceMock.triggerSubscribeTo('LARGE_MESSAGE', {
            title: 'title Test',
            body: '<h1>Hello World</h1><script>console.log("abc")</script>',
            script: 'console.log("script from prop")'
        });
        fixture.detectChanges();

        dialog = fixture.debugElement.query(By.css('dot-dialog'));
        dialog.triggerEventHandler('hide', {});

        fixture.detectChanges();
        expect(fixture.debugElement.query(By.css('dot-dialog'))).toBeNull();
    });

    it('should set default height and width', () => {
        dotcmsEventsServiceMock.triggerSubscribeTo('LARGE_MESSAGE', {
            title: 'title Test',
            body: 'bodyTest',
            code: { lang: 'eng', content: 'codeTest' }
        });
        fixture.detectChanges();
        dialog = fixture.debugElement.query(By.css('dot-dialog'));

        expect(dialog.componentInstance.width).toBe('500px');
        expect(dialog.componentInstance.height).toBe('400px');
    });

    it('should show two dialogs', () => {
        dotcmsEventsServiceMock.triggerSubscribeTo('LARGE_MESSAGE', {
            title: 'title Test',
            body: 'bodyTest',
            code: { lang: 'eng', content: 'codeTest' }
        });
        fixture.detectChanges();

        expect(fixture.debugElement.queryAll(By.css('dot-dialog')).length).toBe(1);

        dotcmsEventsServiceMock.triggerSubscribeTo('LARGE_MESSAGE', {
            title: 'title Test 2',
            body: 'bodyTest 2',
            code: { lang: 'eng', content: 'codeTest 2' }
        });
        fixture.detectChanges();

        expect(fixture.debugElement.queryAll(By.css('dot-dialog')).length).toBe(2);
    });
});
