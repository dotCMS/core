import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotContentTypeService, DotWorkflowActionsFireService } from '@dotcms/data-access';

import { DotEditContentService } from './dot-edit-content.service';

import { CONTENT_TYPE_MOCK } from '../utils/mocks';

const CONTENT_API_ENDPOINT = '/api/v1/content';
const TAGS_API_ENDPOINT = '/api/v2/tags';

describe('DotEditContentService', () => {
    let spectator: SpectatorHttp<DotEditContentService>;
    let dotContentTypeService: SpyObject<DotContentTypeService>;
    let dotWorkflowActionsFireService: SpyObject<DotWorkflowActionsFireService>;

    const createHttp = createHttpFactory({
        service: DotEditContentService,
        providers: [
            mockProvider(DotContentTypeService),
            mockProvider(DotWorkflowActionsFireService)
        ]
    });
    beforeEach(() => {
        spectator = createHttp();
        dotContentTypeService = spectator.inject(DotContentTypeService);
        dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
    });

    describe('Endpoints', () => {
        it('should get content by id', () => {
            const ID = '1';
            spectator.service.getContentById(ID).subscribe();
            spectator.expectOne(`${CONTENT_API_ENDPOINT}/${ID}`, HttpMethod.GET);
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
});
