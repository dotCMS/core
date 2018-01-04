import { ContentTypeFieldsAddRowComponent } from './content-type-fields-add-row.component';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TooltipModule } from 'primeng/primeng';
import { ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { HotkeysService, Hotkey } from 'angular2-hotkeys';
import { TestHotkeysMock } from './../../../../test/hotkeys-service.mock';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';

describe('ContentTypeFieldsAddRowComponent', () => {
    let comp: ContentTypeFieldsAddRowComponent;
    let fixture: ComponentFixture<ContentTypeFieldsAddRowComponent>;
    let de: DebugElement;
    let el: HTMLElement;
    let testHotKeysMock: TestHotkeysMock;
    let toolTips: string[];

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.content.add_rows': 'Add Rows',
        'contenttypes.content.one_column': 'One column',
        'contenttypes.content.two_columns': 'Second column',
        'contenttypes.content.three_columns': 'Three columns',
        'contenttypes.content.four_columns': 'Four columns'
    });

    beforeEach(() => {
        testHotKeysMock = new TestHotkeysMock();

        DOTTestBed.configureTestingModule({
            declarations: [
                ContentTypeFieldsAddRowComponent
            ],
            imports: [TooltipModule, BrowserAnimationsModule],
            providers: [
                { provide: HotkeysService, useValue: testHotKeysMock },
                { provide: DotMessageService, useValue: messageServiceMock}
            ]
        });

        fixture = DOTTestBed.createComponent(ContentTypeFieldsAddRowComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
        el = de.nativeElement;
    });

    it('should render disabled input', () => {
        comp.disabled = true;

        fixture.detectChanges();

        const buttonElement = de.query(By.css('button'));

        expect(buttonElement.nativeElement.disabled).toEqual(true);
    });

    it('should render columns input', () => {
        const columnSelectionList = de.query(By.css('.dot-add-rows-columns-list__container'));

        comp.columns = [1, 2, 3];

        fixture.detectChanges();

        expect(columnSelectionList.children.length).toEqual(3);
    });

    it('should render tooltip input data', () => {
        comp.toolTips = ['contenttypes.content.one_column',
        'contenttypes.content.four_columns'];

        fixture.detectChanges();

        const columnSelectionList = de.query(By.css('.dot-add-rows-columns-list__container'));
        const columnSelectionItems = columnSelectionList.nativeElement.children;

        expect(columnSelectionItems[1].attributes[9].textContent).toEqual('Four columns');
    });

    it('should display the add rows button by default', () => {
        const addRowContainer = de.query(By.css('.dot-add-rows-button__container'));
        const buttonElement = de.query(By.css('button'));

        fixture.detectChanges();

        expect(addRowContainer.nativeElement.classList.contains('dot-add-rows__add')).toEqual(true);
        expect(buttonElement).toBeTruthy();
    });

    it('should display row selection after click on Add Rows button', () => {
        fixture.detectChanges();
        const addRowContainer = de.query(By.css('.dot-add-rows-columns-list__container'));

        const addButton = de.query(By.css('button'));
        addButton.nativeElement.click();
        fixture.detectChanges();

        expect(addRowContainer.nativeElement.classList.contains('dot-add-rows__select')).toEqual(true);
    });

    it('should focus the first column selection after click on Add Rows button', (done) => {
        fixture.detectChanges();

        comp.setColumnSelect();

        const columnSelectionList = de.query(By.css('.dot-add-rows-columns-list__container'));
        const firstElement = columnSelectionList.children[0];

        setTimeout(() => {
            expect(document.activeElement).toBe(firstElement.nativeElement);
            done();
        }, 201);

        expect(firstElement.nativeElement.classList).toContain('active');
    });

    it('should bind keyboard events after click on Add Rows button', () => {
        fixture.detectChanges();

        spyOn(comp, 'setKeyboardEvent');

        const addButton = de.nativeElement.querySelector('.dot-add-rows-button__container button');
        addButton.click();
        fixture.detectChanges();

        expect(comp.setKeyboardEvent).toHaveBeenCalledTimes(4);
    });

    it('should escape to add state', () => {
        spyOn(comp, 'removeFocus');
        spyOn(testHotKeysMock, 'remove');

        fixture.detectChanges();

        const addButton = de.nativeElement.querySelector('.dot-add-rows-button__container button');
        addButton.click();
        fixture.detectChanges();

        expect(comp.rowState).toBe('select');

        testHotKeysMock.callback(['esc']);
        fixture.detectChanges();

        expect(comp.rowState).toEqual('add', 'set state to add');
        expect(comp.selectedColumnIndex).toEqual(0, 'set column index to zero');
        expect(testHotKeysMock.remove).toHaveBeenCalledTimes(1);
        expect(comp.removeFocus).toHaveBeenCalledTimes(1);
    });

    it('should go to last item when left key is pressed while in first item', () => {
        fixture.detectChanges();
        comp.setColumnSelect();
        fixture.detectChanges();
        testHotKeysMock.callback(['left']);

        expect(comp.selectedColumnIndex).toEqual(comp.columns.length - 1);
    });

    it('should add focus and active to previous item after using left keyboard', () => {
        fixture.detectChanges();
        comp.setColumnSelect();
        comp.onMouseEnter(2, new Event('MouseEvent'));
        fixture.detectChanges();

        const items = de.queryAll(By.css('li'));

        expect(items[2].nativeElement.classList).toContain('active');

        testHotKeysMock.callback(['left']);
        fixture.detectChanges();

        expect(items[1].nativeElement.classList).toContain('active');
    });

    it('should go to second item when right key is pressed while in first item', () => {
        fixture.detectChanges();
        comp.setColumnSelect();
        fixture.detectChanges();
        testHotKeysMock.callback(['right']);

        expect(comp.selectedColumnIndex).toEqual(1);
    });

    it('Should add focus and active to next item after using right keyboard', () => {
        fixture.detectChanges();
        comp.setColumnSelect();
        comp.onMouseEnter(2, new Event('MouseEvent'));
        fixture.detectChanges();

        const items = de.queryAll(By.css('li'));

        expect(items[2].nativeElement.classList).toContain('active');

        testHotKeysMock.callback(['right']);
        fixture.detectChanges();

        expect(items[3].nativeElement.classList).toContain('active');
    });

    it('should select columns number after click on li', () => {
        fixture.detectChanges();
        let colsToEmit: number;

        const addButton = de.query(By.css('button'));
        const lis = de.queryAll(By.css('li'));
        addButton.nativeElement.click();

        comp.selectColums.subscribe(cols => colsToEmit = cols);

        lis[0].nativeElement.click();

        expect(colsToEmit).toEqual(1);
    });

    it('should select columns number after use enter keyboard on li', () => {
        fixture.detectChanges();
        comp.setColumnSelect();

        const spy = spyOn(comp.selectColums, 'emit');

        testHotKeysMock.callback(['enter']);

        expect(spy).toHaveBeenCalledWith(1);
    });

    it('should display row selection after ctrl+a combo keyboard event', () => {
        fixture.detectChanges();
        const addRowContainer = de.query(By.css('.dot-add-rows-columns-list__container'));

        testHotKeysMock.callback(['ctrl+a']);
        fixture.detectChanges();

        expect(addRowContainer.nativeElement.classList.contains('dot-add-rows__select')).toEqual(true);
    });

    it('should set toolTip value', () => {
        toolTips = [
            'contenttypes.content.one_column',
            'contenttypes.content.two_columns',
            'contenttypes.content.three_columns',
            'contenttypes.content.four_columns'
        ];
        fixture.detectChanges();

        const columnSelectionList = de.query(By.css('.dot-add-rows-columns-list__container'));
        const columnSelectionItems = columnSelectionList.nativeElement.children;

        expect(comp.i18nMessages[toolTips[1]]).toEqual(columnSelectionItems[1].attributes[9].textContent);
    });

    it('should remove hotkeysService on destroy', () => {
        const hoykeys: Hotkey[] = <Hotkey[]>testHotKeysMock.get(['left', 'right', 'enter', 'esc']);
        const spyMethod = spyOn(testHotKeysMock, 'remove');

        comp.ngOnDestroy();

        expect(spyMethod).toHaveBeenCalledWith(hoykeys);
    });
});
