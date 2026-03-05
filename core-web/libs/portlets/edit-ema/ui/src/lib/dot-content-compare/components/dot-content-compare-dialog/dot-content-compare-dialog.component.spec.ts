/* eslint-disable @typescript-eslint/no-explicit-any */

import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import {
    DotEventsService,
    DotMessageService,
    DotContentTypeService,
    DotContentletService,
    DotFormatDateService,
    DotHttpErrorManagerService,
    DotAlertConfirmService,
    DotIframeService
} from '@dotcms/data-access';
import { DotContentCompareEvent } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { cleanUpDialog, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentCompareDialogComponent } from './dot-content-compare-dialog.component';

const COMPARE_CUSTOM_EVENT = 'compare-contentlet';

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
            imports: [DotContentCompareDialogComponent, DotMessagePipe],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotEventsService,
                { provide: DotContentTypeService, useValue: {} },
                { provide: DotContentletService, useValue: {} },
                { provide: DotFormatDateService, useValue: {} },
                { provide: DotHttpErrorManagerService, useValue: {} },
                { provide: DotAlertConfirmService, useValue: {} },
                { provide: DotIframeService, useValue: {} },
                provideHttpClientTesting()
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
        // Wait for the async pipe to resolve
        fixture.detectChanges();

        const contentCompare = fixture.debugElement.query(By.css('dot-content-compare'));
        expect(contentCompare).toBeTruthy();
    });

    it('should hide dialog on close', () => {
        component.close();
        expect(component.show).toEqual(false);
    });

    it('should hide dialog on close event from DotConentCompare', () => {
        const contentCompare = fixture.debugElement.query(
            By.css('dot-content-compare')
        ).componentInstance;
        component.show = true;
        contentCompare.shutdown.emit(true);

        expect(component.show).toEqual(false);
    });

    it('should have the correct header', () => {
        const pDialog = fixture.debugElement.query(By.css('p-dialog'));
        expect(pDialog).toBeTruthy();
        expect(pDialog.componentInstance.header).toEqual('Compare');
    });

    afterEach(() => {
        cleanUpDialog(fixture);
    });
});
