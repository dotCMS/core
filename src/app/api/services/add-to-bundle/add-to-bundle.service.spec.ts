import { Observable } from 'rxjs';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { AddToBundleService } from './add-to-bundle.service';
import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';

describe('AddToBundleService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            AddToBundleService,
            DotCurrentUserService
        ]);

        this.addToBundleService =  this.injector.get(AddToBundleService);
        this.dotCurrentUserService =  this.injector.get(DotCurrentUserService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => this.lastConnection = connection);
    });

    it('should get bundle list', fakeAsync(() => {
        spyOn(this.dotCurrentUserService, 'getCurrentUser').and.returnValue(Observable.of({
            userId: '1234'
        }));

        const mockBundleItems = [
            {
                name: 'My bundle',
                id: '1234'
            },
            {
                name: 'My bundle 2',
                id: '1sdf5-23fs-dsf2-sf3oj23p4p42d'
            }
        ];

        const mockResponse = {
            idenfitier: 'id',
            items: mockBundleItems,
            label: 'name',
            numRows: 2
        };

        let result: any;
        this.addToBundleService.getBundles().subscribe(items => result = items);
        this.lastConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify(mockResponse)
        })));

        tick();
        expect(this.lastConnection.request.url).toContain('api/bundle/getunsendbundles/userid/1234');
        expect(result).toEqual(mockBundleItems);
    }));

    it('should do a post request and add to bundle', fakeAsync(() => {
        let result: any;
        const mockResponse = {
            'errorMessages': [],
            'total': 1,
            'bundleId': '1234-id-7890-entifier',
            'errors': 0
        };

        const mockBundleData = {
            id: '1234',
            name: 'my bundle'
        };

        this.addToBundleService.addToBundle('1234567890', mockBundleData).subscribe(res => {
            result = res._body;
        });
        this.lastConnection.mockRespond(new Response(new ResponseOptions({
            body: mockResponse
        })));

        tick();
        // tslint:disable-next-line:max-line-length
        expect(this.lastConnection.request.url).toContain('DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle');
        expect(result).toEqual(mockResponse);
    }));
});
