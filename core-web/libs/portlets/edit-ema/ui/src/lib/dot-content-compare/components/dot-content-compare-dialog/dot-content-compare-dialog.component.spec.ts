/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotEventsService, DotMessageService } from '@dotcms/data-access';
import { DotContentCompareEvent } from '@dotcms/dotcms-models';
import { DotDialogComponent, DotDialogModule, DotMessagePipe } from '@dotcms/ui';
import { cleanUpDialog, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentCompareDialogComponent } from './dot-content-compare-dialog.component';

const COMPARE_CUSTOM_EVENT = 'compare-contentlet';

@Component({
    standalone: false,
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
        const closeBtn = fixture.debugElement.query(By.css('.dialog__header p-button'));
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
            fixture.debugElement.query(By.css('dot-dialog .dialog__title')).nativeElement.innerHTML
        ).toEqual('Compare');
    });

    afterEach(() => {
        cleanUpDialog(fixture);
    });
});
