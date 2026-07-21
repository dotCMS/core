/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DotCreateContentletResolver } from './dot-create-contentlet.resolver.service';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';

const activatedRouteSnapshotMock: any = jest.fn<ActivatedRouteSnapshot>('ActivatedRouteSnapshot', [
    'toString'
]);
activatedRouteSnapshotMock.paramMap = {};
activatedRouteSnapshotMock.queryParamMap = {};

class DotContentletEditorServiceMock {
    getActionUrl(_url: string) {}
}

describe('DotCreateContentletResolver', () => {
    let dotCreateContentletResolver: DotCreateContentletResolver;
    let dotContentletEditorService: DotContentletEditorService;

    beforeEach(waitForAsync(() => {
        const testbed = TestBed.configureTestingModule({
            providers: [
                DotCreateContentletResolver,
                {
                    provide: DotContentletEditorService,
                    useClass: DotContentletEditorServiceMock
                }
            ]
        });
        dotCreateContentletResolver = testbed.inject(DotCreateContentletResolver);
        dotContentletEditorService = testbed.inject(DotContentletEditorService);

        activatedRouteSnapshotMock.paramMap.get = () => '123';
        // No `folder` query param by default; individual tests override.
        activatedRouteSnapshotMock.queryParamMap.get = () => null;
    }));

    it('should get and return the action url', () => {
        jest.spyOn<any>(dotContentletEditorService, 'getActionUrl').mockReturnValue(of('urlTest'));

        dotCreateContentletResolver.resolve(activatedRouteSnapshotMock).subscribe((url: string) => {
            expect(url).toEqual('urlTest');
        });
    });

    it('should append the folder inode with `?` when the action url has no query string', () => {
        activatedRouteSnapshotMock.queryParamMap.get = () => 'inode-1';
        jest.spyOn<any>(dotContentletEditorService, 'getActionUrl').mockReturnValue(of('urlTest'));

        dotCreateContentletResolver.resolve(activatedRouteSnapshotMock).subscribe((url: string) => {
            expect(url).toEqual('urlTest?folder=inode-1');
        });
    });

    it('should append the folder inode with `&` when the action url already has a query string', () => {
        activatedRouteSnapshotMock.queryParamMap.get = () => 'inode-1';
        jest.spyOn<any>(dotContentletEditorService, 'getActionUrl').mockReturnValue(
            of('urlTest?foo=bar')
        );

        dotCreateContentletResolver.resolve(activatedRouteSnapshotMock).subscribe((url: string) => {
            expect(url).toEqual('urlTest?foo=bar&folder=inode-1');
        });
    });

    it('should encode the folder inode', () => {
        activatedRouteSnapshotMock.queryParamMap.get = () => 'a b/c';
        jest.spyOn<any>(dotContentletEditorService, 'getActionUrl').mockReturnValue(of('urlTest'));

        dotCreateContentletResolver.resolve(activatedRouteSnapshotMock).subscribe((url: string) => {
            expect(url).toEqual('urlTest?folder=a%20b%2Fc');
        });
    });

    it('should not append anything when there is no folder query param', () => {
        jest.spyOn<any>(dotContentletEditorService, 'getActionUrl').mockReturnValue(of('urlTest'));

        dotCreateContentletResolver.resolve(activatedRouteSnapshotMock).subscribe((url: string) => {
            expect(url).toEqual('urlTest');
        });
    });
});
