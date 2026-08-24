import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUploadButtonComponent } from './dot-upload-button.component';

describe('DotUploadButtonComponent', () => {
    let spectator: Spectator<DotUploadButtonComponent>;

    const createComponent = createComponentFactory({
        component: DotUploadButtonComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.upload': 'Upload',
                    'content-drive.upload-asset': 'Upload Asset',
                    'content-drive.upload-file': 'Upload File'
                })
            }
        ]
    });

    const getButton = () =>
        spectator.query(byTestId('upload-button'))?.querySelector('button') as HTMLButtonElement;

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('label', () => {
        it('should fall back to the generic label when no base type is pinned', () => {
            expect(getButton().textContent?.trim()).toBe('Upload');
        });

        it('should read "Upload Asset" for dotAsset folders', () => {
            spectator.setInput('defaultBaseType', DotCMSBaseTypesContentTypes.DOTASSET);
            spectator.detectChanges();

            expect(getButton().textContent?.trim()).toBe('Upload Asset');
        });

        it('should read "Upload File" for fileAsset folders', () => {
            spectator.setInput('defaultBaseType', DotCMSBaseTypesContentTypes.FILEASSET);
            spectator.detectChanges();

            expect(getButton().textContent?.trim()).toBe('Upload File');
        });

        it('should match the base type case-insensitively', () => {
            spectator.setInput('defaultBaseType', 'dotasset');
            spectator.detectChanges();

            expect(getButton().textContent?.trim()).toBe('Upload Asset');
        });

        it('should fall back to the generic label for an unknown base type', () => {
            spectator.setInput('defaultBaseType', 'WIDGET');
            spectator.detectChanges();

            expect(getButton().textContent?.trim()).toBe('Upload');
        });
    });

    describe('disabled', () => {
        it('should be enabled by default', () => {
            expect(getButton().disabled).toBe(false);
        });

        it('should disable the button when requested', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            expect(getButton().disabled).toBe(true);
        });
    });

    describe('output', () => {
        it('should emit the originating click', () => {
            const handler = jest.fn();
            spectator.output('upload').subscribe(handler);

            spectator.click(getButton());

            expect(handler).toHaveBeenCalledTimes(1);
            expect(handler.mock.calls[0][0]).toBeInstanceOf(Event);
        });
    });
});
