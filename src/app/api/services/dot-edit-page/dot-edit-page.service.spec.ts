import { DotEditPageService } from './dot-edit-page.service';
import { MockBackend } from '@angular/http/testing';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { ConnectionBackend } from '@angular/http';
import { DotPageContainer } from '../../../portlets/dot-edit-page/shared/models/dot-page-container.model';

describe('DotEditPageService', () => {
    let service: DotEditPageService;
    let backend: MockBackend;
    let lastConnection;
    let injector;

    beforeEach(() => {
        lastConnection = [];

        injector = DOTTestBed.configureTestingModule({
            providers: [DotEditPageService]
        });

        service = injector.get(DotEditPageService);
        backend = injector.get(ConnectionBackend) as MockBackend;
        backend.connections.subscribe((connection: any) => {
            lastConnection.push(connection);
        });
    });

    it('should do a request for save content', () => {
        const pageId = '1';
        const model: DotPageContainer[] = [
            {
                identifier: '1',
                uuid: '2',
                contentletsId: ['3', '4']
            },
            {
                identifier: '5',
                uuid: '6',
                contentletsId: ['7', '8']
            }
        ];

        service.save(pageId, model).subscribe(() => {
            expect(lastConnection[0].request.url).toContain(`v1/page/${pageId}/content`);
            expect(lastConnection[0].request.method).toEqual(1);
            expect(lastConnection[0].request._body).toEqual(JSON.stringify(model));
        });
    });
});
