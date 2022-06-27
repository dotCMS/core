import { of as observableOf, Observable } from 'rxjs';
import { DebugElement, Component } from '@angular/core';
import { DotFormSelectorComponent } from './dot-form-selector.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { By } from '@angular/platform-browser';
import { PaginatorService } from '@services/paginator';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { dotcmsContentTypeBasicMock } from '@tests/dot-content-types.mock';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { delay } from 'rxjs/operators';

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
    return observableOf([mockContentType]).pipe(delay(10)) as Observable<T>;
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

    beforeEach(
        waitForAsync(() => {
            DOTTestBed.configureTestingModule({
                declarations: [DotFormSelectorComponent, TestHostComponent],
                providers: [
                    PaginatorService,
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    }
                ],
                imports: [DotDialogModule, BrowserAnimationsModule]
            });
        })
    );

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

            fixture.componentInstance.show = true;
            fixture.detectChanges();
        });

        describe('p-dataTable component', () => {
            let pTableComponent: DebugElement;

            beforeEach(() => {
                pTableComponent = de.query(By.css('p-table'));
            });

            it('should have one', () => {
                expect(pTableComponent).toBeTruthy();
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
                beforeEach(() => {
                    spyOn(component.pick, 'emit');
                    spyOn(component.shutdown, 'emit');

                    fixture.componentInstance.show = true;
                    fixture.detectChanges();
                });

                it('should emit close', () => {
                    const dialog: DebugElement = de.query(By.css('dot-dialog'));
                    dialog.triggerEventHandler('hide', true);

                    expect(component.shutdown.emit).toHaveBeenCalledWith(true);
                });

                xit('trigger event when click select button', async () => {
                    fixture.detectChanges();
                    await fixture.whenStable();

                    const button = de.query(By.css('.form-selector__button'));
                    button.triggerEventHandler('click', null);

                    expect(component.pick.emit).toHaveBeenCalledWith(mockContentType);
                });
            });
        });
    });
});
