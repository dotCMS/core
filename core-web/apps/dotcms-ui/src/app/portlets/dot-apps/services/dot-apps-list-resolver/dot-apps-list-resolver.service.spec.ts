/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DotAppsService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';

import { DotAppsListResolver } from './dot-apps-list-resolver.service';

import { appsResponse } from '../../shared/mocks';

const activatedRouteSnapshotMock: any = {};

describe('DotAppsListResolver', () => {
    let dotAppsService: DotAppsService;
    let dotAppsListResolver: DotAppsListResolver;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotAppsListResolver,
                {
                    provide: DotAppsService,
                    useValue: { get: jest.fn().mockReturnValue(of(appsResponse)) }
                },
                {
                    provide: ActivatedRouteSnapshot,
                    useValue: activatedRouteSnapshotMock
                }
            ]
        });
        dotAppsService = TestBed.inject(DotAppsService);
        dotAppsListResolver = TestBed.inject(DotAppsListResolver);
    });

    it('should get and return apps list', () => {
        jest.spyOn(dotAppsService, 'get').mockReturnValue(of(appsResponse));

        dotAppsListResolver.resolve(activatedRouteSnapshotMock).subscribe((apps: DotApp[]) => {
            expect(apps).toEqual(appsResponse);
        });
        expect(dotAppsService.get).toHaveBeenCalledTimes(1);
    });
});
