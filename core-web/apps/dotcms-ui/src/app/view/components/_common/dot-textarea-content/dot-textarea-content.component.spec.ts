/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */
import { MonacoEditorComponent } from '@materia-ui/ngx-monaco-editor';

import { CommonModule } from '@angular/common';
import { Component, DebugElement, forwardRef, Input } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { SelectButtonModule } from 'primeng/selectbutton';
import { TextareaModule } from 'primeng/textarea';

import { DotTextareaContentComponent } from './dot-textarea-content.component';

function cleanOptionText(option) {
    return option.replace(/\r?\n|\r/g, '');
}

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'ngx-monaco-editor',
    template: '<div>CODE EDITOR</div>',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MonacoEditorMockComponent)
        }
    ]
})
class MonacoEditorMockComponent {
    @Input() options: any;

    writeValue() {}

    registerOnChange() {}

    registerOnTouched() {}
}

describe('DotTextareaContentComponent', () => {
    let component: DotTextareaContentComponent;
    let fixture: ComponentFixture<DotTextareaContentComponent>;
    let de: DebugElement;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                DotTextareaContentComponent,
                SelectButtonModule,
                TextareaModule,
                FormsModule,
                MonacoEditorMockComponent
            ]
        })
            .overrideComponent(DotTextareaContentComponent, {
                set: {
                    imports: [
                        CommonModule,
                        FormsModule,
                        SelectButtonModule,
                        MonacoEditorMockComponent
                    ]
                }
            })
            .compileComponents();

        fixture = TestBed.createComponent(DotTextareaContentComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
    }));

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

        expect(selectFieldWrapper.componentInstance.options).toEqual([
            { label: 'Plain', value: 'plain' },
            { label: 'Code', value: 'code' }
        ]);
    });

    it('should hide select mode buttons when only one option to show is passed', () => {
        component.show = ['code'];
        fixture.detectChanges();
        const selectField = de.query(By.css('.textarea-content__select-field'));
        expect(selectField == null).toBe(true);
    });

    it("should have option 'Plain' selected by default", async () => {
        fixture.detectChanges();
        await fixture.whenStable();
        const selectButton = de.query(By.css('.textarea-content__select-field'));
        expect(selectButton.componentInstance.value).toBe('plain');
    });

    it("should show 'Plain' field by default", () => {
        fixture.detectChanges();
        const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
        expect(plainFieldTexarea).toBeTruthy();
        /*
            We should be u
            sing .toBeFalsey() but there is a bug with this method:
            https://github.com/angular/angular/issues/14235
        */
        const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
        expect(codeFieldTexarea == null).toBe(true);
    });

    it('should show only the valid options we passed in the select mode butons', () => {
        component.show = ['code', 'plain', 'sadf', 'hello', 'world'];
        fixture.detectChanges();
        const selectFieldWrapper = de.query(
            By.css('.textarea-content__select-field .p-selectbutton')
        );
        selectFieldWrapper.children.forEach((option) => {
            const optionText = cleanOptionText(option.nativeElement.textContent);
            expect(['Plain', 'Code'].indexOf(optionText)).toBeGreaterThan(-1);
        });
    });

    it('should set width', async () => {
        component.width = '50%';
        fixture.detectChanges();
        await fixture.whenStable();
        const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
        expect(plainFieldTexarea.nativeElement.style.width).toBe('50%');

        component.selected = 'code';
        fixture.detectChanges();
        await fixture.whenStable();
        const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
        expect(codeFieldTexarea.nativeElement.style.width).toBe('50%');

        // TODO: We need to find a way to set the width to the wysiwyg
    });

    it('should set height', async () => {
        component.height = '50%';
        fixture.detectChanges();
        await fixture.whenStable();
        const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
        expect(plainFieldTexarea.nativeElement.style.height).toBe('50%');

        component.selected = 'code';
        fixture.detectChanges();
        await fixture.whenStable();
        const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
        expect(codeFieldTexarea.nativeElement.style.height).toBe('50%');

        // TODO: We need to find a way to set the height to the wysiwyg
    });

    it('should add new line character', () => {
        component.propagateChange = (propagateChangeValue) => {
            expect('aaaabbbbbccccc').toEqual(propagateChangeValue);
        };

        const value = 'aaaabbbbbccccc';
        component.onModelChange(value);

        expect(component.value).toEqual(value);
    });

    it('should not repeat characters', () => {
        const value = 'aaaabbbbbcccccddddd';

        component.propagateChange = (propagateChangeValue) => {
            expect('aaaabbbbbcccccddddd').toEqual(propagateChangeValue);
        };

        component.onModelChange(value);

        expect(component.value).toEqual(value);
    });

    it('should not propagate enter keyboard event', async () => {
        const spy = jest.fn();
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

    it('should init editor with the correct value', () => {
        const mockEditor = { test: 'editor' };
        component.editorName = 'testName';
        jest.spyOn(component.monacoInit, 'emit');
        fixture.detectChanges();
        component.onInit(mockEditor);
        expect(component.monacoInit.emit).toHaveBeenCalledWith({
            name: 'testName',
            editor: mockEditor
        });
    });

    describe('code', () => {
        it('should have default options', () => {
            component.show = ['code'];
            fixture.detectChanges();

            const editor: MonacoEditorComponent = de.query(
                By.css('ngx-monaco-editor')
            ).componentInstance;

            expect(editor.options).toEqual({
                theme: 'vs-light',
                minimap: Object({ enabled: false }),
                cursorBlinking: 'solid',
                overviewRulerBorder: false,
                mouseWheelZoom: false,
                lineNumbers: 'on',
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

            const editor: MonacoEditorComponent = de.query(
                By.css('ngx-monaco-editor')
            ).componentInstance;
            expect(editor.options).toEqual({
                theme: 'vs-light',
                minimap: Object({ enabled: false }),
                cursorBlinking: 'solid',
                overviewRulerBorder: false,
                mouseWheelZoom: false,
                lineNumbers: 'on',
                selectionHighlight: false,
                roundedSelection: false,
                selectOnLineNumbers: false,
                columnSelection: false,
                language: 'javascript'
            });
        });
    });
});
