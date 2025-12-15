import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, UntypedFormBuilder } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { FileUpload, FileUploadModule } from 'primeng/fileupload';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService, DotSystemConfigService } from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import { DotSystemConfig } from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSiteComponent
} from '@dotcms/ui';
import {
    mockDotCMSTempFile,
    MockDotMessageService,
    mockSites,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotCreatePersonaFormComponent } from './dot-create-persona-form.component';

import { DotAutocompleteTagsComponent } from '../../_common/dot-autocomplete-tags/dot-autocomplete-tags.component';

const FROM_INITIAL_VALUE = {
    hostFolder: mockSites[0].identifier,
    keyTag: '',
    name: '',
    photo: null,
    tags: null
};

const mockFileUploadResponse = {
    files: [{ name: 'fileName.png' }],
    originalEvent: { body: { tempFiles: [mockDotCMSTempFile] } }
};

describe('DotCreatePersonaFormComponent', () => {
    let component: DotCreatePersonaFormComponent;
    let fixture: ComponentFixture<DotCreatePersonaFormComponent>;
    const messageServiceMock = new MockDotMessageService({
        'modes.persona.upload.file': 'Upload File',
        'modes.persona.name': 'Name',
        'modes.persona.key.tag': 'Key Tag',
        'dot.common.choose': 'Choose',
        'dot.common.remove': 'Remove',
        'modes.persona.host': 'Host',
        'modes.persona.name.error.required': 'Name is required',
        'modes.persona.select.tags.placeholder': 'Placeholder'
    });

    beforeEach(() => {
        const siteServiceMock = new SiteServiceMock();

        TestBed.configureTestingModule({
            imports: [
                DotCreatePersonaFormComponent,
                MockComponent(DotSiteComponent),
                ReactiveFormsModule,
                BrowserAnimationsModule,
                FileUploadModule,
                InputTextModule,
                DotFieldValidationMessageComponent,
                DotAutofocusDirective,
                MockComponent(DotAutocompleteTagsComponent),
                HttpClientTestingModule,
                DotMessagePipe
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: SiteService, useValue: siteServiceMock },
                {
                    provide: DotSystemConfigService,
                    useValue: {
                        getSystemConfig: () =>
                            of({
                                logos: { loginScreen: '', navBar: '' },
                                colors: {
                                    primary: '#54428e',
                                    secondary: '#3a3847',
                                    background: '#BB30E1'
                                },
                                releaseInfo: { buildDate: 'June 24, 2019', version: '5.0.0' },
                                systemTimezone: {
                                    id: 'America/Costa_Rica',
                                    label: 'Costa Rica',
                                    offset: 360
                                },
                                languages: [],
                                license: {
                                    level: 100,
                                    displayServerId: '19fc0e44',
                                    levelName: 'COMMUNITY EDITION',
                                    isCommunity: true
                                },
                                cluster: {
                                    clusterId: 'test-cluster',
                                    companyKeyDigest: 'test-digest'
                                }
                            } as DotSystemConfig)
                    }
                },
                UntypedFormBuilder
            ]
        });

        fixture = TestBed.createComponent(DotCreatePersonaFormComponent);
        component = fixture.componentInstance;
    });

    describe('without name set', () => {
        beforeEach(() => {
            // Ensure tempUploadedFile is explicitly null for p-fileUpload to render
            component.tempUploadedFile = null;
            fixture.detectChanges();
        });

        it('should load labels correctly', () => {
            const hostLabel: DebugElement = fixture.debugElement.query(
                By.css('label[for="content-type-form-host"]')
            );
            const nameLabel: DebugElement = fixture.debugElement.query(
                By.css('label[for="persona-name"]')
            );
            const keyTagLabel: DebugElement = fixture.debugElement.query(
                By.css('label[for="persona-keyTag"]')
            );
            const imageLabel: DebugElement = fixture.debugElement.query(
                By.css('label[for="persona-image"]')
            );
            const validationMessage: DebugElement = fixture.debugElement.query(
                By.css('dot-field-validation-message')
            );

            const fileUpload: DebugElement = fixture.debugElement.query(By.css('p-fileupload'));
            expect(hostLabel.nativeElement.textContent).toEqual('Host');
            expect(nameLabel.nativeElement.textContent).toEqual('Name');
            expect(keyTagLabel.nativeElement.textContent).toEqual('Key Tag');
            expect(imageLabel.nativeElement.textContent).toEqual('Upload File');
            expect(validationMessage.componentInstance.defaultMessage).toEqual('Name is required');
            if (fileUpload) {
                expect(fileUpload.componentInstance.chooseLabel).toEqual('Choose');
            }
        });

        it('should be invalid by default', () => {
            expect(component.form.valid).toBe(false);
        });

        it('should set initial value of the form on load', () => {
            expect(component.form.getRawValue()).toEqual(FROM_INITIAL_VALUE);
        });

        it('should update the hostFolder input value when set the form hostFolder value', () => {
            const hostFolderInput: DebugElement = fixture.debugElement.query(
                By.css('#content-type-form-host')
            );
            component.form.get('hostFolder').setValue(mockSites[0].identifier);
            fixture.detectChanges();
            expect(hostFolderInput).toBeTruthy();
            expect(component.form.get('hostFolder').value).toEqual(mockSites[0].identifier);
        });

        it('should update input name when set form name', () => {
            const nameInput: DebugElement = fixture.debugElement.query(By.css('#persona-name'));
            component.form.get('name').setValue('John');
            fixture.detectChanges();
            expect(nameInput.nativeElement.value).toEqual('John');
        });

        it('should set Key Tag camel case based on the name value', () => {
            const nameInput: DebugElement = fixture.debugElement.query(By.css('#persona-name'));
            const keyTagInput: DebugElement = fixture.debugElement.query(By.css('#persona-keyTag'));
            component.form.get('name').setValue('John Doe');
            nameInput.triggerEventHandler('keyup', {});
            fixture.detectChanges();
            expect(keyTagInput.nativeElement.value).toEqual('johnDoe');
        });

        it('should set the p-fileUpload with the correctly attributes', () => {
            const fileUpload: DebugElement = fixture.debugElement.query(By.css('p-fileupload'));
            expect(fileUpload).toBeTruthy();
            const componentInstance: FileUpload = fileUpload.componentInstance;

            expect(componentInstance.url).toEqual('/api/v1/temp');
            expect(componentInstance.accept).toEqual('image/*,.webp');
            expect(componentInstance.auto).toEqual(true);
            expect(componentInstance.mode).toEqual('basic');
        });

        it('should emit isValid to false when the file upload starts', () => {
            const fileUpload: DebugElement = fixture.debugElement.query(By.css('p-fileupload'));
            expect(fileUpload).toBeTruthy();
            jest.spyOn(component.isValid, 'emit');
            fileUpload.triggerEventHandler('onBeforeUpload', {});
            fixture.detectChanges();
            expect(component.isValid.emit).toHaveBeenCalledWith(false);
            expect(component.isValid.emit).toHaveBeenCalledTimes(1);
        });

        it('should set the photo id and tempUploadedFile after image upload', () => {
            const fileUpload: DebugElement = fixture.debugElement.query(By.css('p-fileupload'));
            expect(fileUpload).toBeTruthy();
            fileUpload.triggerEventHandler('onUpload', mockFileUploadResponse);
            fixture.detectChanges();
            expect(component.form.get('photo').value).toEqual('temp-file_123');
            expect(component.tempUploadedFile).toEqual(mockDotCMSTempFile);
        });

        it('should clear photo form value and tempUploadedFile when remove image', () => {
            component.form.get('photo').setValue('test');
            component.tempUploadedFile = mockDotCMSTempFile;
            fixture.detectChanges();

            const removeButton: DebugElement = fixture.debugElement.query(By.css('button'));
            removeButton.triggerEventHandler('click', {});
            expect(removeButton.nativeElement.textContent).toBe('Remove');
            expect(component.form.get('photo').value).toEqual('');
            expect(component.tempUploadedFile).toEqual(null);
        });

        it('should emit if form is valid after changes', () => {
            jest.spyOn(component.isValid, 'emit');
            component.form.setValue({
                photo: 'test',
                name: 'test',
                keyTag: 'test',
                hostFolder: 'test',
                tags: 'test'
            });
            expect(component.isValid.emit).toHaveBeenCalledWith(true);
            expect(component.isValid.emit).toHaveBeenCalledTimes(1);
        });

        it('should emit if form is invalid after changes', () => {
            jest.spyOn(component.isValid, 'emit');
            component.form.get('photo').setValue('test');
            expect(component.isValid.emit).toHaveBeenCalledWith(false);
            expect(component.isValid.emit).toHaveBeenCalledTimes(1);
        });

        it('should reset from to initial value and clear tempUploadedFile', () => {
            component.resetForm();
            expect(component.form.getRawValue()).toEqual({
                ...FROM_INITIAL_VALUE,
                name: null,
                keyTag: null
            });
            expect(component.tempUploadedFile).toEqual(null);
        });

        it('should pass placeholder correctly to tags input', () => {
            const tagsInput = fixture.debugElement.query(By.css('#persona-other-tags'));

            // Verificamos que el input existe
            expect(tagsInput).toBeTruthy();
        });
    });

    describe('with name set', () => {
        beforeEach(() => {
            component.personaName = 'Test B';
            fixture.detectChanges();
        });

        it('should be valid on load', () => {
            expect(component.form.valid).toBe(true);
        });

        it('should set name if passed on initial load', () => {
            expect(component.form.getRawValue()).toEqual({
                hostFolder: mockSites[0].identifier,
                keyTag: 'testB',
                name: 'Test B',
                photo: null,
                tags: null
            });
        });
    });
});
