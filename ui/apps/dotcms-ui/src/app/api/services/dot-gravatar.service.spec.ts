import { TestBed, waitForAsync } from '@angular/core/testing';
import { DotGravatarService } from './dot-gravatar-service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

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
    // let httpClient: HttpClient;
    let httpTestingController: HttpTestingController;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                providers: [DotGravatarService],
                imports: [HttpClientTestingModule]
            });

            service = TestBed.inject(DotGravatarService);
            // httpClient = TestBed.inject(HttpClient);
            httpTestingController = TestBed.inject(HttpTestingController);
        })
    );

    it('Should return the photos url', (done) => {
        service.getPhoto('1').subscribe((avatarUrl: string) => {
            expect(avatarUrl).toEqual(
                'https://secure.gravatar.com/avatar/314d3bbf9bf6e65ff8095fe7f928fe85'
            );
            done();
        });

        const reqMock = httpTestingController.expectOne((req) => {
            return req.url === '//www.gravatar.com/1.json?';
        });
        expect(reqMock.request.method).toBe('JSONP');
        reqMock.flush({
            _body: {
                entry: [mockProfile]
            }
        });
    });

    it('Should return null', (done) => {
        service.getPhoto('1').subscribe((avatarUrl: string) => {
            expect(avatarUrl).toEqual(null);
            done();
        });

        const reqMock = httpTestingController.expectOne((req) => {
            return req.url === '//www.gravatar.com/1.json?';
        });
        reqMock.error(new ErrorEvent('Error'));
    });
});
