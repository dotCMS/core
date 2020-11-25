import { By } from '@angular/platform-browser';
import { Component, DebugElement, forwardRef, Input } from '@angular/core';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotTextareaContentComponent } from './dot-textarea-content.component';
import { FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { SelectButtonModule } from 'primeng/selectbutton';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { EditorComponent } from 'ngx-monaco-editor';

function cleanOptionText(option) {
    return option.replace(/\r?\n|\r/g, '');
}

@Component({
    selector: 'ngx-monaco-editor',
    template: '<div>CODE EDITOR</div>',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MonacoEditorMock)
        }
    ]
})
class MonacoEditorMock {
    @Input() options: any;

    writeValue() {}

    registerOnChange() {}

    registerOnTouched() {}
}

describe('DotTextareaContentComponent', () => {
    let component: DotTextareaContentComponent;
    let fixture: ComponentFixture<DotTextareaContentComponent>;
    let de: DebugElement;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotTextareaContentComponent, MonacoEditorMock],
                imports: [SelectButtonModule, InputTextareaModule, FormsModule]
            }).compileComponents();

            fixture = TestBed.createComponent(DotTextareaContentComponent);
            component = fixture.componentInstance;
            de = fixture.debugElement;
        })
    );

    it('should show a select mode buttons by default', () => {
        fixture.detectChanges();
        const selectField = de.query(By.css('.textarea-content__select-field'));
        expect(selectField).not.toBeFalsy();
    });

    it('should have options: plain, and code in the select mode buttons by default', () => {
        fixture.detectChanges();
        const selectFieldWrapper = de.query(
            By.css('.textarea-content__select-field .p-selectbutton')
        );

        console.log();

        expect(selectFieldWrapper.componentInstance.options).toEqual([
            { label: 'Plain', value: 'plain' },
            { label: 'Code', value: 'code' }
        ]);
    });

    it('should hide select mode buttons when only one option to show is passed', () => {
        component.show = ['code'];
        fixture.detectChanges();
        const selectField = de.query(By.css('.textarea-content__select-field'));
        expect(selectField == null).toBe(true, 'hide buttons');
    });

    it("should have option 'Plain' selected by default", async () => {
        fixture.detectChanges();
        await fixture.whenStable();
        const selectButton = de.query(By.css('.textarea-content__select-field'));
        console.log(selectButton.componentInstance.value);
        expect(selectButton.componentInstance.value).toBe('plain');
    });

    it("should show 'Plain' field by default", () => {
        fixture.detectChanges();
        const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
        expect(plainFieldTexarea).toBeTruthy('show plain field');
        /*
            We should be u
            sing .toBeFalsey() but there is a bug with this method:
            https://github.com/angular/angular/issues/14235
        */
        const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
        expect(codeFieldTexarea == null).toBe(true, 'hide code field');
    });

    it('should show only the valid options we passed in the select mode butons', () => {
        component.show = ['code', 'plain', 'sadf', 'hello', 'world'];
        fixture.detectChanges();
        const selectFieldWrapper = de.query(
            By.css('.textarea-content__select-field .p-selectbutton')
        );
        selectFieldWrapper.children.forEach((option) => {
            const optionText = cleanOptionText(option.nativeElement.textContent);
            expect(['Plain', 'Code'].indexOf(optionText)).toBeGreaterThan(
                -1,
                `${optionText} exist`
            );
        });
    });

    it('should set width', async () => {
        component.width = '50%';
        fixture.detectChanges();
        await fixture.whenStable();
        const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
        expect(plainFieldTexarea.nativeElement.style.width).toBe('50%', 'plain width setted');

        component.selected = 'code';
        fixture.detectChanges();
        await fixture.whenStable();
        const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
        expect(codeFieldTexarea.nativeElement.style.width).toBe('50%', 'code width setted');

        // TODO: We need to find a way to set the width to the wysiwyg
    });

    it('should set height', async () => {
        component.height = '50%';
        fixture.detectChanges();
        await fixture.whenStable();
        const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
        expect(plainFieldTexarea.nativeElement.style.height).toBe('50%', 'plain height setted');

        component.selected = 'code';
        fixture.detectChanges();
        await fixture.whenStable();
        const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
        expect(codeFieldTexarea.nativeElement.style.height).toBe('50%', 'code height setted');

        // TODO: We need to find a way to set the height to the wysiwyg
    });

    it('should add new line character', () => {
        component.propagateChange = (propagateChangeValue) => {
            expect('aaaa\r\nbbbbb\r\nccccc').toEqual(propagateChangeValue);
        };

        const value = 'aaaa\nbbbbb\nccccc';
        component.onModelChange(value);

        expect(component.value).toEqual(value);
    });

    it('should not repeat \r characters', () => {
        const value = 'aaaa\r\nbbbbb\r\nccccc\nddddd';

        component.propagateChange = (propagateChangeValue) => {
            expect('aaaa\r\nbbbbb\r\nccccc\r\nddddd').toEqual(propagateChangeValue);
        };

        component.onModelChange(value);

        expect(component.value).toEqual(value);
    });

    it('should not propagate enter keyboard event', async () => {
        const spy = jasmine.createSpy('stopPropagation');
        component.show = ['plain', 'code'];
        component.selected = 'plain';

        fixture.detectChanges();
        await fixture.whenStable();

        const textarea = de.query(By.css('.textarea-content__plain-field'));
        textarea.triggerEventHandler('keydown.enter', {
            stopPropagation: spy
        });

        component.selected = 'code';
        fixture.detectChanges();
        await fixture.whenStable();

        const monaco = de.query(By.css('ngx-monaco-editor'));
        monaco.triggerEventHandler('keydown.enter', {
            stopPropagation: spy
        });

        expect(spy).toHaveBeenCalledTimes(2);
    });

    describe('code', () => {
        it('should have default options', () => {
            component.show = ['code'];
            fixture.detectChanges();

            const editor: EditorComponent = de.query(By.css('ngx-monaco-editor')).componentInstance;
            expect(editor.options).toEqual({
                theme: 'vs-light',
                minimap: Object({ enabled: false }),
                cursorBlinking: 'solid',
                overviewRulerBorder: false,
                mouseWheelZoom: false,
                LineNumbersType: 'on',
                selectionHighlight: false,
                roundedSelection: false,
                selectOnLineNumbers: false,
                columnSelection: false,
                language: 'text/plain'
            });
        });

        it('should set langiage', () => {
            component.show = ['code'];
            component.language = 'javascript';
            fixture.detectChanges();

            const editor: EditorComponent = de.query(By.css('ngx-monaco-editor')).componentInstance;
            expect(editor.options).toEqual({
                theme: 'vs-light',
                minimap: Object({ enabled: false }),
                cursorBlinking: 'solid',
                overviewRulerBorder: false,
                mouseWheelZoom: false,
                LineNumbersType: 'on',
                selectionHighlight: false,
                roundedSelection: false,
                selectOnLineNumbers: false,
                columnSelection: false,
                language: 'javascript'
            });
        });
    });
});
