/* eslint-disable @typescript-eslint/no-empty-function */

import { Observable, Subject } from 'rxjs';

import { Injectable } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotIconComponent, DotIconModule } from '@dotcms/ui';

import { DotMessageDisplayComponent } from './dot-message-display.component';
import { DotMessage, DotMessageSeverity, DotMessageType } from './model';
import { DotMessageDisplayService } from './services';

@Injectable()
export class DotMessageDisplayServiceMock {
    messages$: Subject<DotMessage> = new Subject<DotMessage>();

    messages(): Observable<DotMessage> {
        return this.messages$.asObservable();
    }

    push(_message: DotMessage): void {}

    unsubscribe(): void {}
}

describe('DotMessageDisplayComponent', () => {
    let component: DotMessageDisplayComponent;
    const dotMessageDisplayServiceMock: DotMessageDisplayServiceMock =
        new DotMessageDisplayServiceMock();
    let fixture: ComponentFixture<DotMessageDisplayComponent>;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [ToastModule, DotIconModule, BrowserAnimationsModule],
            declarations: [DotMessageDisplayComponent],
            providers: [
                { provide: DotMessageDisplayService, useValue: dotMessageDisplayServiceMock }
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotMessageDisplayComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should have p-toast', () => {
        expect(fixture.debugElement.query(By.css('p-toast'))).not.toBeNull();
    });

    it('should have dot-icon', () => {
        dotMessageDisplayServiceMock.messages$.next({
            life: 300,
            message: 'message',
            portletIdList: [],
            severity: DotMessageSeverity.ERROR,
            type: DotMessageType.SIMPLE_MESSAGE
        });
        fixture.detectChanges();
        expect(fixture.debugElement.query(By.css('dot-icon'))).not.toBeNull();
    });

    it('should have set check name on sucess', () => {
        dotMessageDisplayServiceMock.messages$.next({
            life: 300,
            message: 'message',
            portletIdList: [],
            severity: DotMessageSeverity.SUCCESS,
            type: DotMessageType.SIMPLE_MESSAGE
        });
        fixture.detectChanges();
        const icon: DotIconComponent = fixture.debugElement.query(
            By.css('dot-icon')
        ).componentInstance;
        expect(icon.name).toEqual('check');
    });

    it('should have span', () => {
        dotMessageDisplayServiceMock.messages$.next({
            life: 300,
            message: 'message',
            portletIdList: [],
            severity: DotMessageSeverity.ERROR,
            type: DotMessageType.SIMPLE_MESSAGE
        });
        fixture.detectChanges();
        expect(fixture.debugElement.query(By.css('span'))).not.toBeNull();
    });

    it('should add a new message', () => {
        const messageService = fixture.componentRef.injector.get(MessageService);
        spyOn(messageService, 'add');

        dotMessageDisplayServiceMock.messages$.next({
            life: 300,
            message: 'message',
            portletIdList: [],
            severity: DotMessageSeverity.ERROR,
            type: DotMessageType.SIMPLE_MESSAGE
        });

        expect(messageService.add).toHaveBeenCalledWith({
            life: 300,
            detail: 'message',
            severity: 'error'
        });
    });

    it('should unsubscribe', () => {
        spyOn(dotMessageDisplayServiceMock, 'unsubscribe');
        component.ngOnDestroy();
        expect(dotMessageDisplayServiceMock.unsubscribe).toHaveBeenCalled();
    });
});
