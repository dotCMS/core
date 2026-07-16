import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';

import { DotMessageService, DotSiteService } from '@dotcms/data-access';
import { DotFieldValidationMessageComponent, DotMessagePipe, DotSiteComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentTypeCopyDialogComponent } from './dot-content-type-copy-dialog.component';

import { DotMdIconSelectorComponent } from '../../../../../view/components/_common/dot-md-icon-selector/dot-md-icon-selector.component';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-content-type-copy-dialog [isSaving$]="isSaving$"></dot-content-type-copy-dialog>
    `,
    standalone: false
})
class TestHostComponent {
    isSaving$ = of(false);
}

const formValues = {
    name: 'Name of the copied content type',
    variable: 'variablename',
    folder: '',
    host: '',
    icon: ''
};

describe('DotContentTypeCopyDialogComponent', () => {
    let component: DotContentTypeCopyDialogComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dialog: DebugElement;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.form.label.variable_name': 'Variable Name',
            'contenttypes.form.label.icon': 'Icon',
            'contenttypes.content.copy': 'Copy',
            'contenttypes.content.add_to_bundle.form.cancel': 'Cancel',
            'contenttypes.form.name': 'Name'
        });
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [
                DotContentTypeCopyDialogComponent,
                DotFieldValidationMessageComponent,
                DotMdIconSelectorComponent,
                DotSiteComponent,
                ReactiveFormsModule,
                DotMessagePipe
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: DotSiteService,
                    useValue: {
                        getSites: jest.fn().mockReturnValue(of({}))
                    }
                },
                provideHttpClient(),
                provideHttpClientTesting(),
                provideAnimations()
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-content-type-copy-dialog'));

        component = de.componentInstance;

        dialog = de.query(By.css('p-dialog'));
        component.isVisibleDialog = true;

        fixture.detectChanges();
    });

    it('should have a form', () => {
        const form: DebugElement = de.query(By.css('form'));
        expect(form).not.toBeNull();
        expect(component.form).toBeDefined();
    });

    it('should be invalid if no name was added', () => {
        expect(component.form.valid).toEqual(false);
    });

    it('should call submitForm() when accept button is clicked and form is valid', () => {
        const acceptButton: DebugElement = dialog.query(
            By.css('[data-testId="dotDialogAcceptAction"]')
        );
        expect(acceptButton).toBeDefined();

        component.form.setValue(formValues);
        fixture.detectChanges();

        expect(component.form.valid).toEqual(true);
        jest.spyOn(component, 'submitForm');

        acceptButton.nativeElement.click();

        expect(component.submitForm).toHaveBeenCalledTimes(1);
    });

    it('should be valid and emit form values when accept button is clicked', () => {
        const acceptButton: DebugElement = dialog.query(
            By.css('[data-testId="dotDialogAcceptAction"]')
        );
        expect(acceptButton).toBeDefined();
        component.form.setValue(formValues);
        fixture.detectChanges();

        expect(component.form.valid).toEqual(true);
        jest.spyOn(component.$validFormFields, 'emit');

        acceptButton.nativeElement.click();

        expect(component.$validFormFields.emit).toHaveBeenCalledWith(formValues);
        expect(component.$validFormFields.emit).toHaveBeenCalledTimes(1);
    });

    it('should emit cancelBtn event when cancel button is clicked', () => {
        const cancelButton: DebugElement = dialog.query(
            By.css('[data-testId="dotDialogCancelAction"]')
        );

        expect(cancelButton).toBeDefined();
        jest.spyOn(component, 'closeDialog');
        jest.spyOn(component.$cancelBtn, 'emit');

        cancelButton.nativeElement.click();

        expect(component.closeDialog).toHaveBeenCalledTimes(1);
        expect(component.$cancelBtn.emit).toHaveBeenCalledWith(true);
    });

    it("shouldn't emit form values when accept button is clicked and form is invalid", () => {
        const copyButton: DebugElement = dialog.query(
            By.css('[data-testId="dotDialogAcceptAction"]')
        );
        expect(copyButton).toBeDefined();

        expect(component.form.valid).toEqual(false);
        expect(component.dialogActions.accept.disabled).toEqual(true);

        // Check that button component instance is disabled
        const buttonComponent = copyButton.componentInstance;
        expect(buttonComponent.disabled).toBe(true);

        jest.spyOn(component.$validFormFields, 'emit');

        fixture.detectChanges();

        // Even if clicked programmatically, submitForm checks form validity and won't emit
        copyButton.nativeElement.click();

        expect(component.$validFormFields.emit).not.toHaveBeenCalled();
    });
});
