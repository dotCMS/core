import { of as observableOf, Observable } from 'rxjs';
import { DebugElement, Injectable } from '@angular/core';
import { DotFormSelectorComponent } from './dot-form-selector.component';
import { ComponentFixture, TestBed, async, tick, fakeAsync } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { By } from '@angular/platform-browser';
import { PaginatorService } from '@services/paginator';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { MessageKeyDirective } from '@directives/message-keys/message-keys.directive';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

const mockContentType = {
    clazz: 'com.dotcms.contenttype.model.type.ImmutableWidgetContentType',
    defaultType: false,
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: null,
    name: 'Hello World',
    owner: '123',
    system: false
};

@Injectable()
class PaginatorServiceMock {
    url = '';
}

const messageServiceMock = new MockDotMessageService({
    'contenttypes.form.name': 'Name',
    Select: 'Select',
    'modes.Add-Form': 'Add Form'
});

xdescribe('DotFormSelectorComponent', () => {
    let component: DotFormSelectorComponent;
    let fixture: ComponentFixture<DotFormSelectorComponent>;
    let de: DebugElement;
    let paginatorService: PaginatorService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotFormSelectorComponent, MessageKeyDirective],
            providers: [
                {
                    provide: PaginatorService,
                    useClass: PaginatorServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ],
            imports: [DotDialogModule, BrowserAnimationsModule]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotFormSelectorComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        paginatorService = fixture.debugElement.injector.get(PaginatorService);
    });

    describe('hidden dialog', () => {
        beforeEach(() => {
            fixture.detectChanges();
        });

        it('should have dot-dialog hidden', () => {
            const dialog: DebugElement = de.query(By.css('dot-dialog'));
            expect(dialog.componentInstance.show).toBe(false);
            expect(dialog.componentInstance.header).toBe('Add Form');
        });
    });

    describe('show dialog', () => {
        beforeEach(() => {
            spyOn(paginatorService, 'getWithOffset').and.callFake((offset) => {
                return observableOf([mockContentType]);
            });

            component.show = true;
            fixture.detectChanges();
        });

        describe('p-dataTable component', () => {
            let pTableComponent: DebugElement;

            beforeEach(() => {
                fixture.detectChanges();
                pTableComponent = de.query(By.css('p-dataTable'));
            });

            it('should have one', () => {
                expect(pTableComponent).toBeTruthy();
            });
        });

        describe('data', () => {
            let pTableComponent;

            beforeEach(() => {
                fixture.detectChanges();
                pTableComponent = de.query(By.css('p-dataTable'));
            });

            describe('pagination', () => {
                it('should set the url', () => {
                    expect(paginatorService.url).toBe('v1/contenttype?type=FORM');
                });

                it('should load first page', () => {
                    expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
                    expect(component.items).toEqual([mockContentType]);
                });
            });

            describe('data/columns', () => {
                it('should have two columns and two rows', () => {
                    const headRows = de.queryAll(By.css('table thead tr'));
                    expect(headRows.length).toBe(1, 'must have one row head');

                    const firstRowColumns = headRows[0].queryAll(By.css('th'));
                    expect(firstRowColumns.length).toBe(2, 'must have two columns in head');

                    const bodyRow = de.queryAll(By.css('table tbody tr'));
                    expect(bodyRow.length).toBe(1, 'must have one row into body');

                    const secondRowColumns = bodyRow[0].queryAll(By.css('td'));
                    expect(secondRowColumns.length).toBe(2, 'must have two columns into body');
                });

                it('first column should have the right header and content', () => {
                    const label = de.query(By.css('table thead tr:first-child th:first-child span')).nativeElement.innerHTML;
                    expect(label).toBe('Name');

                    const content = de.query(By.css('table tbody tr:first-child td:first-child span')).nativeElement.innerHTML;
                    expect(content).toBe('Hello World');
                });

                it('second column should have the right header and select button', () => {
                    const label = de.query(By.css('table thead tr:first-child th:nth-child(2n) span')).nativeElement.innerHTML;
                    expect(label).toBe('');

                    const link = de.query(By.css('.form-selector__button'));
                    expect(link).not.toBeNull();
                    expect(link.nativeElement.innerText).toBe('Select');
                });
            });

            describe('events', () => {
                beforeEach(() => {
                    spyOn(component.select, 'emit');
                    spyOn(component.close, 'emit');
                });

                it('should emit close', () => {
                    const dialog: DebugElement = de.query(By.css('dot-dialog'));
                    dialog.triggerEventHandler('close', {});

                    expect(component.close.emit).toHaveBeenCalledWith({});
                });

                it('tigger event when click select button', () => {
                    const button = de.query(By.css('.form-selector__button'));
                    button.triggerEventHandler('click', null);

                    expect(component.select.emit).toHaveBeenCalledWith(mockContentType);
                });
            });
        });
    });
});
