import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CoreWebService } from '@dotcms/dotcms-js';

import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import {
    DotCategoriesService,
    CATEGORY_API_URL,
    CATEGORY_CHILDREN_API_URL
} from './dot-categories.service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { of } from 'rxjs';
import {
    CATEGORY_SOURCE,
    DotCategory
} from '@dotcms/app/shared/models/dot-categories/dot-categories.model';

const mockCategory: DotCategory = {
    categoryId: '1222',
    categoryName: 'Test',
    key: 'adsdsd',
    sortOrder: 1,
    deleted: false,
    categoryVelocityVarName: 'sdsdsds',
    friendlyName: 'asas',
    identifier: '1222',
    inode: '2121',
    name: 'Test',
    type: 'ggg',
    source: CATEGORY_SOURCE.DB
};

describe('DotCategorysService', () => {
    let service: DotCategoriesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotCategoriesService,
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle() {
                            return of({});
                        }
                    }
                },
                {
                    provide: CoreWebService,
                    useClass: CoreWebServiceMock
                }
            ],
            imports: [HttpClientTestingModule]
        });
        service = TestBed.inject(DotCategoriesService);

        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get a categories list', () => {
        service
            .getCategories({
                first: 0,
                rows: 40,
                sortOrder: 1,
                filters: {},
                globalFilter: null
            })
            .subscribe((categories: DotCategory[]) => {
                expect(categories).toEqual([mockCategory]);
            });

        const req = httpMock.expectOne(`${CATEGORY_API_URL}?direction=ASC&per_page=40`);

        expect(req.request.method).toBe('GET');

        req.flush({
            entity: [mockCategory]
        });
    });

    it('should get a children categories list', () => {
        service
            .getChildrenCategories({
                first: 0,
                rows: 40,
                sortOrder: 1,
                filters: {
                    inode: { value: '123' }
                },
                globalFilter: null
            })
            .subscribe((categories: DotCategory[]) => {
                expect(categories).toEqual([mockCategory]);
            });

        const req = httpMock.expectOne(
            `${CATEGORY_CHILDREN_API_URL}?direction=ASC&per_page=40&inode=123`
        );

        expect(req.request.method).toBe('GET');

        req.flush({
            entity: [mockCategory]
        });
    });
});
