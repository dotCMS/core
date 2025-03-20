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
    DotSiteService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotContentletDepths } from '@dotcms/dotcms-models';

import { DotEditContentService } from './dot-edit-content.service';

import { CONTENT_TYPE_MOCK } from '../utils/mocks';

const CONTENT_API_ENDPOINT = '/api/v1/content';
const TAGS_API_ENDPOINT = '/api/v2/tags';

describe('DotEditContentService', () => {
    let spectator: SpectatorHttp<DotEditContentService>;
    let dotContentTypeService: SpyObject<DotContentTypeService>;
    let dotWorkflowActionsFireService: SpyObject<DotWorkflowActionsFireService>;
    let dotSiteService: SpyObject<DotSiteService>;

    const createHttp = createHttpFactory({
        service: DotEditContentService,
        providers: [
            mockProvider(DotSiteService),
            mockProvider(DotContentTypeService),
            mockProvider(DotWorkflowActionsFireService)
        ]
    });
    beforeEach(() => {
        spectator = createHttp();
        dotContentTypeService = spectator.inject(DotContentTypeService);
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        dotSiteService = spectator.inject(DotSiteService);
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
        it('should call siteService with correct params when only folderId is provided', () => {
            dotSiteService.getContentByFolder.mockReturnValue(of([]));
            spectator.service.getContentByFolder({ folderId: '123' });

            expect(dotSiteService.getContentByFolder).toHaveBeenCalledWith({
                mimeTypes: [],
                hostFolderId: '123',
                showLinks: false,
                showDotAssets: true,
                showPages: false,
                showFiles: true,
                showFolders: false,
                showWorking: true,
                sortByDesc: true,
                showArchived: false
            });
        });
    });
});
