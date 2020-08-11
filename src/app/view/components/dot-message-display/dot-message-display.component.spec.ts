import { Injectable } from '@angular/core';
import { DotMessageDisplayComponent } from './dot-message-display.component';
import { ComponentFixture, async, TestBed } from '@angular/core/testing';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { By } from '@angular/platform-browser';
import { DotMessageDisplayService } from './services';
import { Observable, Subject } from 'rxjs';
import { DotMessage } from './model';
import { DotMessageSeverity } from './model';
import { DotMessageType } from './model';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotIconComponent } from '@components/_common/dot-icon/dot-icon.component';

@Injectable()
export class DotMessageDisplayServiceMock {
    messages$: Subject<DotMessage> = new Subject<DotMessage>();

    messages(): Observable<DotMessage> {
        return this.messages$.asObservable();
    }

    unsubscribe(): void {}
}

describe('DotMessageDisplayComponent', () => {
    let component: DotMessageDisplayComponent;
    const dotMessageDisplayServiceMock: DotMessageDisplayServiceMock = new DotMessageDisplayServiceMock();
    let fixture: ComponentFixture<DotMessageDisplayComponent>;

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                imports: [ToastModule, DotIconModule, DotIconButtonModule, BrowserAnimationsModule],
                declarations: [DotMessageDisplayComponent],
                providers: [
                    { provide: DotMessageDisplayService, useValue: dotMessageDisplayServiceMock }
                ]
            }).compileComponents();
        })
    );

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
        const icon: DotIconComponent = fixture.debugElement.query(By.css('dot-icon'))
            .componentInstance;
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
