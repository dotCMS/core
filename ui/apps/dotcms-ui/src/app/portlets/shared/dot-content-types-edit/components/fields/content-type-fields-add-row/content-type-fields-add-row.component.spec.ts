import { ContentTypeFieldsAddRowComponent } from './content-type-fields-add-row.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TooltipModule } from 'primeng/tooltip';
import { SplitButtonModule } from 'primeng/splitbutton';
import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '@tests/dot-test-bed';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { RouterTestingModule } from '@angular/router/testing';

describe('ContentTypeFieldsAddRowComponent', () => {
    let comp: ContentTypeFieldsAddRowComponent;
    let fixture: ComponentFixture<ContentTypeFieldsAddRowComponent>;
    let de: DebugElement;
    let dotEventsService: DotEventsService;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.content.add_rows': 'Add Rows',
        'contenttypes.content.one_column': 'One column',
        'contenttypes.content.two_columns': 'Second column',
        'contenttypes.content.three_columns': 'Three columns',
        'contenttypes.content.four_columns': 'Four columns',
        'contenttypes.dropzone.rows.tab_divider': 'Add Tab',
        'contenttypes.dropzone.rows.add': 'Add Row'
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [ContentTypeFieldsAddRowComponent],
            imports: [
                TooltipModule,
                BrowserAnimationsModule,
                UiDotIconButtonModule,
                SplitButtonModule,
                RouterTestingModule
            ],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsAddRowComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
        dotEventsService = fixture.debugElement.injector.get(DotEventsService);
    });

    it('should render disabled input', () => {
        comp.disabled = true;

        fixture.detectChanges();

        const buttonElement = de.query(By.css('button'));

        expect(buttonElement.nativeElement.disabled).toEqual(true);
    });

    it('should render columns input', () => {
        comp.columns = [1, 2, 3];
        comp.rowState = 'select';

        fixture.detectChanges();
        const columnSelectionList = de.query(By.css('.dot-add-rows-columns-list__container'));
        expect(columnSelectionList.children.length).toEqual(3);
    });

    it('should display the add rows button by default', () => {
        comp.rowState = 'add';
        fixture.detectChanges();
        const addRowContainer = de.query(By.css('.dot-add-rows-button__container'));
        const buttonsElement = de.queryAll(By.css('button'));
        expect(addRowContainer.nativeElement.classList.contains('dot-add-rows__add')).toEqual(true);
        expect(buttonsElement[0].nativeElement.textContent).toBe('Add Row');
        buttonsElement[1].nativeElement.click();
        fixture.detectChanges();
        const splitOptionsBtn = de.queryAll(By.css('p-splitbutton .p-menuitem-text'));
        expect(splitOptionsBtn.length).toBe(2);
        expect(splitOptionsBtn[0].nativeElement.innerText).toBe('Add Row');
        expect(splitOptionsBtn[1].nativeElement.innerText).toBe('Add Tab');
    });

    it('should display row selection after click on Add Rows button and focus the first column selection', () => {
        comp.rowState = 'add';
        fixture.detectChanges();
        const addButton = de.nativeElement.querySelector('.dot-add-rows-button__container button');
        addButton.click();
        fixture.detectChanges();
        const addRowContainer = de.query(By.css('.dot-add-rows-columns-list__container'));
        const firstColumRowContainer = de.query(By.css('.dot-add-rows-columns-list')).children[0];
        expect(addRowContainer).toBeTruthy();
        expect(firstColumRowContainer.nativeElement.classList.contains('active')).toEqual(true);
    });

    it('should bind send notification after click on Add Tab button', () => {
        spyOn(dotEventsService, 'notify');
        fixture.detectChanges();
        de.queryAll(By.css('button'))[1].nativeElement.click();
        fixture.detectChanges();
        de.queryAll(By.css('p-splitbutton .p-menuitem-link'))[1].nativeElement.click();
        fixture.detectChanges();
        expect(dotEventsService.notify).toHaveBeenCalledWith('add-tab-divider');
    });

    it('should select columns number after click on li', () => {
        fixture.detectChanges();
        let colsToEmit: number;
        const addButton = de.nativeElement.querySelector('.dot-add-rows-button__container button');
        addButton.click();
        fixture.detectChanges();
        const lis = de.queryAll(By.css('li'));
        comp.selectColums.subscribe((cols) => (colsToEmit = cols));
        lis[0].nativeElement.click();
        expect(colsToEmit).toEqual(1);
    });

    it('should display add row when close button has been clicked', () => {
        comp.rowState = 'select';
        fixture.detectChanges();
        const closeButton = de.query(By.css('dot-icon-button'));
        closeButton.nativeElement.click();
        fixture.detectChanges();
        expect(comp.rowState).toBe('add');
        expect(comp.selectedColumnIndex).toBe(0);
    });

    it('should call setColumnSelect when "add-row" event received', fakeAsync(() => {
        fixture.detectChanges();
        spyOn(comp, 'setColumnSelect');
        dotEventsService.notify('add-row');
        tick();
        expect(comp.setColumnSelect).toHaveBeenCalled();
    }));
});
