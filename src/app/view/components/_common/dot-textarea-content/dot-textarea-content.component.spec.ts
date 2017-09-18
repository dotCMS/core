import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SelectButtonModule, InputTextareaModule } from 'primeng/primeng';

import { DotTextareaContentComponent } from './dot-textarea-content.component';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { AceEditorModule } from 'ng2-ace-editor';
import { TinymceModule } from 'angular2-tinymce';

function cleanOptionText(option) {
    return option.replace(/\r?\n|\r/g, '');
}

describe('DotTextareaContentComponent', () => {
    let component: DotTextareaContentComponent;
    let fixture: ComponentFixture<DotTextareaContentComponent>;
    let de: DebugElement;

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                declarations: [DotTextareaContentComponent],
                imports: [
                    AceEditorModule,
                    SelectButtonModule,
                    InputTextareaModule,
                    TinymceModule.withConfig({})
                ]
            });

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

    it('should have options: plain, code and wysiwyg in the select mode buttons by default', () => {
        fixture.detectChanges();
        const selectFieldWrapper = de.query(
            By.css('.textarea-content__select-field .ui-selectbutton')
        );

        selectFieldWrapper.children.forEach(option => {
            const optionText = cleanOptionText(option.nativeElement.innerText);
            expect(['Plain', 'Code', 'WYSIWYG'].indexOf(optionText)).toBeGreaterThan(-1);
        });
    });

    it('should hide select mode buttons when only one option to show is passed', () => {
        component.show = ['code'];
        fixture.detectChanges();
        const selectField = de.query(By.css('.textarea-content__select-field'));
        expect(selectField == null).toBe(true, 'hide buttons');
    });

    it(
        'should have option \'Plain\' selected by default',
        async(() => {
            fixture.detectChanges();
            /*
                We need to to async and whenStable here because the ngModel in the PrimeNg component
            */
            fixture.whenStable().then(() => {
                fixture.detectChanges();
                const selectedOption = de.query(
                    By.css('.textarea-content__select-field .ui-state-active')
                );
                const defaultOptionText = cleanOptionText(selectedOption.nativeElement.innerText);
                expect(defaultOptionText).toBe('Plain');
            });
        })
    );

    it('should show \'Plain\' field by default', () => {
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

        const wysiwygFieldTexarea = de.query(By.css('.textarea-content__wysiwyg-field'));
        expect(wysiwygFieldTexarea == null).toBe(true, 'hide wysiwyg field');
    });

    it('should show only options we passed in the select mode butons', () => {
        component.show = ['wysiwyg', 'plain'];
        fixture.detectChanges();
        const selectFieldWrapper = de.query(
            By.css('.textarea-content__select-field .ui-selectbutton')
        );
        selectFieldWrapper.children.forEach(option => {
            const optionText = cleanOptionText(option.nativeElement.innerText);
            expect(['Plain', 'WYSIWYG'].indexOf(optionText)).toBeGreaterThan(
                -1,
                `${optionText} exist`
            );
        });
    });

    it('should show only the valid options we passed in the select mode butons', () => {
        component.show = ['code', 'plain', 'sadf', 'hello', 'world'];
        fixture.detectChanges();
        const selectFieldWrapper = de.query(
            By.css('.textarea-content__select-field .ui-selectbutton')
        );
        selectFieldWrapper.children.forEach(option => {
            const optionText = cleanOptionText(option.nativeElement.innerText);
            expect(['Plain', 'Code'].indexOf(optionText)).toBeGreaterThan(
                -1,
                `${optionText} exist`
            );
        });
    });

    it(
        'should show by default the first mode we passed',
        async(() => {
            component.show = ['wysiwyg', 'plain'];
            fixture.detectChanges();
            fixture.whenStable().then(() => {
                const wysiwygFieldTexarea = de.query(By.css('.textarea-content__wysiwyg-field'));
                expect(wysiwygFieldTexarea).toBeTruthy('show wysiwyg field');

                /*
                We should be using .toBeFalsey() but there is a bug with this method:
                https://github.com/angular/angular/issues/14235
            */
                const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
                expect(plainFieldTexarea == null).toBe(true, 'hide plain field');
                const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
                expect(codeFieldTexarea == null).toBe(true, 'hide code field');
            });
        })
    );

    it('should set width', () => {
        component.width = '50%';
        fixture.detectChanges();
        const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
        expect(plainFieldTexarea.nativeElement.style.width).toBe('50%', 'plain width setted');

        component.selected = 'code';
        fixture.detectChanges();
        const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
        expect(codeFieldTexarea.nativeElement.style.width).toBe('50%', 'code width setted');

        // TODO: We need to find a way to set the width to the wysiwyg
    });

    it('should set height', () => {
        component.height = '50%';
        fixture.detectChanges();
        const plainFieldTexarea = de.query(By.css('.textarea-content__plain-field'));
        expect(plainFieldTexarea.nativeElement.style.height).toBe('50%', 'plain height setted');

        component.selected = 'code';
        fixture.detectChanges();
        const codeFieldTexarea = de.query(By.css('.textarea-content__code-field'));
        expect(codeFieldTexarea.nativeElement.style.height).toBe('50%', 'code height setted');

        // TODO: We need to find a way to set the height to the wysiwyg
    });

    it('should have default mode and options in the code editor', () => {
        component.show = ['code'];
        fixture.detectChanges();

        expect(component.ace._mode).toBe('text', 'set mode default');
        expect(component.ace._options).toEqual({}, 'set options default');
    });

    it('should set mode and options in the code editor', () => {
        component.show = ['code'];
        component.code = {
            mode: 'javascript',
            options: {
                cursorStyle: 'ace'
            }
        };
        fixture.detectChanges();

        expect(component.ace._mode).toBe('javascript', 'set mode correctly');
        expect(component.ace._options).toEqual({ cursorStyle: 'ace' }, 'set options correctly');
    });
});
