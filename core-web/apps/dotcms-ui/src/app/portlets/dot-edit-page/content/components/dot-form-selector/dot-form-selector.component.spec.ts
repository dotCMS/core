import { Observable, of as observableOf } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';

import { delay } from 'rxjs/operators';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';
import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    dotcmsContentTypeBasicMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotFormSelectorComponent } from './dot-form-selector.component';

const mockContentType: DotCMSContentType = {
    ...dotcmsContentTypeBasicMock,
    clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
    defaultType: false,
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: null,
    name: 'Hello World',
    owner: '123',
    system: false
};

@Component({
    template: ` <dot-form-selector [show]="show"></dot-form-selector> `
})
class TestHostComponent {
    show = false;
}

function getWithOffsetMock<T>(): Observable<T> {
    return observableOf([mockContentType]).pipe(delay(0)) as Observable<T>;
}

const messageServiceMock = new MockDotMessageService({
    'contenttypes.form.name': 'Name',
    Select: 'Select',
    'modes.Add-Form': 'Add Form'
});

describe('DotFormSelectorComponent', () => {
    let component: DotFormSelectorComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let paginatorService: PaginatorService;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotFormSelectorComponent, TestHostComponent],
            providers: [
                PaginatorService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock }
            ],
            imports: [
                DotDialogModule,
                BrowserAnimationsModule,
                HttpClientTestingModule,
                TableModule,
                DotPipesModule,
                DotMessagePipe,
                ButtonModule
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.debugElement.query(By.css('dot-form-selector')).componentInstance;
        de = fixture.debugElement;
        paginatorService = component.paginatorService;
    });

    describe('hidden dialog', () => {
        beforeEach(() => {
            fixture.detectChanges();
        });

        it('should have dot-dialog hidden', () => {
            const dialog: DebugElement = de.query(By.css('dot-dialog'));
            expect(dialog.componentInstance.visible).toBe(false);
            expect(dialog.componentInstance.header).toBe('Add Form');
        });
    });

    describe('show dialog', () => {
        beforeEach(() => {
            spyOn(paginatorService, 'getWithOffset').and.callFake(getWithOffsetMock);

            fixture.detectChanges();
            fixture.componentInstance.show = true;
            fixture.detectChanges();
        });

        describe('p-dataTable component', () => {
            let pTableComponent: DebugElement;

            beforeEach(() => {
                pTableComponent = de.query(By.css('p-table'));
            });

            it('should have one', (done) => {
                setTimeout(() => {
                    fixture.detectChanges();
                    expect(pTableComponent).toBeTruthy();
                    done();
                }, 0);
            });
        });

        describe('data', () => {
            describe('pagination', () => {
                it('should set the url', () => {
                    expect(paginatorService.url).toBe('v1/contenttype?type=FORM');
                });

                it('should load first page and add paginator CSS class', async () => {
                    await fixture.whenStable();
                    paginatorService.totalRecords = 12;
                    paginatorService.paginationPerPage = 5;
                    fixture.detectChanges();
                    expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
                    expect(component.items).toEqual([mockContentType]);
                    expect(component.dotDialog.dialog.nativeElement.classList).toContain(
                        'paginator'
                    );
                });
            });

            describe('events', () => {
                beforeEach(async () => {
                    spyOn(component.pick, 'emit');
                    spyOn(component.shutdown, 'emit');

                    fixture.componentInstance.show = true;
                    paginatorService.totalRecords = 1;
                    paginatorService.paginationPerPage = 1;

                    await fixture.whenStable();
                });

                it('should emit close', () => {
                    const dialog: DebugElement = de.query(By.css('dot-dialog'));
                    dialog.triggerEventHandler('hide', true);

                    expect(component.shutdown.emit).toHaveBeenCalledWith(true);
                });

                it('trigger event when click select button', () => {
                    fixture.detectChanges();
                    const button = de.query(By.css('.form-selector__button'));
                    button.triggerEventHandler('click', null);
                    expect(component.pick.emit).toHaveBeenCalledWith(mockContentType);
                });
            });
        });

        afterEach(() => {
            fixture.componentInstance.show = false;
            fixture.detectChanges();
        });
    });
});
