import { DotWorkflowActionsFireService } from './dot-workflow-actions-fire.service';
import {
    BaseRequestOptions,
    ConnectionBackend,
    Http,
    RequestMethod,
    RequestOptions,
    Response,
    ResponseOptions
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { DotActionBulkRequestOptions } from '@models/dot-action-bulk-request-options/dot-action-bulk-request-options.model';
import { DotActionBulkResult } from '@models/dot-action-bulk-result/dot-action-bulk-result.model';
import { TestBed } from '@angular/core/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

const mockBulkOptions: DotActionBulkRequestOptions = {
    workflowActionId: '1',
    contentletIds: ['1'],
    additionalParams: {
        assignComment: {
            comment: 'comment',
            assign: '12345'
        },
        pushPublish: {
            whereToSend: 'w',
            iWantTo: 'i',
            expireDate: 'e',
            expireTime: 'e',
            publishDate: 'p',
            publishTime: 'pp',
            filterKey: 'f'
        }
    }
};

describe('DotWorkflowActionsFireService', () => {
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let backend;
    let lastConnection;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotWorkflowActionsFireService,
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: RequestOptions, useClass: BaseRequestOptions },
                Http
            ]
        });

        dotWorkflowActionsFireService = TestBed.get(DotWorkflowActionsFireService);

        backend = TestBed.get(ConnectionBackend);
        backend.connections.subscribe((connection: any) => (lastConnection = connection));
    });

    it('should SAVE and return a new contentlet', () => {
        let result;
        dotWorkflowActionsFireService.newContentlet('persona', { name: 'Test' }).subscribe(res => {
            result = res;
        });

        lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [
                            {
                                name: 'test'
                            }
                        ]
                    }
                })
            )
        );

        expect(result).toEqual([
            {
                name: 'test'
            }
        ]);

        expect(lastConnection.request.url).toContain('v1/workflow/actions/default/fire/NEW');
        expect(lastConnection.request.method).toEqual(RequestMethod.Put);
        expect(JSON.parse(lastConnection.request.getBody())).toEqual({
            contentlet: {
                contentType: 'persona',
                name: 'Test'
            }
        });
    });

    it('should PUBLISH and return a new contentlet', () => {
        let result;
        dotWorkflowActionsFireService
            .publishContentlet('persona', { name: 'Test' })
            .subscribe(res => {
                result = res;
            });

        lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [
                            {
                                name: 'test'
                            }
                        ]
                    }
                })
            )
        );

        expect(result).toEqual([
            {
                name: 'test'
            }
        ]);

        expect(lastConnection.request.url).toContain('v1/workflow/actions/default/fire/PUBLISH');
        expect(lastConnection.request.method).toEqual(RequestMethod.Put);
        expect(JSON.parse(lastConnection.request.getBody())).toEqual({
            contentlet: {
                contentType: 'persona',
                name: 'Test'
            }
        });
    });

    it('should PUBLISH, wait for index and return a new contentlet', () => {
        let result;
        dotWorkflowActionsFireService
            .publishContentletAndWaitForIndex('persona', { name: 'Test' })
            .subscribe(res => {
                result = res;
            });

        lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [
                            {
                                name: 'test'
                            }
                        ]
                    }
                })
            )
        );

        expect(result).toEqual([
            {
                name: 'test'
            }
        ]);

        expect(lastConnection.request.url).toContain('v1/workflow/actions/default/fire/PUBLISH');
        expect(lastConnection.request.method).toEqual(RequestMethod.Put);
        expect(JSON.parse(lastConnection.request.getBody())).toEqual({
            contentlet: {
                contentType: 'persona',
                name: 'Test',
                indexPolicy: 'WAIT_FOR'
            }
        });
    });

    it('should create and return a new Content', () => {
        let result;
        const data = { id: '123' };
        dotWorkflowActionsFireService.fireTo('123', 'new', data).subscribe(res => {
            result = res;
        });

        lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [
                            {
                                name: 'test'
                            }
                        ]
                    }
                })
            )
        );

        expect(result).toEqual([
            {
                name: 'test'
            }
        ]);

        expect(lastConnection.request.url).toContain('v1/workflow/actions/new/fire?inode=123');
        expect(lastConnection.request.method).toEqual(RequestMethod.Put);
        expect(JSON.parse(lastConnection.request.getBody())).toEqual(data);
    });

    it('should fire bulk request', () => {
        const mockResult: DotActionBulkResult = {
            skippedCount: 1,
            successCount: 2,
            fails: []
        };
        let result;
        dotWorkflowActionsFireService.bulkFire(mockBulkOptions).subscribe(res => {
            result = res;
        });

        lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: mockResult
                    }
                })
            )
        );

        expect(result).toEqual(mockResult);

        expect(lastConnection.request.url).toContain(
            '/api/v1/workflow/contentlet/actions/bulk/fire'
        );
        expect(lastConnection.request.method).toEqual(RequestMethod.Put);
        expect(JSON.parse(lastConnection.request.getBody())).toEqual(mockBulkOptions);
    });
});
