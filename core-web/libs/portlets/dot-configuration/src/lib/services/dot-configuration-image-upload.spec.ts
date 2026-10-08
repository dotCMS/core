import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { Subject, of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { FileSelectEvent, FileUpload } from 'primeng/fileupload';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { DotConfigurationAssetService } from './dot-configuration-asset.service';
import { injectImageUpload } from './dot-configuration-image-upload';

@Injectable()
class UploadHost {
    readonly imageUpload = injectImageUpload();
}

describe('injectImageUpload', () => {
    let spectator: SpectatorService<UploadHost>;
    let picker: FileUpload;
    let onUploaded: ReturnType<typeof vi.fn>;

    const file = new File(['<svg/>'], 'brand.svg', { type: 'image/svg+xml' });
    const selection = (...files: File[]) => ({ currentFiles: files }) as unknown as FileSelectEvent;

    const createService = createServiceFactory({
        service: UploadHost,
        providers: [
            mockProvider(DotConfigurationAssetService, {
                uploadImage: vi.fn().mockReturnValue(of('/dA/new-id/asset/brand.svg'))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() })
        ]
    });

    beforeEach(() => {
        vi.clearAllMocks();
        spectator = createService();
        picker = { clear: vi.fn() } as unknown as FileUpload;
        onUploaded = vi.fn();
    });

    it('uploads the picked file and hands over its path', () => {
        spectator.service.imageUpload.upload(selection(file), picker, onUploaded);

        expect(spectator.inject(DotConfigurationAssetService).uploadImage).toHaveBeenCalledWith(
            file
        );
        expect(onUploaded).toHaveBeenCalledWith('/dA/new-id/asset/brand.svg');
        expect(picker.clear).toHaveBeenCalled();
    });

    it('is uploading only while the request runs', () => {
        const response = new Subject<string>();
        vi.mocked(spectator.inject(DotConfigurationAssetService).uploadImage).mockReturnValueOnce(
            response
        );
        const { imageUpload } = spectator.service;

        imageUpload.upload(selection(file), picker, onUploaded);
        expect(imageUpload.$uploading()).toBe(true);

        response.next('/dA/new-id/asset/brand.svg');
        response.complete();
        expect(imageUpload.$uploading()).toBe(false);
    });

    it('does nothing but clear the picker when no file was picked', () => {
        spectator.service.imageUpload.upload(selection(), picker, onUploaded);

        expect(spectator.inject(DotConfigurationAssetService).uploadImage).not.toHaveBeenCalled();
        expect(picker.clear).toHaveBeenCalled();
        expect(spectator.service.imageUpload.$uploading()).toBe(false);
    });

    it('reports a failed upload and does not hand over a path', () => {
        const error = new HttpErrorResponse({ status: 500 });
        vi.mocked(spectator.inject(DotConfigurationAssetService).uploadImage).mockReturnValueOnce(
            throwError(() => error)
        );

        spectator.service.imageUpload.upload(selection(file), picker, onUploaded);

        expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
        expect(onUploaded).not.toHaveBeenCalled();
        expect(spectator.service.imageUpload.$uploading()).toBe(false);
    });
});
