import { HttpClientModule } from '@angular/common/http';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, NgControl, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotClipboardDirective } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

@Component({
    template: `<form [formGroup]="form">
        <textarea pInputTextarea dotClipboard formControlName="generatedText"></textarea>
    </form>`,
    imports: [
        DotClipboardDirective,
        ReactiveFormsModule,
        InputTextareaModule,
        ButtonModule,
        TooltipModule
    ],
    standalone: true,
    providers: [DotMessageService]
})
class TestHostComponent {
    form: FormGroup = new FormGroup({
        generatedText: new FormControl('')
    });
}

const messageServiceMock = new MockDotMessageService({
    copied: 'Copied'
});

describe('DotClipboardDirective', () => {
    let fixture: ComponentFixture<TestHostComponent>;
    let component: TestHostComponent;
    let element: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [DotClipboardDirective, TestHostComponent, HttpClientModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }, NgControl]
        });
        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.componentInstance;
        element = fixture.debugElement;
    });

    // it('should hide copy button when input value is empty', () => {
    //     component.form.patchValue({ generatedText: null });
    //     fixture.detectChanges();
    //
    //     const copyButton = element.query(By.css('.p-button.hidden'));
    //     expect(copyButton).not.toBeNull();
    // });

    it('should show copy button when input value is not empty', () => {
        component.form.patchValue({ generatedText: 'non-empty value' });
        fixture.detectChanges();

        const copyButton = element.query(By.css('.p-button:not(.hidden)'));
        expect(copyButton).toBeTruthy();
    });

    it('should copy input value to clipboard on button click', () => {
        const clipboardSpy = spyOn(navigator.clipboard, 'writeText');
        component.form.patchValue({ generatedText: 'non-empty value' });
        fixture.detectChanges();

        const copyButton = element.query(By.css('.p-button:not(.hidden)'));
        copyButton.nativeElement.click();
        expect(clipboardSpy).toHaveBeenCalledWith('non-empty value');
    });
});
