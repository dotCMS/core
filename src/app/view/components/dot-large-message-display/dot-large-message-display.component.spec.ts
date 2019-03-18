import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { DotLargeMessageDisplayComponent } from './dot-large-message-display.component';
import { Injectable } from '@angular/core';
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

    clear(): void {}
}

describe('DotLargeMessageDisplayComponent', () => {
    let component: DotLargeMessageDisplayComponent;
    let fixture: ComponentFixture<DotLargeMessageDisplayComponent>;
    const dotLargeMessageDisplayServiceMock: DotLargeMessageDisplayServiceMock = new DotLargeMessageDisplayServiceMock();

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [DotDialogModule],
            declarations: [DotLargeMessageDisplayComponent],
            providers: [{ provide: DotLargeMessageDisplayService, useValue: dotLargeMessageDisplayServiceMock }]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotLargeMessageDisplayComponent);
        component = fixture.componentInstance;
        spyOn(dotLargeMessageDisplayServiceMock, 'sub').and.callThrough();
        spyOn(dotLargeMessageDisplayServiceMock, 'clear').and.callThrough();

        dotLargeMessageDisplayServiceMock.push({
            title: 'title Test',
            height: '200',
            width: '1000',
            body: 'bodyTest',
            code: { lang: 'eng', content: 'codeTest' }
        });
        fixture.detectChanges();
    });

    it('should create DotLargeMessageDisplayComponent', () => {
        expect(component).toBeTruthy();
        expect(dotLargeMessageDisplayServiceMock.sub).toHaveBeenCalled();
        expect(component.data$).not.toBe(null);
    });

    it('should close DotLargeMessageDisplayComponent', () => {
        spyOn(component, 'close').and.callThrough();
        fixture.detectChanges();
        const dialog = fixture.debugElement.query(By.css('dot-dialog'));
        dialog.triggerEventHandler('hide', {});
        fixture.detectChanges();
        expect(component.close).toHaveBeenCalled();
        expect(dotLargeMessageDisplayServiceMock.clear).toHaveBeenCalled();
    });
});
