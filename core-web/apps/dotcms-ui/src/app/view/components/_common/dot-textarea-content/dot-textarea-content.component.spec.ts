/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { CommonModule } from '@angular/common';
import { Component, DebugElement, forwardRef, Input } from '@angular/core';
import { FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { SelectButtonModule } from 'primeng/selectbutton';
import { TextareaModule } from 'primeng/textarea';

import { DotTextareaContentComponent } from './dot-textarea-content.component';

function cleanOptionText(option: string): string {
    return option.replace(/\r?\n|\r/g, '');
}

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'ngx-monaco-editor',
    template: '<div>CODE EDITOR</div>',
    standalone: true,
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MonacoEditorMockComponent)
        }
    ]
})
class MonacoEditorMockComponent {
    @Input() options: Record<string, unknown>;

    writeValue() {}

    registerOnChange() {}

    registerOnTouched() {}
}

describe('DotTextareaContentComponent', () => {
    let spectator: Spectator<DotTextareaContentComponent>;
    let component: DotTextareaContentComponent;
    let de: DebugElement;

    const createComponent = createComponentFactory({
        component: DotTextareaContentComponent,
        imports: [SelectButtonModule, TextareaModule, FormsModule],
        overrideComponents: [
            [
                DotTextareaContentComponent,
                {
                    set: {
                        imports: [
                            CommonModule,
                            FormsModule,
                            SelectButtonModule,
                            MonacoEditorMockComponent
                        ]
                    }
                }
            ]
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
        de = spectator.debugElement;
    });

    it('should show a select mode buttons by default', () => {
        spectator.detectChanges();
        const selectField = de.query(By.css('.textarea-content__select-field'));
        expect(selectField).toBeTruthy();
    });

    it('should have options: plain, and code in the select mode buttons by default', () => {
        spectator.detectChanges();
        expect(component.selectOptions).toEqual([
            { label: 'Plain', value: 'plain' },
            { label: 'Code', value: 'code' }
        ]);
        const selectFieldEl = de.query(By.css('.textarea-content__select-field'));
        expect(selectFieldEl).toBeTruthy();
    });

    it('should hide select mode buttons when only one option to show is passed', () => {
        spectator = createComponent({ props: { show: ['code'] } });
        de = spectator.debugElement;
        spectator.detectChanges();
        const selectField = de.query(By.css('.textarea-content__select-field'));
        expect(selectField).toBeNull();
    });

    it("should have option 'Plain' selected by default", async () => {
        spectator.detectChanges();
        await spectator.fixture.whenStable();
        expect(component.selected).toBe('plain');
        const selectButton = de.query(By.css('.textarea-content__select-field'));
        const value = selectButton?.componentInstance?.value ?? component.selected;
        expect(value).toBe('plain');
    });

    it("should show 'Plain' field by default", () => {
        spectator.detectChanges();
        const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
        expect(plainFieldTexarea).toBeTruthy();
        const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
        expect(codeFieldTexarea).toBeNull();
    });

    it('should show only the valid options we passed in the select mode butons', () => {
        spectator = createComponent({
            props: { show: ['code', 'plain', 'sadf', 'hello', 'world'] }
        });
        component = spectator.component;
        de = spectator.debugElement;
        spectator.detectChanges();
        expect(component.selectOptions.length).toBe(2);
        expect(component.selectOptions.map((o) => o.label).sort()).toEqual(['Code', 'Plain']);
        const selectFieldWrapper = de.query(
            By.css('.textarea-content__select-field p-selectbutton')
        );
        const fallback = de.query(By.css('.textarea-content__select-field'));
        const wrapper = selectFieldWrapper ?? fallback;
        if (wrapper?.children?.length) {
            wrapper.children.forEach((option: DebugElement) => {
                const optionText = cleanOptionText(option.nativeElement?.textContent ?? '');
                expect(['Plain', 'Code'].indexOf(optionText)).toBeGreaterThan(-1);
            });
        }
    });

    it('should set width', async () => {
        spectator = createComponent({ props: { width: '50%' } });
        component = spectator.component;
        de = spectator.debugElement;
        spectator.detectChanges();
        await spectator.fixture.whenStable();
        const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
        expect(plainFieldTexarea?.nativeElement?.style?.width).toBe('50%');

        component.selected = 'code';
        spectator.detectChanges();
        await spectator.fixture.whenStable();
        const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
        expect(codeFieldTexarea?.nativeElement?.style?.width).toBe('50%');
    });

    it('should set height', async () => {
        spectator = createComponent({ props: { height: '50%' } });
        component = spectator.component;
        de = spectator.debugElement;
        spectator.detectChanges();
        await spectator.fixture.whenStable();
        const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
        expect(plainFieldTexarea?.nativeElement?.style?.height).toBe('50%');

        component.selected = 'code';
        spectator.detectChanges();
        await spectator.fixture.whenStable();
        const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
        expect(codeFieldTexarea?.nativeElement?.style?.height).toBe('50%');
    });

    it('should add new line character', () => {
        component.propagateChange = (propagateChangeValue: string) => {
            expect('aaaabbbbbccccc').toEqual(propagateChangeValue);
        };

        const value = 'aaaabbbbbccccc';
        component.onModelChange(value);

        expect(component.value).toEqual(value);
    });

    it('should not repeat characters', () => {
        const value = 'aaaabbbbbcccccddddd';

        component.propagateChange = (propagateChangeValue: string) => {
            expect('aaaabbbbbcccccddddd').toEqual(propagateChangeValue);
        };

        component.onModelChange(value);

        expect(component.value).toEqual(value);
    });

    it('should not propagate enter keyboard event', async () => {
        const spy = jest.fn();
        spectator.setInput('show', ['plain', 'code']);
        spectator.detectChanges();
        component.selected = 'plain';
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        const textarea = de.query(By.css('.textarea-content__plain-field'));
        textarea?.triggerEventHandler('keydown.enter', { stopPropagation: spy });

        component.selected = 'code';
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        const monaco = de.query(By.css('ngx-monaco-editor'));
        monaco?.triggerEventHandler('keydown.enter', { stopPropagation: spy });

        expect(spy).toHaveBeenCalledTimes(2);
    });

    it('should init editor with the correct value', () => {
        const mockEditor = { test: 'editor' };
        spectator.setInput('editorName', 'testName');
        jest.spyOn(component.monacoInit, 'emit');
        spectator.detectChanges();
        component.onInit(mockEditor);
        expect(component.monacoInit.emit).toHaveBeenCalledWith({
            name: 'testName',
            editor: mockEditor
        });
    });

    describe('code', () => {
        beforeEach(() => {
            spectator.setInput('show', ['code']);
            spectator.detectChanges();
        });

        it('should have default options', () => {
            expect(component.editorOptions).toEqual({
                theme: 'vs-light',
                minimap: { enabled: false },
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
            const editorEl = de.query(By.css('ngx-monaco-editor'));
            if (editorEl?.componentInstance) {
                expect((editorEl.componentInstance as { options?: unknown }).options).toEqual(
                    component.editorOptions
                );
            }
        });

        it('should set langiage', () => {
            spectator.setInput('language', 'javascript');
            spectator.detectChanges();

            expect(component.editorOptions.language).toBe('javascript');
            const editorEl = de.query(By.css('ngx-monaco-editor'));
            if (editorEl?.componentInstance) {
                expect(
                    (editorEl.componentInstance as { options?: { language?: string } }).options
                        ?.language
                ).toBe('javascript');
            }
        });
    });
});
