import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { DotDialogMessageComponent } from './dot-dialog-message.component';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import {
    DotDialogMessageParams,
    DotDialogMessageService
} from './services/dot-dialog-message.service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { By } from '@angular/platform-browser';

@Injectable()
export class DotDialogMessageServiceMock {
    _messages: BehaviorSubject<DotDialogMessageParams> = new BehaviorSubject(null);

    sub(): Observable<DotDialogMessageParams> {
        return this._messages.asObservable();
    }

    push(message: DotDialogMessageParams): void {
        this._messages.next(message);
    }
}

describe('DotDialogMessageComponent', () => {
    let component: DotDialogMessageComponent;
    let fixture: ComponentFixture<DotDialogMessageComponent>;
    const dotDialogMessageServiceMock: DotDialogMessageServiceMock = new DotDialogMessageServiceMock();

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [DotDialogModule],
            declarations: [DotDialogMessageComponent],
            providers: [{ provide: DotDialogMessageService, useValue: dotDialogMessageServiceMock }]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotDialogMessageComponent);
        component = fixture.componentInstance;
        spyOn(dotDialogMessageServiceMock, 'sub').and.callThrough();
        spyOn(dotDialogMessageServiceMock, 'push').and.callThrough();

        dotDialogMessageServiceMock.push({
            title: 'title Test',
            height: '200',
            width: '1000',
            body: 'bodyTest',
            code: { lang: 'eng', content: 'codeTest' }
        });
        fixture.detectChanges();
    });

    it('should create DotDialogMessageComponent', () => {
        expect(component).toBeTruthy();
        expect(dotDialogMessageServiceMock.sub).toHaveBeenCalled();
        expect(component.data$).not.toBe(null);
    });

    it('should close DotDialogMessageComponent', () => {
        spyOn(component, 'close').and.callThrough();
        fixture.detectChanges();
        const dialog = fixture.debugElement.query(By.css('dot-dialog'));
        dialog.triggerEventHandler('hide', {});
        fixture.detectChanges();
        expect(component.close).toHaveBeenCalled();
        expect(dotDialogMessageServiceMock.push).toHaveBeenCalledWith(null);
    });
});
