import { SpectatorService, SpyObject, createServiceFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';

import { DotContentTypeService, DotWorkflowActionsFireService } from '@dotcms/data-access';

import { DotEditContentService } from './dot-edit-content.service';

import { CONTENT_TYPE_MOCK } from '../feature/edit-content/edit-content.layout.component.spec';

describe('DotEditContentService', () => {
    let spectator: SpectatorService<DotEditContentService>;
    let dotEditContentService: DotEditContentService;
    let contentTypeService: SpyObject<DotContentTypeService>;
    const createService = createServiceFactory({
        service: DotEditContentService,
        mocks: [DotContentTypeService, DotWorkflowActionsFireService, HttpClient]
    });

    beforeEach(() => {
        spectator = createService();
        dotEditContentService = spectator.inject(DotEditContentService);
        contentTypeService = spectator.inject(DotContentTypeService);
    });

    it('should get content by id', (done) => {
        const httpService = spectator.inject(HttpClient);
        httpService.get.mockReturnValue(of({ entity: CONTENT_TYPE_MOCK }));
        const id = '1';
        dotEditContentService.getContentById(id).subscribe(() => {
            expect(httpService.get).toHaveBeenCalledWith(`/api/v1/content/${id}`);
            done();
        });
    });

    it('should get content type form data', (done) => {
        const contentIdOrVar = '456';

        contentTypeService.getContentType.mockReturnValue(of(CONTENT_TYPE_MOCK));

        dotEditContentService.getContentTypeFormData(contentIdOrVar).subscribe((response) => {
            expect(response).toEqual(CONTENT_TYPE_MOCK.layout);
            expect(contentTypeService.getContentType).toHaveBeenCalledWith(contentIdOrVar);
            done();
        });
    });

    it('should call dotWorkflowActionsFireService.saveContentlet with the provided data', () => {
        const data = { title: 'Test Contentlet', body: 'This is a test' };

        const dotWorkflowActionsFireService = spectator.inject(DotWorkflowActionsFireService);
        dotWorkflowActionsFireService.saveContentlet.mockReturnValue(of({}));

        dotEditContentService.saveContentlet(data).subscribe(() => {
            expect(dotWorkflowActionsFireService.saveContentlet).toHaveBeenCalledWith(data);
        });
    });
});
