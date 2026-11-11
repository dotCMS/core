import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp
} from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { DotContentletService, DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotSourceEditorService, DotSourceEditorUploadError } from './dot-source-editor.service';

const FILE_VERSION_URL = '/dA/vtl-inode/fileAsset/header.vtl';

const fileContentlet = (overrides: Partial<DotCMSContentlet> = {}) =>
    ({
        identifier: 'vtl-id',
        inode: 'vtl-inode',
        fileName: 'header.vtl',
        fileAssetVersion: FILE_VERSION_URL,
        ...overrides
    }) as unknown as DotCMSContentlet;

describe('DotSourceEditorService', () => {
    let spectator: SpectatorHttp<DotSourceEditorService>;

    const createHttp = createHttpFactory({
        service: DotSourceEditorService,
        providers: [
            mockProvider(DotContentletService, {
                getContentletByInode: vi.fn(() => of(fileContentlet()))
            }),
            mockProvider(DotWorkflowActionsFireService, {
                saveContentlet: vi.fn(() => of(fileContentlet({ inode: 'new-inode' })))
            })
        ]
    });

    beforeEach(() => (spectator = createHttp()));

    // The factory's mocks are shared across tests: drop their calls.
    afterEach(() => vi.clearAllMocks());

    describe('load', () => {
        it("downloads the version's own file as text, with the name the save must keep", () => {
            const result = vi.fn();

            spectator.service.load('vtl-inode').subscribe(result);

            const req = spectator.expectOne(FILE_VERSION_URL, HttpMethod.GET);
            expect(req.request.responseType).toBe('text');
            req.flush('#set($x = 1)');

            expect(
                spectator.inject(DotContentletService).getContentletByInode
            ).toHaveBeenCalledWith('vtl-inode');
            expect(result).toHaveBeenCalledWith({ fileName: 'header.vtl', source: '#set($x = 1)' });
        });

        it('falls back to the file name the server adds to file rows', () => {
            spectator
                .inject(DotContentletService)
                .getContentletByInode.mockReturnValue(
                    of(fileContentlet({ fileName: undefined, name: 'footer.vtl' }))
                );
            const result = vi.fn();

            spectator.service.load('vtl-inode').subscribe(result);
            spectator.expectOne(FILE_VERSION_URL, HttpMethod.GET).flush('');

            expect(result).toHaveBeenCalledWith({ fileName: 'footer.vtl', source: '' });
        });

        it('fails when the version has no file to download', () => {
            spectator
                .inject(DotContentletService)
                .getContentletByInode.mockReturnValue(
                    of(fileContentlet({ fileAssetVersion: undefined }))
                );
            const error = vi.fn();

            spectator.service.load('vtl-inode').subscribe({ error });

            expect(error).toHaveBeenCalled();
        });
    });

    describe('save', () => {
        it('stages the source as a temp file under its own name, then saves it on the opened version', async () => {
            const result = vi.fn();

            spectator.service
                .save({
                    inode: 'vtl-inode',
                    fileName: 'header.vtl',
                    source: '#set($x = 2)'
                })
                .subscribe(result);

            const req = spectator.expectOne('/api/v1/temp', HttpMethod.POST);
            const file = (req.request.body as FormData).get('file') as File;

            expect(file.name).toBe('header.vtl');
            expect(await file.text()).toBe('#set($x = 2)');

            req.flush({ tempFiles: [{ id: 'temp_123' }] });

            expect(
                spectator.inject(DotWorkflowActionsFireService).saveContentlet
            ).toHaveBeenCalledWith({ inode: 'vtl-inode', fileAsset: 'temp_123' });
            expect(result).toHaveBeenCalledWith(expect.objectContaining({ inode: 'new-inode' }));
        });

        it('fails without saving when the server refuses the file, with its reason', () => {
            const error = vi.fn();

            spectator.service
                .save({ inode: 'vtl-inode', fileName: 'header.vtl', source: '#set($x = 2)' })
                .subscribe({ error });

            // A refused file still answers 200, with the reason where the staged file would be.
            spectator
                .expectOne('/api/v1/temp', HttpMethod.POST)
                .flush({ tempFiles: [{ errorCode: '400', message: 'File too large' }] });

            expect(
                spectator.inject(DotWorkflowActionsFireService).saveContentlet
            ).not.toHaveBeenCalled();
            expect(error).toHaveBeenCalledWith(new DotSourceEditorUploadError('File too large'));
        });
    });
});
