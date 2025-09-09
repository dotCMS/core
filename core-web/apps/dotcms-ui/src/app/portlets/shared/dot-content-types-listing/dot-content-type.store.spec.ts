import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotContentTypeService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCopyContentTypeDialogFormFields } from '@dotcms/dotcms-models';
import {
    CoreWebServiceMock,
    dotcmsContentTypeBasicMock,
    mockResponseView
} from '@dotcms/utils-testing';

import { DotContentTypeStore } from './dot-content-type.store';

describe('DotContentTypeComponentStore', () => {
    let store: DotContentTypeStore;
    let dotContentTypeService: DotContentTypeService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let router: Router;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule, RouterTestingModule],
            providers: [
                DotContentTypeService,
                DotContentTypeStore,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle: jest.fn().mockReturnValue(of({}))
                    }
                }
            ]
        });
        store = TestBed.inject(DotContentTypeStore);
        dotContentTypeService = TestBed.inject(DotContentTypeService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        router = TestBed.inject(Router);
    });

    describe('updaters', () => {
        it('should update isSaving', () => {
            store.isSaving(true);
            store.state$.subscribe((data) => {
                expect(data.isSaving).toEqual(true);
            });
        });
        it('should update assetSelected', () => {
            const contentTypeSelectedId = 'content-type-id';
            store.setAssetSelected(contentTypeSelectedId);
            store.state$.subscribe((data) => {
                expect(data.assetSelected).toEqual(contentTypeSelectedId);
            });
        });
    });

    describe('effects', () => {
        it('should save Content Type Copy values', () => {
            jest.spyOn(dotContentTypeService, 'saveCopyContentType').mockReturnValue(
                of({
                    ...dotcmsContentTypeBasicMock,
                    id: '1234567890',
                    name: 'ContentTypeName',
                    variable: 'helloVariable',
                    baseType: 'testBaseType'
                })
            );

            jest.spyOn(router, 'navigate');

            store.setAssetSelected('content-type-id');

            const formFields: DotCopyContentTypeDialogFormFields = {
                name: 'new-name',
                host: 'host',
                icon: 'icon',
                folder: 'folder',
                variable: 'validVariableName'
            };

            store.saveCopyDialog(formFields);

            expect(dotContentTypeService.saveCopyContentType).toHaveBeenCalledWith(
                'content-type-id',
                {
                    name: 'new-name',
                    host: 'host',
                    icon: 'icon',
                    folder: 'folder',
                    variable: 'validVariableName'
                }
            );

            expect(router.navigate).toHaveBeenCalledWith([
                '/content-types-angular/edit',
                '1234567890'
            ]);
        });

        it('should handler error on update template', (done) => {
            const error = new HttpErrorResponse(mockResponseView(400));
            jest.spyOn(dotContentTypeService, 'saveCopyContentType').mockReturnValue(
                throwError(error)
            );

            store.saveCopyDialog({
                name: 'new-name',
                host: 'host',
                icon: 'icon',
                folder: 'folder',
                variable: 'validVariableName'
            });

            expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(error);

            store.isSaving$.subscribe((resp) => {
                expect(resp).toBe(false);
                done();
            });
        });
    });
});
