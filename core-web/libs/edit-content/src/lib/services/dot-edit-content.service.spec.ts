import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import {
    DotContentTypeService,
    DotBrowsingService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotContentletDepths } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentService } from './dot-edit-content.service';

import { CONTENT_TYPE_MOCK } from '../utils/mocks';

const CONTENT_API_ENDPOINT = '/api/v1/content';
const TAGS_API_ENDPOINT = '/api/v2/tags';

describe('DotEditContentService', () => {
    let spectator: SpectatorHttp<DotEditContentService>;
    let dotContentTypeService: SpyObject<DotContentTypeService>;
    let dotWorkflowActionsFireService: SpyObject<DotWorkflowActionsFireService>;
    let dotBrowsingService: SpyObject<DotBrowsingService>;

    const createHttp = createHttpFactory({
        service: DotEditContentService,
        providers: [
            mockProvider(DotBrowsingService),
            mockProvider(DotContentTypeService),
            mockProvider(DotWorkflowActionsFireService)
        ]
    });
    beforeEach(() => {
        spectator = createHttp();
        dotContentTypeService = spectator.inject(DotContentTypeService);
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        dotBrowsingService = spectator.inject(DotBrowsingService);
    });

    describe('Endpoints', () => {
        it('should get content by id', () => {
            const ID = '1';
            spectator.service.getContentById({ id: ID }).subscribe();
            spectator.expectOne(`${CONTENT_API_ENDPOINT}/${ID}`, HttpMethod.GET);
        });

        it('should get content by id and language', () => {
            const ID = '1';
            spectator.service.getContentById({ id: ID, languageId: 1 }).subscribe();
            spectator.expectOne(`${CONTENT_API_ENDPOINT}/${ID}?language=1`, HttpMethod.GET);
        });

        it('should get content by id and depth', () => {
            const ID = '1';
            const DEPTH = DotContentletDepths.TWO;
            spectator.service.getContentById({ id: ID, depth: DEPTH }).subscribe();

            spectator.expectOne(`${CONTENT_API_ENDPOINT}/${ID}?depth=${DEPTH}`, HttpMethod.GET);
        });

        it('should get tags', () => {
            const NAME = 'test';
            spectator.service.getTags(NAME).subscribe();
            spectator.expectOne(`${TAGS_API_ENDPOINT}?name=${NAME}`, HttpMethod.GET);
        });

        it('should get activities for a contentlet', () => {
            const identifier = '123-456-789';
            spectator.service.getActivities(identifier).subscribe();
            spectator.expectOne(
                `/api/v1/workflow/tasks/history/comments/${identifier}`,
                HttpMethod.GET
            );
        });

        it('should create an activity for a contentlet', () => {
            const identifier = '123-456-789';
            const comment = 'Test comment';
            spectator.service.createActivity(identifier, comment).subscribe();

            const req = spectator.expectOne(
                `/api/v1/workflow/${identifier}/comments`,
                HttpMethod.POST
            );
            expect(req.request.body).toEqual({ comment });
        });

        it('should return activities from response entity', (done) => {
            const identifier = '123-456-789';
            const mockActivities = [
                {
                    commentDescription: 'Test comment',
                    createdDate: 1234567890,
                    email: 'test@test.com',
                    postedBy: 'Test User',
                    roleId: '1',
                    taskId: '1',
                    type: 'comment'
                }
            ];

            spectator.service.getActivities(identifier).subscribe((activities) => {
                expect(activities).toEqual(mockActivities);
                done();
            });

            const req = spectator.expectOne(
                `/api/v1/workflow/tasks/history/comments/${identifier}`,
                HttpMethod.GET
            );
            req.flush({ entity: mockActivities });
        });

        it('should return created activity from response entity', (done) => {
            const identifier = '123-456-789';
            const comment = 'Test comment';
            const mockActivity = {
                commentDescription: comment,
                createdDate: 1234567890,
                email: 'test@test.com',
                postedBy: 'Test User',
                roleId: '1',
                taskId: '1',
                type: 'comment'
            };

            spectator.service.createActivity(identifier, comment).subscribe((activity) => {
                expect(activity).toEqual(mockActivity);
                done();
            });

            const req = spectator.expectOne(
                `/api/v1/workflow/${identifier}/comments`,
                HttpMethod.POST
            );
            expect(req.request.body).toEqual({ comment });
            req.flush({ entity: mockActivity });
        });

        it('should get versions with offset and limit parameters', (done) => {
            const identifier = '123-456-789';
            const paginationParams = { offset: 2, limit: 10 };
            const mockVersions = [
                {
                    archived: false,
                    country: 'United States',
                    countryCode: 'US',
                    experimentVariant: false,
                    inode: 'test-inode-123',
                    isoCode: 'en-us',
                    language: 'English',
                    languageCode: 'en',
                    languageFlag: 'en_US',
                    languageId: 1,
                    live: true,
                    modDate: 1756414525995,
                    modUserName: 'dotcms.org.1',
                    title: 'Test Version',
                    working: true
                }
            ];
            const mockPagination = {
                currentPage: 2,
                perPage: 10,
                totalEntries: 50,
                totalPages: 5
            };
            const mockResponse = {
                entity: mockVersions,
                pagination: mockPagination
            };

            spectator.service.getVersions(identifier, paginationParams).subscribe((response) => {
                expect(response.entity).toEqual(mockVersions);
                expect(response.pagination).toEqual(mockPagination);
                done();
            });

            const req = spectator.expectOne(
                `/api/v1/content/versions/id/${identifier}/history?offset=2&limit=10`,
                HttpMethod.GET
            );
            req.flush(mockResponse);
        });

        it('should get versions with languageId parameter', (done) => {
            const identifier = '123-456-789';
            const languageId = 1;
            const mockVersions = [
                {
                    archived: false,
                    country: 'United States',
                    countryCode: 'US',
                    experimentVariant: false,
                    inode: 'test-inode-123',
                    isoCode: 'en-us',
                    language: 'English',
                    languageCode: 'en',
                    languageFlag: 'en_US',
                    languageId: 1,
                    live: true,
                    modDate: 1756414525995,
                    modUserName: 'dotcms.org.1',
                    title: 'Test Version',
                    working: true
                }
            ];
            const mockResponse = {
                entity: mockVersions
            };

            spectator.service
                .getVersions(identifier, undefined, languageId)
                .subscribe((response) => {
                    expect(response.entity).toEqual(mockVersions);
                    done();
                });

            const req = spectator.expectOne(
                `/api/v1/content/versions/id/${identifier}/history?languageId=1`,
                HttpMethod.GET
            );
            req.flush(mockResponse);
        });

        it('should get versions with pagination and languageId parameters', (done) => {
            const identifier = '123-456-789';
            const paginationParams = { offset: 2, limit: 10 };
            const languageId = 1;
            const mockVersions = [
                {
                    archived: false,
                    country: 'United States',
                    countryCode: 'US',
                    experimentVariant: false,
                    inode: 'test-inode-123',
                    isoCode: 'en-us',
                    language: 'English',
                    languageCode: 'en',
                    languageFlag: 'en_US',
                    languageId: 1,
                    live: true,
                    modDate: 1756414525995,
                    modUserName: 'dotcms.org.1',
                    title: 'Test Version',
                    working: true
                }
            ];
            const mockPagination = {
                currentPage: 2,
                perPage: 10,
                totalEntries: 50,
                totalPages: 5
            };
            const mockResponse = {
                entity: mockVersions,
                pagination: mockPagination
            };

            spectator.service
                .getVersions(identifier, paginationParams, languageId)
                .subscribe((response) => {
                    expect(response.entity).toEqual(mockVersions);
                    expect(response.pagination).toEqual(mockPagination);
                    done();
                });

            const req = spectator.expectOne(
                `/api/v1/content/versions/id/${identifier}/history?offset=2&limit=10&languageId=1`,
                HttpMethod.GET
            );
            req.flush(mockResponse);
        });

        it('should delete push publish history for a contentlet', () => {
            const identifier = '123-456-789';
            spectator.service.deletePushPublishHistory(identifier).subscribe();
            spectator.expectOne(
                `/api/bundle/deletepushhistory/assetid/${identifier}`,
                HttpMethod.GET
            );
        });

        it('should get push publish history for a contentlet', (done) => {
            const identifier = '123-456-789';
            const mockPushPublishHistory = [
                {
                    bundleId: 'bundle-123',
                    environment: 'production',
                    pushDate: 1756414525995,
                    pushedBy: 'admin@dotcms.com'
                },
                {
                    bundleId: 'bundle-456',
                    environment: 'staging',
                    pushDate: 1756414425995,
                    pushedBy: 'editor@dotcms.com'
                }
            ];
            const mockResponse = {
                entity: mockPushPublishHistory
            };

            spectator.service.getPushPublishHistory(identifier).subscribe((response) => {
                expect(response.entity).toEqual(mockPushPublishHistory);
                done();
            });

            const req = spectator.expectOne(
                `/api/v1/content/${identifier}/push/history`,
                HttpMethod.GET
            );
            req.flush(mockResponse);
        });

        it('should get push publish history with pagination parameters', (done) => {
            const identifier = '123-456-789';
            const paginationParams = { offset: 2, limit: 10 };
            const mockPushPublishHistory = [
                {
                    bundleId: 'bundle-789',
                    environment: 'development',
                    pushDate: 1756414325995,
                    pushedBy: 'dev@dotcms.com'
                }
            ];
            const mockPagination = {
                currentPage: 2,
                perPage: 10,
                totalEntries: 25,
                totalPages: 3
            };
            const mockResponse = {
                entity: mockPushPublishHistory,
                pagination: mockPagination
            };

            spectator.service
                .getPushPublishHistory(identifier, paginationParams)
                .subscribe((response) => {
                    expect(response.entity).toEqual(mockPushPublishHistory);
                    expect(response.pagination).toEqual(mockPagination);
                    done();
                });

            const req = spectator.expectOne(
                `/api/v1/content/${identifier}/push/history?offset=2&limit=10`,
                HttpMethod.GET
            );
            req.flush(mockResponse);
        });
    });

    describe('Facades', () => {
        it('should get content type form data', (done) => {
            const CONTENTID_OR_VAR = '456';
            dotContentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));

            spectator.service.getContentType(CONTENTID_OR_VAR).subscribe(() => {
                expect(dotContentTypeService.getContentType).toHaveBeenCalledWith(CONTENTID_OR_VAR);
                done();
            });
        });

        it('should call dotWorkflowActionsFireService.saveContentlet with the provided data', (done) => {
            const DATA = { title: 'Test Contentlet', body: 'This is a test' };
            dotWorkflowActionsFireService.saveContentlet.mockReturnValue(of({}));

            spectator.service.saveContentlet(DATA).subscribe(() => {
                expect(dotWorkflowActionsFireService.saveContentlet).toHaveBeenCalledWith(DATA);
                done();
            });
        });
    });

    describe('getContentByFolder', () => {
        it('should call dotBrowsingService with correct params when only hostFolderId is provided', () => {
            const mockContentlets = [];
            dotBrowsingService.getContentByFolder.mockReturnValue(of(mockContentlets));

            const params = { hostFolderId: '123' };
            spectator.service.getContentByFolder(params);

            expect(dotBrowsingService.getContentByFolder).toHaveBeenCalledWith(params);
        });

        it('should call dotBrowsingService with all provided params', () => {
            const mockContentlets = [];
            const params = {
                hostFolderId: '123',
                mimeTypes: ['image/jpeg', 'image/png'],
                showLinks: true,
                showDotAssets: false,
                showPages: true,
                showFiles: false,
                showFolders: true,
                showWorking: false,
                sortByDesc: false,
                showArchived: true
            };
            dotBrowsingService.getContentByFolder.mockReturnValue(of(mockContentlets));

            spectator.service.getContentByFolder(params);

            expect(dotBrowsingService.getContentByFolder).toHaveBeenCalledWith(params);
        });

        it('should return content from dotBrowsingService', (done) => {
            const mockContentlets = [
                createFakeContentlet({
                    inode: 'content-1',
                    title: 'Test Content',
                    identifier: 'content-1'
                })
            ];
            const params = { hostFolderId: '123' };
            dotBrowsingService.getContentByFolder.mockReturnValue(of(mockContentlets));

            spectator.service.getContentByFolder(params).subscribe((result) => {
                expect(result).toEqual(mockContentlets);
                done();
            });
        });
    });
});
