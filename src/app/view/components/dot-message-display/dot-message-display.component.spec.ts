import { Injectable } from '@angular/core';
import { DotMessageDisplayComponent } from './dot-message-display.component';
import { ComponentFixture, async, TestBed } from '@angular/core/testing';
import { DOTTestBed } from '@tests/dot-test-bed';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { By } from '@angular/platform-browser';
import { DotMessageDisplayService } from './services';
import { Observable, Subject } from 'rxjs';


@Injectable()
export class DotMessageDisplayServiceMock {
    messages$: Subject<Dot.Message.Message> = new Subject<Dot.Message.Message>();

    messages(): Observable<Dot.Message.Message> {
        return this.messages$.asObservable();
    }

    unsubscribe(): void {}
}



describe('DotMessageDisplayComponent', () => {
    let component: DotMessageDisplayComponent;
    const dotMessageDisplayServiceMock: DotMessageDisplayServiceMock = new DotMessageDisplayServiceMock();
    let fixture: ComponentFixture<DotMessageDisplayComponent>;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [ToastModule],
            declarations: [DotMessageDisplayComponent],
            providers: [
                { provide: DotMessageDisplayService, useValue: dotMessageDisplayServiceMock },
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

    it('should add a new message', () => {
        const messageService = fixture.componentRef.injector.get(MessageService);
        spyOn(messageService, 'add');

        dotMessageDisplayServiceMock.messages$.next({
            life: 300,
            message: 'message',
            portletIdList: [],
            severity: Dot.Message.Severity.ERROR,
            type: Dot.Message.Type.SIMPLE_MESSAGE
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
