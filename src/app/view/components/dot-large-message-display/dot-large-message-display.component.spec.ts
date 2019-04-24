import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { DotLargeMessageDisplayComponent } from './dot-large-message-display.component';
import { Injectable, DebugElement } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import {
    DotLargeMessageDisplayParams,
    DotLargeMessageDisplayService
} from './services/dot-large-message-display.service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { By } from '@angular/platform-browser';

@Injectable()
export class DotLargeMessageDisplayServiceMock {
    _messages: BehaviorSubject<DotLargeMessageDisplayParams> = new BehaviorSubject(null);

    sub(): Observable<DotLargeMessageDisplayParams> {
        return this._messages.asObservable();
    }

    push(message: DotLargeMessageDisplayParams): void {
        this._messages.next(message);
    }

    clear(): void {
        this._messages.next(null);
    }
}

describe('DotLargeMessageDisplayComponent', () => {
    let fixture: ComponentFixture<DotLargeMessageDisplayComponent>;
    let dialog: DebugElement;
    const dotLargeMessageDisplayServiceMock: DotLargeMessageDisplayServiceMock = new DotLargeMessageDisplayServiceMock();

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [DotDialogModule],
            declarations: [DotLargeMessageDisplayComponent],
            providers: [
                {
                    provide: DotLargeMessageDisplayService,
                    useValue: dotLargeMessageDisplayServiceMock
                }
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotLargeMessageDisplayComponent);
        spyOn(dotLargeMessageDisplayServiceMock, 'sub').and.callThrough();
        spyOn(dotLargeMessageDisplayServiceMock, 'clear').and.callThrough();
    });

    it('should create DotLargeMessageDisplayComponent', (done) => {
        dotLargeMessageDisplayServiceMock.push({
            title: 'title Test',
            height: '200',
            width: '1000',
            body: '<h1>Hello World</h1>',
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
        expect(dotLargeMessageDisplayServiceMock.sub).toHaveBeenCalledTimes(1);

        setTimeout(() => {
            expect(bodyElem.nativeElement.innerHTML.trim()).toBe('<h1>Hello World</h1>');
            done();
        }, 0);
    });

    it('should render script tag from body', (done) => {
        dotLargeMessageDisplayServiceMock.push({
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
        dotLargeMessageDisplayServiceMock.push({
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

    it('should clear the DotLargeMessageDisplayService on dialog hide', () => {
        dialog.triggerEventHandler('hide', {});
        expect(dotLargeMessageDisplayServiceMock.clear).toHaveBeenCalled();
    });

    it('should set default height and width', () => {
        dotLargeMessageDisplayServiceMock.push({
            title: 'title Test',
            body: 'bodyTest',
            code: { lang: 'eng', content: 'codeTest' }
        });
        fixture.detectChanges();
        dialog = fixture.debugElement.query(By.css('dot-dialog'));

        expect(dialog.componentInstance.width).toBe('500px');
        expect(dialog.componentInstance.height).toBe('400px');
    });
});
