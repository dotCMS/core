import { DotWorkflowActionsFireService } from './dot-workflow-actions-fire.service';
import { DOTTestBed } from '@tests/dot-test-bed';

import { ConnectionBackend, RequestMethod, Response, ResponseOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';

describe('DotWorkflowActionsFireService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotWorkflowActionsFireService]);
        this.dotWorkflowActionsFireService = this.injector.get(DotWorkflowActionsFireService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should create and return a new Content', () => {
        let result;
        this.dotWorkflowActionsFireService
            .newContentlet('persona', { name: 'Test' })
            .subscribe(res => {
                result = res;
            });

        this.lastConnection.mockRespond(
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

        expect(this.lastConnection.request.url).toContain('v1/workflow/actions/default/fire/NEW');
        expect(this.lastConnection.request.method).toEqual(RequestMethod.Put);
    });

    it('should create and return a new Content', () => {
        let result;
        this.dotWorkflowActionsFireService.fireTo('123', 'new').subscribe(res => {
            result = res;
        });

        this.lastConnection.mockRespond(
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

        expect(this.lastConnection.request.url).toContain('v1/workflow/actions/new/fire?inode=123');
        expect(this.lastConnection.request.method).toEqual(RequestMethod.Put);
    });
});
