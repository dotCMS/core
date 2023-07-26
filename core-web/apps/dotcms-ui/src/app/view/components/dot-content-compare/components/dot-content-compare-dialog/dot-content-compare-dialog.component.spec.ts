/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotContentCompareEvent } from '@components/dot-content-compare/dot-content-compare.component';
import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { COMPARE_CUSTOM_EVENT } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotEventsService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { cleanUpDialog, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentCompareDialogComponent } from './dot-content-compare-dialog.component';

@Component({
    selector: 'dot-content-compare',
    template: ''
})
class TestDotContentCompareComponent {
    @Input() data: DotContentCompareEvent;
    @Output() shutdown = new EventEmitter<boolean>();
}

describe('DotContentCompareDialogComponent', () => {
    let component: DotContentCompareDialogComponent;
    let fixture: ComponentFixture<DotContentCompareDialogComponent>;
    let dotEventsService: DotEventsService;
    const messageServiceMock = new MockDotMessageService({
        compare: 'Compare'
    });
    const data = 'data' as unknown as DotContentCompareEvent;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotContentCompareDialogComponent, TestDotContentCompareComponent],
            imports: [DotDialogModule, DotMessagePipe],
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
        contentCompare.shutdown.emit(true);

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
