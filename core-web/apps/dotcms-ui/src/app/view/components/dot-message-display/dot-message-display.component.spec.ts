/* eslint-disable @typescript-eslint/no-empty-function */

import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotMessageDisplayService } from '@dotcms/data-access';
import { DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import { DotIconComponent } from '@dotcms/ui';
import { DotMessageDisplayServiceMock } from '@dotcms/utils-testing';

import { DotMessageDisplayComponent } from './dot-message-display.component';

describe('DotMessageDisplayComponent', () => {
    let component: DotMessageDisplayComponent;
    const dotMessageDisplayServiceMock: DotMessageDisplayServiceMock =
        new DotMessageDisplayServiceMock();
    let fixture: ComponentFixture<DotMessageDisplayComponent>;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                DotMessageDisplayComponent,
                ToastModule,
                DotIconComponent,
                BrowserAnimationsModule
            ],
            providers: [MessageService]
        })
            .overrideComponent(DotMessageDisplayComponent, {
                set: {
                    providers: [
                        MessageService,
                        {
                            provide: DotMessageDisplayService,
                            useValue: dotMessageDisplayServiceMock
                        }
                    ]
                }
            })
            .compileComponents();
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
        const icon = fixture.debugElement.query(By.css('dot-icon')).componentInstance;
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
        jest.spyOn(messageService, 'add');

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
        jest.spyOn(dotMessageDisplayServiceMock, 'unsubscribe');
        component.ngOnDestroy();
        expect(dotMessageDisplayServiceMock.unsubscribe).toHaveBeenCalled();
    });
});
