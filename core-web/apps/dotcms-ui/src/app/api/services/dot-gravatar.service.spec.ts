import md5 from 'md5';

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, waitForAsync } from '@angular/core/testing';

import { DotGravatarService } from './dot-gravatar-service';

const mockProfile = {
    displayName: 'admindotcms',
    hash: '314d3bbf9bf6e65ff8095fe7f928fe85',
    id: '138297517',
    name: [],
    photos: [
        {
            type: 'thumbnail',
            value: 'https://secure.gravatar.com/avatar/314d3bbf9bf6e65ff8095fe7f928fe85'
        }
    ],
    preferredUsername: 'admindotcms',
    profileUrl: 'http://gravatar.com/admindotcms',
    requestHash: '314d3bbf9bf6e65ff8095fe7f928fe85',
    thumbnailUrl: 'https://secure.gravatar.com/avatar/314d3bbf9bf6e65ff8095fe7f928fe85',
    urls: []
};

describe('DotGravatarService', () => {
    let service: DotGravatarService;
    let httpTestingController: HttpTestingController;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [],
            providers: [
                DotGravatarService,
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting()
            ]
        });

        service = TestBed.inject(DotGravatarService);
        httpTestingController = TestBed.inject(HttpTestingController);
    }));

    it('Should return the photos url', (done) => {
        service.getPhoto('1').subscribe((avatarUrl: string) => {
            expect(avatarUrl).toEqual(
                'https://secure.gravatar.com/avatar/314d3bbf9bf6e65ff8095fe7f928fe85'
            );
            done();
        });

        const reqMock = httpTestingController.expectOne((req) => {
            return req.url === `//www.gravatar.com/${md5('1')}.json?`;
        });
        expect(reqMock.request.method).toBe('JSONP');
        reqMock.flush({
            entry: [mockProfile]
        });
    });

    it('Should return trigger an error', (done) => {
        service.getPhoto('1').subscribe(
            () => null,
            (e) => {
                expect(e).toBeTruthy();
                done();
            }
        );

        const reqMock = httpTestingController.expectOne((req) => {
            return req.url === `//www.gravatar.com/${md5('1')}.json?`;
        });

        reqMock.flush({ status: 404, statusText: 'Not Found' });
    });
});
