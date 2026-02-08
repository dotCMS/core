/* eslint-disable @typescript-eslint/no-explicit-any */
import { DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { ButtonModule } from 'primeng/button';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TooltipModule } from 'primeng/tooltip';

import { DotEventsService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { ContentTypeFieldsAddRowComponent } from './content-type-fields-add-row.component';

import { DOTTestBed } from '../../../../../../test/dot-test-bed';

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
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn()
            }))
        });

        DOTTestBed.configureTestingModule({
            imports: [
                TooltipModule,
                BrowserAnimationsModule,
                ButtonModule,
                SplitButtonModule,
                RouterTestingModule,
                DotMessagePipe,
                ContentTypeFieldsAddRowComponent
            ],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsAddRowComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
        dotEventsService = fixture.debugElement.injector.get(DotEventsService);
    });

    it('should render disabled input', () => {
        fixture.componentRef.setInput('disabled', true);

        fixture.detectChanges();

        const buttonElement = de.query(By.css('p-splitbutton button'));

        expect(buttonElement.nativeElement.disabled).toEqual(true);
    });

    it('should render columns input', () => {
        fixture.componentRef.setInput('columns', [1, 2, 3]);
        comp.rowState = 'select';

        fixture.detectChanges();
        const columnSelectionList = de.queryAll(By.css('ul li'));
        expect(columnSelectionList.length).toEqual(3);
    });

    it('should display the add rows button by default', () => {
        comp.rowState = 'add';
        fixture.detectChanges();
        const addRowContainer = de.query(By.css('.dot-add-rows__add'));
        const buttonsElement = de.queryAll(By.css('p-splitbutton button'));
        expect(addRowContainer).toBeTruthy();
        expect(buttonsElement[0].nativeElement.textContent).toContain('Add Row');
        expect(comp.actions.map((action) => action.label)).toEqual(['Add Row', 'Add Tab']);
    });

    it('should display row selection after click on Add Rows button and focus the first column selection', () => {
        comp.rowState = 'add';
        comp.setColumnSelect();
        fixture.detectChanges();
        const addRowContainer = de.query(By.css('ul'));
        const firstColumRowContainer = de.query(By.css('li.active'));
        expect(addRowContainer).toBeTruthy();
        expect(firstColumRowContainer).toBeTruthy();
    });

    it('should bind send notification after click on Add Tab button', () => {
        jest.spyOn(dotEventsService, 'notify');
        fixture.detectChanges();
        comp.actions[1].command();
        expect(dotEventsService.notify).toHaveBeenCalledWith('add-tab-divider');
        expect(dotEventsService.notify).toHaveBeenCalledTimes(1);
    });

    it('should select columns number after click on li', () => {
        let colsToEmit: number;
        comp.rowState = 'select';
        fixture.detectChanges();
        const lis = de.queryAll(By.css('ul li'));
        comp.$selectColums.subscribe((cols) => (colsToEmit = cols));
        lis[0].nativeElement.click();
        expect(colsToEmit).toEqual(1);
    });

    it('should display add row when close button has been clicked', () => {
        comp.rowState = 'select';
        fixture.detectChanges();
        const closeButton = de.query(By.css('p-button'));
        closeButton.nativeElement.click();
        fixture.detectChanges();
        expect(comp.rowState).toBe('add');
        expect(comp.selectedColumnIndex).toBe(0);
    });

    it('should call setColumnSelect when "add-row" event received', fakeAsync(() => {
        // Set up the component to show the select state so ViewChild is available
        comp.rowState = 'select';
        fixture.detectChanges();

        jest.spyOn(comp, 'setColumnSelect');
        dotEventsService.notify('add-row');
        tick(201); // Wait for the setTimeout in setColumnSelect

        expect(comp.setColumnSelect).toHaveBeenCalled();
    }));

    it('should handle ViewChild properly when in select state', fakeAsync(() => {
        jest.spyOn(comp, 'setFocus');

        comp.setColumnSelect();
        fixture.detectChanges();
        tick(201);

        expect(comp.setFocus).toHaveBeenCalled();
    }));
});
