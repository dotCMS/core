import { expect, it } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';

import { DotBinaryFieldUrlModeComponent } from './dot-binary-field-url-mode.component';
import { DotBinaryFieldUrlModeStore } from './store/dot-binary-field-url-mode.store';

import { DotBinaryFieldValidatorService } from '../../service/dot-binary-field-validator/dot-binary-field-validator.service';
import { TEMP_FILE_MOCK } from '../../store/binary-field.store.spec';
import { CONTENTTYPE_FIELDS_MESSAGE_MOCK } from '../../utils/mock';

describe('DotBinaryFieldUrlModeComponent', () => {
    let spectator: Spectator<DotBinaryFieldUrlModeComponent>;
    let component: DotBinaryFieldUrlModeComponent;

    let store: DotBinaryFieldUrlModeStore;

    const createComponent = createComponentFactory({
        component: DotBinaryFieldUrlModeComponent,
        imports: [ButtonModule],
        componentProviders: [DotBinaryFieldUrlModeStore],
        providers: [
            DotBinaryFieldValidatorService,
            {
                provide: DotUploadService,
                useValue: {
                    uploadFile: ({ file }) => {
                        return new Promise((resolve) => {
                            if (file) {
                                resolve(TEMP_FILE_MOCK);
                            }
                        });
                    }
                }
            },
            {
                provide: DotMessageService,
                useValue: CONTENTTYPE_FIELDS_MESSAGE_MOCK
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        component = spectator.component;
        store = spectator.inject(DotBinaryFieldUrlModeStore, true);
        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should have a form with url field', () => {
        expect(spectator.query(byTestId('url-input'))).not.toBeNull();
    });

    it('should have a button to import', () => {
        expect(spectator.query(byTestId('import-button'))).not.toBeNull();
    });

    describe('Actions', () => {
        it('should upload file by url form when click on import button', async () => {
            const spy = jest.spyOn(component.tempFileUploaded, 'emit');
            const spyUploadFileByUrl = jest.spyOn(store, 'uploadFileByUrl');
            const importButton = spectator.query('[data-testId="import-button"] button');
            const form = spectator.component.form;

            form.setValue({ url: 'http://dotcms.com' });
            spectator.click(importButton);

            expect(spy).toHaveBeenCalledWith(TEMP_FILE_MOCK);
            expect(spectator.component.form.valid).toBeTruthy();
            expect(spyUploadFileByUrl).toHaveBeenCalled();
        });

        it('should cancel when click on cancel button', () => {
            const spyCancel = jest.spyOn(spectator.component.cancel, 'emit');
            const cancelButton = spectator.query('[data-testId="cancel-button"] button');

            spectator.click(cancelButton);

            expect(spyCancel).toHaveBeenCalled();
        });

        it('should show loading button when isLoading', async () => {
            store.setIsLoading(true);
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            const loadingButton = spectator.query(byTestId('loading-button'));
            const importButton = spectator.query(byTestId('import-button'));

            expect(loadingButton).toBeTruthy();
            expect(importButton).not.toBeTruthy();
        });
    });

    describe('validation', () => {
        it('should be invalid when url is empty', () => {
            spectator.component.form.setValue({ url: '' });
            expect(spectator.component.form.valid).toBe(false);
        });

        it('should be invalid when url is not valid', () => {
            spectator.component.form.setValue({ url: 'Not a url' });
            expect(spectator.component.form.valid).toBe(false);
        });

        it('should be valid when url is valid', () => {
            spectator.component.form.setValue({ url: 'http://dotcms.com' });
            expect(spectator.component.form.valid).toBeTruthy();
        });

        it('should show error when value is empty and user is trying to upload file ', async () => {
            const button = spectator.query('[data-testId="import-button"] button');
            const form = spectator.component.form;

            form.setValue({ url: '' });
            spectator.click(button);
            spectator.detectChanges();
            await spectator.fixture.whenStable();

            const fieldMessage = spectator.fixture.debugElement.query(
                By.css('dot-field-validation-message')
            );
            const error = fieldMessage.componentInstance.defaultMessage;

            expect(spectator.component.form.invalid).toBeTruthy();
            expect(error).toBe('The URL you requested is not valid. Please try again.');
        });
    });

    describe('template', () => {
        it('should show error message when url is invalid', () => {
            const input = spectator.query<HTMLInputElement>(byTestId('url-input'));

            input.focus(); // to trigger touched
            input.value = 'Not a url'; // to trigger invalid
            input.blur(); // to trigger dirty

            expect(spectator.query(byTestId('error-message'))).toBeTruthy();
        });
    });
});
