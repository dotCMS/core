import { of } from 'rxjs';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CATEGORY_SOURCE, DotCategory } from '@dotcms/dotcms-models';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import {
    CATEGORY_API_URL,
    CATEGORY_CHILDREN_API_URL,
    DotCategoriesService
} from './dot-categories.service';

const mockCategory: DotCategory = {
    active: false,
    childrenCount: 0,
    description: '',
    iDate: 0,
    keywords: '',
    owner: '',
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
