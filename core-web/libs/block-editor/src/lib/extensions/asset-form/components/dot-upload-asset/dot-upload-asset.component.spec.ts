import { of } from 'rxjs';

import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { By, DomSanitizer } from '@angular/platform-browser';

import { delay } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotUploadAssetComponent, STATUS } from './dot-upload-asset.component';

import { DotUploadFileService } from '../../../../shared';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'p-fileupload',
    template: '<input type="file">'
})
class FileUploadMockComponent {
    @Output()
    // eslint-disable-next-line @angular-eslint/no-output-on-prefix
    onSelect: EventEmitter<File> = new EventEmitter();

    @Input()
    chooseLabel = '';
    @Input()
    mode = '';
    @Input()
    accept = '';
    @Input()
    maxFileSize = '';
    @Input()
    customUpload = true;
}

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dot-spinner',
    template: '<input type="file">'
})
class DotSpinnerMockComponent {}

describe('DotUploadAssetComponent', () => {
    let fixture: ComponentFixture<DotUploadAssetComponent>;
    let de: DebugElement;
    let component: DotUploadAssetComponent;

    let dotUploadFileService: DotUploadFileService;
    let domSanitizer: DomSanitizer;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotUploadAssetComponent,
                FileUploadMockComponent,
                DotSpinnerMockComponent
            ],
            providers: [
                {
                    provide: DotUploadFileService,
                    useValue: {
                        publishContent: jest.fn().mockReturnValue(of())
                    }
                },
                {
                    provide: DomSanitizer,
                    useValue: {
                        bypassSecurityTrustUrl: jest.fn()
                    }
                }
            ]
        }).compileComponents();

        dotUploadFileService = TestBed.inject(DotUploadFileService);
        domSanitizer = TestBed.inject(DomSanitizer);
        fixture = TestBed.createComponent(DotUploadAssetComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
    });

    test('should create', () => {
        expect(component).toBeTruthy();
    });

    describe('Select File', () => {
        test('should show the p-fileupload when status is "SELECT"', () => {
            component.status = STATUS.SELECT;
            fixture.detectChanges();
            const element = de.query(By.css('p-fileupload'));

            expect(element).toBeTruthy();
        });

        test('should show the image and change the status to "PREVIEW" when a image is selected', () => {
            const fileMock = new File([''], 'filename', { type: 'image/png' });
            global.URL.createObjectURL = jest.fn();

            jest.spyOn(component, 'onSelectFile');

            fixture.detectChanges();
            const element = de.query(By.css('p-fileupload'));
            element.triggerEventHandler('onSelect', { files: [fileMock] });

            fixture.detectChanges();

            const img = de.query(By.css('img'));

            expect(component.onSelectFile).toHaveBeenCalled();
            expect(component.status).toEqual(STATUS.PREVIEW);
            expect(component.file).toEqual(fileMock);
            expect(img).toBeTruthy();

            // Should Call the Sanitizer
            expect(domSanitizer.bypassSecurityTrustUrl).toHaveBeenCalled();
            expect(global.URL.createObjectURL).toHaveBeenCalled();
        });
    });

    describe('Go Back', () => {
        beforeEach(() => {
            const fileMock = new File([''], 'filename', { type: 'image/png' });
            component.file = fileMock;
            component.status = STATUS.PREVIEW;
            fixture.detectChanges();
        });

        test('should remove the current file and go back to the "SELECT Status', () => {
            // Click on Back Button
            const btn = de.query(By.css('[data-test-id="back-btn"]'));
            btn.triggerEventHandler('click');

            fixture.detectChanges();

            const element = de.query(By.css('p-fileupload'));

            expect(component.file).toBeNull();
            expect(component.status).toEqual(STATUS.SELECT);
            expect(element).toBeTruthy();
        });
    });

    describe('Upload File', () => {
        beforeEach(() => {
            const fileMock = new File([''], 'filename', { type: 'image/png' });
            component.file = fileMock;
            component.status = STATUS.PREVIEW;
            fixture.detectChanges();
        });

        test('should remove the current file and go upload to the "SELECT Status', fakeAsync(() => {
            const dotContentlet = {
                asset: 'https://media.istockphoto.com/vectors/costa-rica-vector-id652225694?s=170667a',
                mimeType: 'image/png',
                name: 'costarica.png',
                icon: 'inventory_2',
                url: '/inventory/product-in-the-store',
                path: '/inventory/product-in-the-store',
                variable: 'inventory',
                title: 'Cras ornare tristique elit.',
                inode: '1213',
                image: 'https://images.unsplash.com/photo-1433883669848-fa8a7ce070b2?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2988&q=80',
                languageId: 2,
                modDate: '2021-10-20 14:56:53.052',
                baseType: 'CONTENT',
                archived: false,
                working: true,
                locked: false,
                live: true,
                identifier: 'f1d378c9-b784-45d0-a43c-9790af678f13',
                titleImage: 'image',
                hasLiveVersion: true,
                folder: 'SYSTEM_FOLDER',
                hasTitleImage: true,
                __icon__: 'contentIcon',
                contentTypeIcon: 'file_copy',
                contentType: 'Blog'
            };
            const data: unknown = [
                {
                    cd769844de530f7b5d3434b1b5cfdd62: dotContentlet
                }
            ];

            jest.spyOn(dotUploadFileService, 'publishContent').mockReturnValue(
                of(data as DotCMSContentlet[]).pipe(delay(500))
            );
            const emitSpy = jest.spyOn(component.uploadedFile, 'emit');

            // Click on Upload Button
            const btn = de.query(By.css('[data-test-id="upload-btn"]'));
            const btnBack = de.query(By.css('[data-test-id="back-btn"]'));
            btn.triggerEventHandler('click');

            fixture.detectChanges();
            expect(component.status).toEqual(STATUS.UPLOAD);
            expect(btn.nativeElement.disabled).toBeTruthy();
            expect(btnBack.nativeElement.disabled).toBeTruthy();

            tick(500);
            fixture.detectChanges();

            expect(emitSpy).toHaveBeenCalledWith(dotContentlet);
            expect(component.status).toEqual(STATUS.SELECT);
        }));

        test('should show loading when the status is "UPLOAD"', () => {
            component.status = STATUS.UPLOAD;
            fixture.detectChanges();

            const spinner = de.query(By.css('dot-spinner'));
            expect(spinner).toBeTruthy();
        });
    });
});
