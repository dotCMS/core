import { Observable } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { PageViewService } from './../../api/services/page-view/page-view.service';
import { TestBed, async } from '@angular/core/testing';
import { PageViewResolver } from './dot-edit-page-resolver.service';

class PageViewServiceMock {
    get(url) { }
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
    'toString'
]);
activatedRouteSnapshotMock.queryParams = {};

describe('ContentTypeResolver', () => {
    let router: ActivatedRouteSnapshot;
    let pageViewResolver: PageViewResolver;
    let pageViewService: PageViewService;

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                providers: [
                    PageViewResolver,
                    { provide: PageViewService, useClass: PageViewServiceMock },
                    {
                        provide: ActivatedRouteSnapshot,
                        useValue: activatedRouteSnapshotMock
                    }
                ],
                imports: [ RouterTestingModule ]
            });

            router = TestBed.get(ActivatedRouteSnapshot);
            pageViewService = TestBed.get(PageViewService);
            pageViewResolver = TestBed.get(PageViewResolver);
        })
    );

    it('should do a resolve and return an object', () => {
        let result: any;

        spyOn(pageViewService, 'get').and.returnValue(
            Observable.of({
                object: 'Fake object'
            })
        );

        pageViewResolver.resolve(activatedRouteSnapshotMock).subscribe((fakeRes: any) => result = fakeRes);

        expect(result).toEqual({ object: 'Fake object' });
    });
});
