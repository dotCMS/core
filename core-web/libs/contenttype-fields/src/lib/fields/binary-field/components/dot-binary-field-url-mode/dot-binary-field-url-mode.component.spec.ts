import { expect, it } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';

import { DotBinaryFieldUrlModeComponent } from './dot-binary-field-url-mode.component';

import { CONTENTTYPE_FIELDS_MESSAGE_MOCK } from '../../../../utils/mock';

describe('DotBinaryFieldUrlModeComponent', () => {
    let spectator: Spectator<DotBinaryFieldUrlModeComponent>;

    const createComponent = createComponentFactory({
        component: DotBinaryFieldUrlModeComponent,
        imports: [ButtonModule],
        providers: [
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

        spectator.detectChanges();
    });

    afterEach(() => {
        jest.resetAllMocks();
    });

    it('should have a form with url field', () => {
        expect(spectator.query(byTestId('url-input'))).not.toBeNull();
    });

    describe('actions', () => {
        it('should emit accept event with url value when form is valid', () => {
            const sypAccept = jest.spyOn(spectator.component.accept, 'emit');
            const button = spectator.query('[data-testId="action-import-btn"] button');
            const url = 'http://dotcms.com';

            spectator.component.form.setValue({ url });

            spectator.click(button);
            expect(sypAccept).toHaveBeenCalledWith(url);
        });

        it('should not emit accept event when form is invalid', () => {
            const sypAccept = jest.spyOn(spectator.component.accept, 'emit');
            const button = spectator.query('[data-testId="action-import-btn"] button');
            const url = 'http://dotcms.com';

            spectator.click(button);
            expect(sypAccept).not.toHaveBeenCalledWith(url);
        });

        it('should emit cancel when cancel button is clicked', () => {
            const sypCancel = jest.spyOn(spectator.component.cancel, 'emit');
            const cancelButton = spectator.query('[data-testId="action-cancel-btn"]');

            spectator.click(cancelButton);

            expect(sypCancel).toHaveBeenCalled();
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
            expect(spectator.component.form.valid).toBe(true);
        });
    });

    describe('template', () => {
        it('should show error message when url is invalid', () => {
            const input = spectator.query(byTestId('url-input')) as HTMLInputElement;

            input.focus(); // to trigger touched
            input.value = 'Not a url'; // to trigger invalid
            input.blur(); // to trigger dirty
            spectator.detectChanges();

            expect(spectator.query(byTestId('error-message'))).toBeTruthy();
        });
    });
});
