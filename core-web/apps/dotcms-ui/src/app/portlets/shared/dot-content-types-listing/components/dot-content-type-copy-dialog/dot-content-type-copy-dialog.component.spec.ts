import { Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotEventsService, DotMessageService, DotSiteService, DotSystemConfigService } from '@dotcms/data-access';
import { DotSystemConfig } from '@dotcms/dotcms-models';
import {
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSafeHtmlPipe,
    DotSiteComponent
} from '@dotcms/ui';
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

const mockSystemConfig: DotSystemConfig = {
    logos: { loginScreen: '', navBar: '' },
    colors: { primary: '#54428e', secondary: '#3a3847', background: '#BB30E1' },
    releaseInfo: { buildDate: 'June 24, 2019', version: '5.0.0' },
    systemTimezone: { id: 'America/Costa_Rica', label: 'Costa Rica', offset: 360 },
    languages: [],
    license: {
        level: 100,
        displayServerId: '19fc0e44',
        levelName: 'COMMUNITY EDITION',
        isCommunity: true
    },
    cluster: { clusterId: 'test-cluster', companyKeyDigest: 'test-digest' }
};

class MockDotSystemConfigService {
    getSystemConfig(): Observable<DotSystemConfig> {
        return of(mockSystemConfig);
    }
}

describe('DotContentTypeCloneDialogComponent', () => {
    let component: DotContentTypeCopyDialogComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dotdialog: DebugElement;

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
                BrowserAnimationsModule,
                DotFieldValidationMessageComponent,
                DotMdIconSelectorComponent,
                DotSiteComponent,
                ReactiveFormsModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                HttpClientTestingModule
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotSystemConfigService, useClass: MockDotSystemConfigService },
                {
                    provide: DotEventsService,
                    useValue: {
                        listen() {
                            return of([]);
                        }
                    }
                },
                {
                    provide: DotSiteService,
                    useValue: {
                        getSites: jest.fn().mockReturnValue(of({}))
                    }
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-content-type-copy-dialog'));

        component = de.componentInstance;

        dotdialog = de.query(By.css('p-dialog'));
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

    it('should be valid and emit form values', () => {
        const acceptButton: DebugElement = dotdialog.query(
            By.css('[data-testId="dotDialogAcceptAction"]')
        );
        expect(acceptButton).toBeDefined();
        component.form.setValue(formValues);
        fixture.detectChanges();

        expect(component.form.valid).toEqual(true);
        jest.spyOn(component.validFormFields, 'emit');

        acceptButton.nativeElement.click();

        expect(component.validFormFields.emit).toHaveBeenCalledWith(formValues);
        expect(component.validFormFields.emit).toHaveBeenCalledTimes(1);
    });

    it('should call cancelBtn() on cancel button click', () => {
        const cancelButton: DebugElement = dotdialog.query(
            By.css('[data-testId="dotDialogCancelAction"]')
        );

        expect(cancelButton).toBeDefined();
        jest.spyOn(component, 'closeDialog');
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
        jest.spyOn(component, 'submitForm');

        acceptButton.nativeElement.click();

        expect(component.submitForm).toHaveBeenCalledTimes(1);
    });

    it("shouldn't call submitForm() on Copy button click and form invalid", () => {
        const copyButton: DebugElement = dotdialog.query(
            By.css('[data-testId="dotDialogAcceptAction"]')
        );
        expect(copyButton).toBeDefined();

        expect(component.form.valid).toEqual(false);
        expect(component.dialogActions.accept.disabled).toEqual(true);

        // Check that button component instance is disabled
        const buttonComponent = copyButton.componentInstance;
        expect(buttonComponent.disabled).toBe(true);

        jest.spyOn(component.validFormFields, 'emit');

        fixture.detectChanges();

        // Even if clicked programmatically, submitForm checks form validity and won't emit
        copyButton.nativeElement.click();

        expect(component.validFormFields.emit).not.toHaveBeenCalled();
    });
});
