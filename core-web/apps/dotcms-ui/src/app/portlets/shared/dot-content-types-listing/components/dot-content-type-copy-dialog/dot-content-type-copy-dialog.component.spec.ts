import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotMdIconSelectorModule } from '@components/_common/dot-md-icon-selector/dot-md-icon-selector.module';
import { SiteSelectorFieldModule } from '@components/_common/dot-site-selector-field/dot-site-selector-field.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotEventsService, DotMessageService } from '@dotcms/data-access';
import { CoreWebService, SiteService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { CoreWebServiceMock, MockDotMessageService, SiteServiceMock } from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotFormSelectorModule } from '@portlets/dot-edit-page/content/components/dot-form-selector/dot-form-selector.module';

import { DotContentTypeCopyDialogComponent } from './dot-content-type-copy-dialog.component';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-content-type-copy-dialog [isSaving$]="isSaving$"></dot-content-type-copy-dialog>
    `
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

describe('DotContentTypeCloneDialogComponent', () => {
    const siteServiceMock = new SiteServiceMock();
    let component: DotContentTypeCopyDialogComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dotdialog: DebugElement;

    beforeEach(() => {
        const messageServiceMock = new MockDotMessageService({
            'contenttypes.form.label.variable_name': 'Variable Name',
            'contenttypes.form.label.icon': 'Icon'
        });
        TestBed.configureTestingModule({
            declarations: [DotContentTypeCopyDialogComponent, TestHostComponent],
            imports: [
                DotFormSelectorModule,
                BrowserAnimationsModule,
                DotFieldValidationMessageModule,
                DotMdIconSelectorModule,
                SiteSelectorFieldModule,
                DotDialogModule,
                ReactiveFormsModule,
                DotPipesModule,
                DotMessagePipe,
                HttpClientTestingModule
            ],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: SiteService, useValue: siteServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: DotEventsService,
                    useValue: {
                        listen() {
                            return of([]);
                        }
                    }
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-content-type-copy-dialog'));

        component = de.componentInstance;

        dotdialog = de.query(By.css('dot-dialog'));
        component.isVisibleDialog = true;

        fixture.detectChanges();
    });

    it('should have a form', () => {
        const form: DebugElement = de.query(By.css('form'));
        expect(form).not.toBeNull();
        expect(component.form).toEqual(form.componentInstance.form);
    });

    it('should be invalid if no name was added', () => {
        expect(component.form.valid).toEqual(false);
    });

    it('should be valid and emit form values', () => {
        const acceptButton: DebugElement = dotdialog.query(
            By.css('[data-testId="dotDialogAcceptAction"]')
        );
        expect(acceptButton).toBeDefined();
        component.form.setValue(formValues);
        fixture.detectChanges();

        expect(component.form.valid).toEqual(true);
        spyOn(component.validFormFields, 'emit');

        acceptButton.nativeElement.click();

        expect(component.validFormFields.emit).toHaveBeenCalledWith(formValues);
    });

    it('should call cancelBtn() on cancel button click', () => {
        const cancelButton: DebugElement = dotdialog.query(
            By.css('[data-testId="dotDialogCancelAction"]')
        );

        expect(cancelButton).toBeDefined();
        spyOn(component, 'closeDialog');
        cancelButton.nativeElement.click();

        expect(component.closeDialog).toHaveBeenCalledTimes(1);
        component.cancelBtn.subscribe((res) => {
            expect(res).toEqual(true);
        });
    });

    it('should call submitForm() on Copy button click and form valid', async () => {
        const acceptButton: DebugElement = dotdialog.query(
            By.css('[data-testId="dotDialogAcceptAction"]')
        );
        expect(acceptButton).toBeDefined();

        component.form.setValue(formValues);
        fixture.detectChanges();

        expect(component.form.valid).toEqual(true);
        spyOn(component, 'submitForm');

        acceptButton.nativeElement.click();

        expect(component.submitForm).toHaveBeenCalledTimes(1);
    });

    it("shouldn't call submitForm() on Copy button click and form invalid", () => {
        const copyButton: DebugElement = dotdialog.query(
            By.css('[data-testId="dotDialogAcceptAction"]')
        );
        expect(copyButton).toBeDefined();

        expect(component.form.valid).toEqual(false);
        spyOn(component, 'submitForm');

        fixture.detectChanges();
        copyButton.nativeElement.click();

        expect(component.submitForm).toHaveBeenCalledTimes(0);
    });
});
