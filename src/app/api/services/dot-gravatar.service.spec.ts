import { Observable, of, throwError } from 'rxjs';
import { async, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { DotGravatarService } from './dot-gravatar-service';
import { Jsonp } from '@angular/http';

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

class MockJsonp {
    get(): Observable<any> {
        return of({
                _body: {
                    entry: [mockProfile]
                }
            });
    }
}

describe('DotGravatarService', () => {
    let dotGravatarService: DotGravatarService;
    let mockJsonp: Jsonp;

    beforeEach(async(() => {
        const testbed = TestBed.configureTestingModule({
            providers: [
                DotGravatarService,
                { provide: Jsonp, useClass: MockJsonp },
            ],
            imports: [RouterTestingModule]
        });

        dotGravatarService = testbed.get(DotGravatarService);
        mockJsonp = testbed.get(Jsonp);
    }));

    it('Should return the photos url', (done) => {
        spyOn(mockJsonp, 'get').and.callThrough();

        dotGravatarService.getPhoto('1').subscribe((avatarUrl: string) => {
            expect(mockJsonp.get).toHaveBeenCalledWith('//www.gravatar.com/1.json?callback=JSONP_CALLBACK');
            expect(avatarUrl).toEqual(mockProfile.photos[0].value);
            done();
        });
    });


    it('Should return null', (done) => {
        spyOn(mockJsonp, 'get').and.returnValue(throwError('Error'));

        dotGravatarService.getPhoto('1').subscribe((avatarUrl: string) => {
            expect(mockJsonp.get).toHaveBeenCalledWith('//www.gravatar.com/1.json?callback=JSONP_CALLBACK');
            expect(avatarUrl).toBeNull();
            done();
        });
    });
});
