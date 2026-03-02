/* eslint-disable @typescript-eslint/no-explicit-any */

import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of as observableOf, throwError as observableThrowError } from 'rxjs';

import { ActivatedRouteSnapshot } from '@angular/router';

import {
    DotContentTypesInfoService,
    DotCrudService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotRouterService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotMessageDisplayServiceMock, LoginServiceMock } from '@dotcms/utils-testing';

import { DotContentTypeEditResolver } from './dot-content-types-edit-resolver.service';

const getDataByIdSpy = jest.fn();
const gotoPortletSpy = jest.fn();
const addNewBreadcrumbSpy = jest.fn();
const handleErrorSpy = jest.fn().mockReturnValue(observableOf({ redirected: false }));

function createRouteSnapshot(paramMapGet: (key: string) => string | null): ActivatedRouteSnapshot {
    return {
        paramMap: { get: paramMapGet },
        data: {}
    } as unknown as ActivatedRouteSnapshot;
}

describe('DotContentTypeEditResolver', () => {
    let spectator: SpectatorService<DotContentTypeEditResolver>;

    const createService = createServiceFactory({
        service: DotContentTypeEditResolver,
        providers: [
            DotContentTypesInfoService,
            {
                provide: DotCrudService,
                useValue: { getDataById: getDataByIdSpy }
            },
            {
                provide: DotHttpErrorManagerService,
                useValue: { handle: handleErrorSpy }
            },
            {
                provide: DotMessageDisplayService,
                useClass: DotMessageDisplayServiceMock
            },
            {
                provide: DotRouterService,
                useValue: { gotoPortlet: gotoPortletSpy }
            },
            { provide: LoginService, useClass: LoginServiceMock },
            {
                provide: GlobalStore,
                useValue: { addNewBreadcrumb: addNewBreadcrumbSpy }
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        getDataByIdSpy.mockReset();
        gotoPortletSpy.mockReset();
        addNewBreadcrumbSpy.mockReset();
        handleErrorSpy.mockReset();
        handleErrorSpy.mockReturnValue(observableOf({ redirected: false }));
    });

    it('should get and return a content type', (done) => {
        const route = createRouteSnapshot((key) => (key === 'id' ? '123' : null));
        const contentType = { fake: 'content-type', object: 'right?' };
        getDataByIdSpy.mockReturnValue(observableOf(contentType));

        spectator.service.resolve(route).subscribe((result: any) => {
            expect(result).toEqual(contentType);
            expect(getDataByIdSpy).toHaveBeenCalledWith('v1/contenttype', '123');
            expect(getDataByIdSpy).toHaveBeenCalledTimes(1);
            done();
        });
    });

    it("should redirect to content-types if content type it's not found", (done) => {
        const route = createRouteSnapshot((key) => (key === 'id' ? 'invalid-id' : null));
        getDataByIdSpy.mockReturnValue(
            observableThrowError(() => ({
                bodyJsonObject: { error: '' },
                response: { status: 403 }
            }))
        );
        handleErrorSpy.mockReturnValue(observableOf({ redirected: false }));

        spectator.service.resolve(route).subscribe({
            next: () => {
                expect(getDataByIdSpy).toHaveBeenCalledWith('v1/contenttype', 'invalid-id');
                expect(getDataByIdSpy).toHaveBeenCalledTimes(1);
                expect(gotoPortletSpy).toHaveBeenCalledWith('/content-types-angular', {
                    replaceUrl: true
                });
                done();
            },
            error: () => {
                // tap(contentType => addNewBreadcrumb(...)) throws when contentType is null
                expect(getDataByIdSpy).toHaveBeenCalledWith('v1/contenttype', 'invalid-id');
                expect(getDataByIdSpy).toHaveBeenCalledTimes(1);
                expect(gotoPortletSpy).toHaveBeenCalledWith('/content-types-angular', {
                    replaceUrl: true
                });
                done();
            }
        });
    });

    it.skip('should get and return null and go to home', () => {
        const route = createRouteSnapshot((key) => (key === 'id' ? '123' : null));
        getDataByIdSpy.mockReturnValue(
            observableThrowError(() => ({
                bodyJsonObject: { error: '' },
                response: { status: 403 }
            }))
        );
        handleErrorSpy.mockReturnValue(observableOf({ redirected: false }));

        spectator.service.resolve(route).subscribe({
            error: () => {
                expect(getDataByIdSpy).toHaveBeenCalledWith('v1/contenttype', '123');
                expect(getDataByIdSpy).toHaveBeenCalledTimes(1);
                expect(gotoPortletSpy).toHaveBeenCalledWith('/content-types-angular', {
                    replaceUrl: true
                });
            }
        });
    });

    it.skip('should return a content type placeholder base on type', (done) => {
        const route = createRouteSnapshot((key) => (key === 'type' ? 'content' : null));
        getDataByIdSpy.mockReturnValue(observableOf(false));

        spectator.service.resolve(route).subscribe((res: DotCMSContentType) => {
            expect(res).toEqual({
                baseType: 'content',
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                defaultType: false,
                fields: [],
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                host: null,
                iDate: null,
                id: null,
                layout: [],
                modDate: null,
                multilingualable: false,
                nEntries: 0,
                name: null,
                owner: '123',
                system: false,
                variable: null,
                versionable: false,
                workflows: []
            });
            done();
        });
    });
});
