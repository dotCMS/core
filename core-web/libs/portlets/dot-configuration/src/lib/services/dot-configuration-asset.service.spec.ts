import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp
} from '@openng/spectator/vitest';
import { of } from 'rxjs';

import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotConfigurationAssetService } from './dot-configuration-asset.service';

describe('DotConfigurationAssetService', () => {
    let spectator: SpectatorHttp<DotConfigurationAssetService>;

    const createHttp = createHttpFactory({
        service: DotConfigurationAssetService,
        providers: [
            mockProvider(DotWorkflowActionsFireService, {
                publishContentletAndWaitForIndex: vi
                    .fn()
                    .mockReturnValue(
                        of(createFakeContentlet({ asset: '/dA/new-id/asset/logo.svg' }))
                    )
            })
        ]
    });

    beforeEach(() => {
        spectator = createHttp();
    });

    afterEach(() => vi.clearAllMocks());

    it('uploads the file, publishes it as a dotAsset and returns its path', () => {
        const file = new File(['<svg/>'], 'logo.svg', { type: 'image/svg+xml' });
        const workflow = spectator.inject(DotWorkflowActionsFireService);
        let path: string | undefined;

        spectator.service.uploadImage(file).subscribe((value) => (path = value));
        const req = spectator.expectOne('/api/v1/temp', HttpMethod.POST);
        req.flush({ tempFiles: [{ id: 'temp-123', fileName: 'logo.svg' }] });

        expect((req.request.body as FormData).get('file')).toBe(file);
        expect(workflow.publishContentletAndWaitForIndex).toHaveBeenCalledWith('dotAsset', {
            asset: 'temp-123',
            hostFolder: ''
        });
        expect(path).toBe('/dA/new-id/asset/logo.svg');
    });

    it('propagates an upload failure without publishing anything', () => {
        const workflow = spectator.inject(DotWorkflowActionsFireService);
        let error: unknown;

        spectator.service
            .uploadImage(new File(['x'], 'logo.png'))
            .subscribe({ error: (err) => (error = err) });
        spectator
            .expectOne('/api/v1/temp', HttpMethod.POST)
            .flush(null, { status: 413, statusText: 'Payload Too Large' });

        expect(error).toBeDefined();
        expect(workflow.publishContentletAndWaitForIndex).not.toHaveBeenCalled();
    });
});
