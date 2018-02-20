import { Observable } from 'rxjs/Observable';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { PageViewService } from '../../../../../api/services/page-view/page-view.service';
import { TestBed, async } from '@angular/core/testing';
import { EditLayoutResolver } from './dot-edit-layout-resolver.service';

class PageViewServiceMock {
    get(url) {}
}

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
    'toString'
]);
activatedRouteSnapshotMock.queryParams = {};

describe('PageViewResolver', () => {
    let router: ActivatedRouteSnapshot;
    let pageViewResolver: EditLayoutResolver;
    let pageViewService: PageViewService;

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                providers: [
                    EditLayoutResolver,
                    { provide: PageViewService, useClass: PageViewServiceMock },
                    {
                        provide: ActivatedRouteSnapshot,
                        useValue: activatedRouteSnapshotMock
                    }
                ],
                imports: [RouterTestingModule]
            });

            router = TestBed.get(ActivatedRouteSnapshot);
            pageViewService = TestBed.get(PageViewService);
            pageViewResolver = TestBed.get(EditLayoutResolver);
        })
    );

    it('should do a resolve and return an object', () => {
        let result: any;

        spyOn(pageViewService, 'get').and.returnValue(
            Observable.of({
                object: 'Fake object'
            })
        );

        pageViewResolver.resolve(activatedRouteSnapshotMock).subscribe((fakeRes: any) => (result = fakeRes));

        expect(result).toEqual({ object: 'Fake object' });
    });
});
