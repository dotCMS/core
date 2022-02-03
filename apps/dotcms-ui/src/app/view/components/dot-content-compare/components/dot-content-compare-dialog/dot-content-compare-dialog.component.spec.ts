/* eslint-disable @typescript-eslint/no-explicit-any */

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentCompareDialogComponent } from './dot-content-compare-dialog.component';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { COMPARE_CUSTOM_EVENT } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { By } from '@angular/platform-browser';
import cleanUpDialog from '@tests/clean-up-dialog';
import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';

@Component({
    selector: 'dot-content-compare',
    template: ''
})
class TestDotContentCompareComponent {
    @Input() data: any;
    @Output() close = new EventEmitter<boolean>();
}

describe('DotContentCompareDialogComponent', () => {
    let component: DotContentCompareDialogComponent;
    let fixture: ComponentFixture<DotContentCompareDialogComponent>;
    let dotEventsService: DotEventsService;
    const messageServiceMock = new MockDotMessageService({
        compare: 'Compare'
    });
    const data = 'data';

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotContentCompareDialogComponent, TestDotContentCompareComponent],
            imports: [DotDialogModule, DotMessagePipeModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotEventsService
            ]
        });

        fixture = TestBed.createComponent(DotContentCompareDialogComponent);
        dotEventsService = TestBed.inject(DotEventsService);
        fixture.detectChanges();
        component = fixture.componentInstance;
        dotEventsService.notify(COMPARE_CUSTOM_EVENT, data);
        fixture.detectChanges();
    });

    it('should listen compare event and pass the data', () => {
        const contentCompare: TestDotContentCompareComponent = fixture.debugElement.query(
            By.css('dot-content-compare')
        ).componentInstance;
        expect(contentCompare.data).toEqual(data);
    });

    it('should hide dialog on close', () => {
        const closeBtn = fixture.debugElement.query(By.css('.dialog__header dot-icon-button'));
        closeBtn.triggerEventHandler('click', {
            preventDefault: () => {
                //
            }
        });
        fixture.detectChanges();
        const dotDialog: DotDialogComponent = fixture.debugElement.query(
            By.css('dot-dialog')
        ).componentInstance;
        expect(dotDialog.visible).toEqual(false);
    });

    it('should hide dialog on close event from DotConentCompare', () => {
        const contentCompare: TestDotContentCompareComponent = fixture.debugElement.query(
            By.css('dot-content-compare')
        ).componentInstance;
        component.show = true;
        contentCompare.close.emit(true);

        expect(component.show).toEqual(false);
    });

    it('should have the correct header', () => {
        expect(
            fixture.debugElement.query(By.css('dot-dialog .dialog__title')).nativeElement.innerText
        ).toEqual('Compare');
    });

    afterEach(() => {
        cleanUpDialog(fixture);
    });
});
