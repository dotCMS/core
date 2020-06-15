/* tslint:disable:no-unused-variable */

import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';

import {
    DotPushPublishFiltersService,
    DotPushPublishFilter
} from './dot-push-publish-filters.service';
import { DOTTestBed } from '@tests/dot-test-bed';

describe('DotPushPublishFiltersService', () => {
    let service: DotPushPublishFiltersService;
    let backend: MockBackend;
    let lastConnection;
    let injector;

    beforeEach(() => {
        lastConnection = [];

        injector = DOTTestBed.configureTestingModule({
            providers: [DotPushPublishFiltersService]
        });

        service = injector.get(DotPushPublishFiltersService);

        backend = injector.get(ConnectionBackend) as MockBackend;
        backend.connections.subscribe((connection: any) => {
            lastConnection.push(connection);
        });
    });

    it('should get hit pp filters url', () => {
        service.get().subscribe();
        expect(lastConnection[0].request.url).toBe('/api/v1/pushpublish/filters/');
    });

    it('should return entity', () => {
        let result: DotPushPublishFilter[];

        service.get().subscribe((res: DotPushPublishFilter[]) => {
            result = res;
        });
        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        entity: [
                            {
                                default: true,
                                key: 'some.yml',
                                title: 'Hello World'
                            }
                        ]
                    }
                })
            )
        );

        expect(result).toEqual([
            {
                default: true,
                key: 'some.yml',
                title: 'Hello World'
            }
        ]);
    });
});
