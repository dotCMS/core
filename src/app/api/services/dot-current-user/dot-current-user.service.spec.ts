import { DotCurrentUserService } from './dot-current-user.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';

describe('DotCurrentUserService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotCurrentUserService]);

        this.dotCurrentUserService = this.injector.get(DotCurrentUserService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it(
        'should get logged user',
        fakeAsync(() => {
            const mockCurrentUserResponse = {
                email: 'admin@dotcms.com',
                givenName: 'TEST',
                roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
                surnaname: 'User',
                userId: 'testId'
            };
            let currentUser: any;
            this.dotCurrentUserService.getCurrentUser().subscribe((user) => {
                currentUser = user._body;
            });

            this.lastConnection.mockRespond(
                new Response(
                    new ResponseOptions({
                        body: mockCurrentUserResponse
                    })
                )
            );

            tick();
            expect(this.lastConnection.request.url).toContain('v1/users/current');
            expect(currentUser).toEqual(mockCurrentUserResponse);
        })
    );
});
