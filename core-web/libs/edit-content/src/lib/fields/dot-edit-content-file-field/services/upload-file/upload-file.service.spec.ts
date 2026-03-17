import { createHttpFactory, mockProvider, SpectatorHttp, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotContentletService, DotUploadFileService, DotUploadService } from '@dotcms/data-access';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotFileFieldUploadService, UploadFileProps } from './upload-file.service';

import { TEMP_FILE_MOCK } from '../../../../utils/mocks';

describe('DotFileFieldUploadService', () => {
    let spectator: SpectatorHttp<DotFileFieldUploadService>;
    let dotUploadFileService: SpyObject<DotUploadFileService>;
    let dotContentletService: SpyObject<DotContentletService>;
    let tempFileService: SpyObject<DotUploadService>;

    const createHttp = createHttpFactory({
        service: DotFileFieldUploadService,
        providers: [
            mockProvider(DotUploadFileService),
            mockProvider(DotContentletService),
            mockProvider(DotUploadService)
        ]
    });

    beforeEach(() => {
        spectator = createHttp();
        dotUploadFileService = spectator.inject(DotUploadFileService);
        dotContentletService = spectator.inject(DotContentletService);
        tempFileService = spectator.inject(DotUploadService);
    });

    it('should be created', () => {
        expect(spectator.service).toBeTruthy();
    });

    describe('uploadFile', () => {
        it('should upload a file with temp upload type', () => {
            tempFileService.uploadFile.mockResolvedValue(TEMP_FILE_MOCK);

            const file = new File([''], 'test.png', { type: 'image/png' });
            const uploadType = 'temp';
            const params: UploadFileProps = { file, uploadType, acceptedFiles: [], maxSize: null };

            spectator.service.uploadFile(params).subscribe((result) => {
                expect(result.source).toBe('temp');
                expect(result.file).toBe(TEMP_FILE_MOCK);
                expect(tempFileService.uploadFile).toHaveBeenCalledTimes(1);
                expect(tempFileService.uploadFile).toHaveBeenCalledWith({
                    file,
                    signal: undefined
                });
            });
        });

        it('should upload a file with temp upload type and abort signal', () => {
            const abortSignal = new AbortController().signal;
            tempFileService.uploadFile.mockResolvedValue(TEMP_FILE_MOCK);

            const file = new File([''], 'test.png', { type: 'image/png' });
            const uploadType = 'temp';
            const params: UploadFileProps = {
                file,
                uploadType,
                acceptedFiles: [],
                maxSize: null,
                abortSignal
            };

            spectator.service.uploadFile(params).subscribe((result) => {
                expect(result.source).toBe('temp');
                expect(result.file).toBe(TEMP_FILE_MOCK);
                expect(tempFileService.uploadFile).toHaveBeenCalledWith({
                    file,
                    signal: abortSignal
                });
            });
        });

        it('should upload a file with contentlet upload type when file is a File instance', () => {
            const mockContentlet = createFakeContentlet({
                mimeType: 'image/png',
                asset: '/dA/test-id/asset/test.png'
            });
            dotUploadFileService.uploadDotAssetWithContent.mockReturnValue(of(mockContentlet));

            const file = new File([''], 'test.png', { type: 'image/png' });
            const uploadType = 'dotasset';
            const params: UploadFileProps = { file, uploadType, acceptedFiles: [], maxSize: null };

            spectator.service.uploadFile(params).subscribe((result) => {
                expect(result.source).toBe('contentlet');
                expect(result.file).toBe(mockContentlet);
                expect(dotUploadFileService.uploadDotAssetWithContent).toHaveBeenCalledWith(file);
            });
        });

        it('should upload a file with contentlet upload type when file is a string', () => {
            const mockContentlet = createFakeContentlet({
                mimeType: 'image/png',
                asset: '/dA/test-id/asset/test.png'
            });
            dotUploadFileService.uploadDotAssetWithContent.mockReturnValue(of(mockContentlet));
            tempFileService.uploadFile.mockResolvedValue(TEMP_FILE_MOCK);

            const file = 'temp-file-id';
            const uploadType = 'dotasset';
            const params: UploadFileProps = { file, uploadType, acceptedFiles: [], maxSize: null };

            spectator.service.uploadFile(params).subscribe((result) => {
                expect(result.source).toBe('contentlet');
                expect(result.file).toBe(mockContentlet);
                expect(tempFileService.uploadFile).toHaveBeenCalledWith({
                    file,
                    signal: undefined
                });
                expect(dotUploadFileService.uploadDotAssetWithContent).toHaveBeenCalledWith(
                    TEMP_FILE_MOCK.id
                );
            });
        });

        it('should upload a file with contentlet upload type when file is a string and has abort signal', () => {
            const abortSignal = new AbortController().signal;
            const mockContentlet = createFakeContentlet({
                mimeType: 'image/png',
                asset: '/dA/test-id/asset/test.png'
            });
            dotUploadFileService.uploadDotAssetWithContent.mockReturnValue(of(mockContentlet));
            tempFileService.uploadFile.mockResolvedValue(TEMP_FILE_MOCK);

            const file = 'temp-file-id';
            const uploadType = 'dotasset';
            const params: UploadFileProps = {
                file,
                uploadType,
                acceptedFiles: [],
                maxSize: null,
                abortSignal
            };

            spectator.service.uploadFile(params).subscribe((result) => {
                expect(result.source).toBe('contentlet');
                expect(result.file).toBe(mockContentlet);
                expect(tempFileService.uploadFile).toHaveBeenCalledWith({
                    file,
                    signal: abortSignal
                });
                expect(dotUploadFileService.uploadDotAssetWithContent).toHaveBeenCalledWith(
                    TEMP_FILE_MOCK.id
                );
            });
        });
    });

    describe('uploadTempFile', () => {
        it('should upload a file to temp service', () => {
            tempFileService.uploadFile.mockResolvedValue(TEMP_FILE_MOCK);

            const file = new File([''], 'test.png', { type: 'image/png' });
            const acceptedFiles: string[] = [];

            spectator.service.uploadTempFile(file, acceptedFiles).subscribe((result) => {
                expect(result).toBe(TEMP_FILE_MOCK);
                expect(tempFileService.uploadFile).toHaveBeenCalledWith({
                    file,
                    signal: undefined
                });
            });
        });

        it('should upload a file to temp service with abort signal', () => {
            const abortSignal = new AbortController().signal;
            tempFileService.uploadFile.mockResolvedValue(TEMP_FILE_MOCK);

            const file = new File([''], 'test.png', { type: 'image/png' });
            const acceptedFiles: string[] = [];

            spectator.service
                .uploadTempFile(file, acceptedFiles, abortSignal)
                .subscribe((result) => {
                    expect(result).toBe(TEMP_FILE_MOCK);
                    expect(tempFileService.uploadFile).toHaveBeenCalledWith({
                        file,
                        signal: abortSignal
                    });
                });
        });

        it('should throw error when file type is not accepted', () => {
            const tempFile = { ...TEMP_FILE_MOCK, mimeType: 'application/pdf' };
            tempFileService.uploadFile.mockResolvedValue(tempFile);

            const file = new File([''], 'test.pdf', { type: 'application/pdf' });
            const acceptedFiles: string[] = ['image/png', 'image/jpeg'];

            spectator.service.uploadTempFile(file, acceptedFiles).subscribe({
                next: () => fail('should have thrown an error'),
                error: (error) => {
                    expect(error).toEqual(new Error('Invalid file type'));
                }
            });
        });

        it('should accept file when acceptedFiles is empty', () => {
            tempFileService.uploadFile.mockResolvedValue(TEMP_FILE_MOCK);

            const file = new File([''], 'test.png', { type: 'image/png' });
            const acceptedFiles: string[] = [];

            spectator.service.uploadTempFile(file, acceptedFiles).subscribe((result) => {
                expect(result).toBe(TEMP_FILE_MOCK);
            });
        });
    });

    describe('uploadDotAssetByFile', () => {
        it('should upload a file as dotAsset', () => {
            const mockContentlet = createFakeContentlet({
                mimeType: 'image/png',
                asset: '/dA/test-id/asset/test.png'
            });
            dotUploadFileService.uploadDotAssetWithContent.mockReturnValue(of(mockContentlet));

            const file = new File([''], 'test.png', { type: 'image/png' });
            const acceptedFiles: string[] = ['image/png'];

            spectator.service.uploadDotAssetByFile(file, acceptedFiles).subscribe((result) => {
                expect(result).toBe(mockContentlet);
                expect(dotUploadFileService.uploadDotAssetWithContent).toHaveBeenCalledWith(file);
            });
        });

        it('should throw error when file type is not accepted', () => {
            const mockContentlet = createFakeContentlet({
                mimeType: 'application/pdf',
                asset: '/dA/test-id/asset/test.pdf'
            });
            dotUploadFileService.uploadDotAssetWithContent.mockReturnValue(of(mockContentlet));

            const file = new File([''], 'test.pdf', { type: 'application/pdf' });
            const acceptedFiles: string[] = ['image/png', 'image/jpeg'];

            spectator.service.uploadDotAssetByFile(file, acceptedFiles).subscribe({
                next: () => fail('should have thrown an error'),
                error: (error) => {
                    expect(error).toEqual(new Error('Invalid file type'));
                }
            });
        });
    });

    describe('uploadDotAssetByUrl', () => {
        it('should upload a file by URL as dotAsset', () => {
            const mockContentlet = createFakeContentlet({
                mimeType: 'image/png',
                asset: '/dA/test-id/asset/test.png'
            });
            dotUploadFileService.uploadDotAssetWithContent.mockReturnValue(of(mockContentlet));
            tempFileService.uploadFile.mockResolvedValue(TEMP_FILE_MOCK);

            const file = 'temp-file-id';
            const acceptedFiles: string[] = ['image/png'];

            spectator.service.uploadDotAssetByUrl(file, acceptedFiles).subscribe((result) => {
                expect(result).toBe(mockContentlet);
                expect(tempFileService.uploadFile).toHaveBeenCalledWith({
                    file,
                    signal: undefined
                });
                expect(dotUploadFileService.uploadDotAssetWithContent).toHaveBeenCalledWith(
                    TEMP_FILE_MOCK.id
                );
            });
        });

        it('should upload a file by URL with abort signal', () => {
            const abortSignal = new AbortController().signal;
            const mockContentlet = createFakeContentlet({
                mimeType: 'image/png',
                asset: '/dA/test-id/asset/test.png'
            });
            dotUploadFileService.uploadDotAssetWithContent.mockReturnValue(of(mockContentlet));
            tempFileService.uploadFile.mockResolvedValue(TEMP_FILE_MOCK);

            const file = 'temp-file-id';
            const acceptedFiles: string[] = ['image/png'];

            spectator.service
                .uploadDotAssetByUrl(file, acceptedFiles, abortSignal)
                .subscribe((result) => {
                    expect(result).toBe(mockContentlet);
                    expect(tempFileService.uploadFile).toHaveBeenCalledWith({
                        file,
                        signal: abortSignal
                    });
                    expect(dotUploadFileService.uploadDotAssetWithContent).toHaveBeenCalledWith(
                        TEMP_FILE_MOCK.id
                    );
                });
        });

        it('should throw error when file type is not accepted', () => {
            const tempFile = { ...TEMP_FILE_MOCK, mimeType: 'application/pdf' };
            tempFileService.uploadFile.mockResolvedValue(tempFile);

            const file = 'temp-file-id';
            const acceptedFiles: string[] = ['image/png', 'image/jpeg'];

            spectator.service.uploadDotAssetByUrl(file, acceptedFiles).subscribe({
                next: () => fail('should have thrown an error'),
                error: (error) => {
                    expect(error).toEqual(new Error('Invalid file type'));
                }
            });
        });
    });

    describe('uploadDotAsset', () => {
        it('should upload a file and return contentlet', () => {
            const mockContentlet = createFakeContentlet({
                mimeType: 'image/png',
                asset: '/dA/test-id/asset/test.png'
            });
            dotUploadFileService.uploadDotAssetWithContent.mockReturnValue(of(mockContentlet));

            const file = new File([''], 'test.png', { type: 'image/png' });

            spectator.service.uploadDotAsset(file).subscribe((result) => {
                expect(result).toBe(mockContentlet);
                expect(dotUploadFileService.uploadDotAssetWithContent).toHaveBeenCalledWith(file);
            });
        });

        it('should upload a file by string id and return contentlet', () => {
            const mockContentlet = createFakeContentlet({
                mimeType: 'image/png',
                asset: '/dA/test-id/asset/test.png'
            });
            dotUploadFileService.uploadDotAssetWithContent.mockReturnValue(of(mockContentlet));

            const fileId = 'temp-file-id';

            spectator.service.uploadDotAsset(fileId).subscribe((result) => {
                expect(result).toBe(mockContentlet);
                expect(dotUploadFileService.uploadDotAssetWithContent).toHaveBeenCalledWith(fileId);
            });
        });
    });

    describe('getContentById', () => {
        it('should get a contentlet by identifier', () => {
            const mockContentlet = createFakeContentlet({
                identifier: 'test-identifier',
                mimeType: 'image/png'
            });
            dotContentletService.getContentletByInodeWithContent.mockReturnValue(
                of(mockContentlet)
            );

            spectator.service.getContentById('test-identifier').subscribe((result) => {
                expect(result).toBe(mockContentlet);
                expect(dotContentletService.getContentletByInodeWithContent).toHaveBeenCalledWith(
                    'test-identifier'
                );
            });
        });

        it('should get a contentlet with content when editable as text', () => {
            const mockContentlet = createFakeContentlet({
                identifier: 'test-identifier',
                mimeType: 'text/plain',
                asset: '/dA/test-id/asset/test.txt',
                assetMetaData: {
                    editableAsText: true
                }
            });
            dotContentletService.getContentletByInodeWithContent.mockReturnValue(
                of(mockContentlet)
            );

            spectator.service.getContentById('test-identifier').subscribe((result) => {
                expect(result).toBe(mockContentlet);
                expect(dotContentletService.getContentletByInodeWithContent).toHaveBeenCalledWith(
                    'test-identifier'
                );
            });
        });
    });
});
