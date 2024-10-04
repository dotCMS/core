import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { HttpHeaders } from '@angular/common/http';

import { DotActionBulkRequestOptions, DotActionBulkResult } from '@dotcms/dotcms-models';
import { dotcmsContentletMock } from '@dotcms/utils-testing';

import { DotWorkflowActionsFireService } from './dot-workflow-actions-fire.service';

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
            filterKey: 'f',
            timezoneId: 'America/Costa_Rica'
        },
        additionalParamsMap: { _path_to_move: '' }
    }
};

describe('DotWorkflowActionsFireService', () => {
    let spectator: SpectatorHttp<DotWorkflowActionsFireService>;
    const createHttp = createHttpFactory(DotWorkflowActionsFireService);
    const defaultHeaders = new HttpHeaders()
        .set('Accept', '*/*')
        .set('Content-Type', 'application/json');

    beforeEach(() => (spectator = createHttp()));

    it('should SAVE and return a new contentlet', (done) => {
        const mockResult = {
            name: 'test'
        };

        const requestBody = {
            contentlet: {
                contentType: 'persona',
                name: 'Test'
            }
        };

        spectator.service.newContentlet('persona', { name: 'Test' }).subscribe((res) => {
            expect(res).toEqual([mockResult]);
            done();
        });

        const req = spectator.expectOne(
            '/api/v1/workflow/actions/default/fire/NEW',
            HttpMethod.PUT
        );

        expect(req.request.body).toEqual(requestBody);
        expect(req.request.headers).toEqual(defaultHeaders);

        req.flush({
            entity: [mockResult]
        });
    });

    it('should SAVE and return a new contentlet with FormData', (done) => {
        const mockResult = {
            name: 'test'
        };

        const file = new File(['hello'], 'hello.txt', { type: 'text/plain' });

        const requestBody = {
            contentlet: {
                contentType: 'dotAsset',
                file: file.name
            }
        };

        const formData = new FormData();
        formData.append('file', file);

        spectator.service
            .newContentlet('dotAsset', { file: file.name }, formData)
            .subscribe((res) => {
                expect(res).toEqual([mockResult]);
                done();
            });

        const req = spectator.expectOne(
            '/api/v1/workflow/actions/default/fire/NEW',
            HttpMethod.PUT
        );

        expect(req.request.body.get('json')).toEqual(JSON.stringify(requestBody));

        req.flush({
            entity: [mockResult]
        });
    });

    it('should EDIT and return the updated contentlet', (done) => {
        const mockResult = {
            inode: '123'
        };

        spectator.service
            .saveContentlet({ inode: '123', title: 'hello world' })
            .subscribe((res) => {
                expect(res).toEqual([mockResult]);
                done();
            });

        const req = spectator.expectOne(
            '/api/v1/workflow/actions/default/fire/EDIT?inode=123',
            HttpMethod.PUT
        );
        req.flush({
            entity: [mockResult]
        });
    });

    it('should DESTROY and return the deleted contentlet', (done) => {
        const mockResult = {
            inode: '123'
        };

        spectator.service.deleteContentlet({ inode: '123' }).subscribe((res) => {
            expect(res).toEqual([mockResult]);
            done();
        });

        const req = spectator.expectOne(
            '/api/v1/workflow/actions/default/fire/DESTROY?inode=123',
            HttpMethod.PUT
        );

        req.flush({
            entity: [mockResult]
        });
    });

    it('should PUBLISH and return a new contentlet', (done) => {
        const mockResult = {
            name: 'test'
        };

        const requestBody = {
            contentlet: {
                contentType: 'persona',
                name: 'test'
            }
        };

        spectator.service.publishContentlet('persona', { name: 'test' }).subscribe((res) => {
            expect(res).toEqual([mockResult]);
            done();
        });

        const req = spectator.expectOne(
            '/api/v1/workflow/actions/default/fire/PUBLISH',
            HttpMethod.PUT
        );

        expect(req.request.body).toEqual(requestBody);
        expect(req.request.headers).toEqual(defaultHeaders);

        req.flush({
            entity: [mockResult]
        });
    });

    it('should PUBLISH, wait for index and return a new contentlet', (done) => {
        const mockResult = {
            name: 'test'
        };
        const requestBody = {
            contentlet: {
                contentType: 'persona',
                name: 'test',
                indexPolicy: 'WAIT_FOR'
            }
        };

        spectator.service
            .publishContentletAndWaitForIndex('persona', { name: 'test' })
            .subscribe((res) => {
                expect(res).toEqual([mockResult]);
                done();
            });

        const req = spectator.expectOne(
            '/api/v1/workflow/actions/default/fire/PUBLISH',
            HttpMethod.PUT
        );

        expect(req.request.body).toEqual(requestBody);
        expect(req.request.headers).toEqual(defaultHeaders);

        req.flush({
            entity: [mockResult]
        });
    });

    it('should PUBLISH and Wait For Index with Individual Permissions', (done) => {
        const mockResult = {
            name: 'test'
        };

        const requestBody = {
            contentlet: {
                contentType: 'persona',
                name: 'test',
                indexPolicy: 'WAIT_FOR'
            },
            individualPermissions: { READ: ['123'], WRITE: ['456'] }
        };

        spectator.service
            .publishContentletAndWaitForIndex(
                'persona',
                { name: 'test' },
                { READ: ['123'], WRITE: ['456'] }
            )
            .subscribe((res) => {
                expect(res).toEqual([mockResult]);
                done();
            });

        const req = spectator.expectOne(
            '/api/v1/workflow/actions/default/fire/PUBLISH',
            HttpMethod.PUT
        );

        expect(req.request.body).toEqual(requestBody);
        expect(req.request.headers).toEqual(defaultHeaders);

        req.flush({
            entity: [mockResult]
        });
    });

    it('should create and return a new Content', (done) => {
        spectator.service
            .fireTo({
                inode: '123',
                actionId: 'new',
                data: { id: '123' }
            })
            .subscribe((res) => {
                expect(res).toEqual(dotcmsContentletMock);
                done();
            });

        const req = spectator.expectOne(
            '/api/v1/workflow/actions/new/fire?inode=123&indexPolicy=WAIT_FOR',
            HttpMethod.PUT
        );

        req.flush({
            entity: dotcmsContentletMock
        });
    });

    it('should fire bulk request', (done) => {
        const mockResult: DotActionBulkResult = {
            skippedCount: 1,
            successCount: 2,
            fails: []
        };

        spectator.service.bulkFire(mockBulkOptions).subscribe((res) => {
            expect(res).toEqual(mockResult);
            done();
        });

        const req = spectator.expectOne(
            '/api/v1/workflow/contentlet/actions/bulk/fire',
            HttpMethod.PUT
        );
        req.flush({
            entity: mockResult
        });
    });

    afterEach(() => {
        spectator.controller.verify();
    });
});
