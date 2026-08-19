import { Observable, of } from 'rxjs';

import { TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DotCreateContentletResolver } from './dot-create-contentlet.resolver.service';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';

// A plain object rather than `jest.fn<T>(name, methods)`: that shape is `jasmine.createSpyObj`
// migrated mechanically, and `jest.fn` accepts neither argument. The spec only assigns and reads
// `paramMap` / `queryParamMap`.
const activatedRouteSnapshotMock = {
    paramMap: {} as { get?: () => string | null },
    queryParamMap: {} as { get?: () => string | null }
} as unknown as ActivatedRouteSnapshot & {
    paramMap: { get?: () => string | null };
    queryParamMap: { get?: () => string | null };
};

class DotContentletEditorServiceMock {
    // Matches the real service, which returns `Observable<string>`. Declared `void`, every
    // `.mockReturnValue(of(...))` below was assigning an observable to a method that returns nothing.
    getActionUrl(_url: string): Observable<string> {
        return of('');
    }
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
        jest.spyOn(dotContentletEditorService, 'getActionUrl').mockReturnValue(of('urlTest'));

        dotCreateContentletResolver.resolve(activatedRouteSnapshotMock).subscribe((url: string) => {
            expect(url).toEqual('urlTest');
        });
    });

    it('should append the folder inode with `?` when the action url has no query string', () => {
        activatedRouteSnapshotMock.queryParamMap.get = () => 'inode-1';
        jest.spyOn(dotContentletEditorService, 'getActionUrl').mockReturnValue(of('urlTest'));

        dotCreateContentletResolver.resolve(activatedRouteSnapshotMock).subscribe((url: string) => {
            expect(url).toEqual('urlTest?folder=inode-1');
        });
    });

    it('should append the folder inode with `&` when the action url already has a query string', () => {
        activatedRouteSnapshotMock.queryParamMap.get = () => 'inode-1';
        jest.spyOn(dotContentletEditorService, 'getActionUrl').mockReturnValue(
            of('urlTest?foo=bar')
        );

        dotCreateContentletResolver.resolve(activatedRouteSnapshotMock).subscribe((url: string) => {
            expect(url).toEqual('urlTest?foo=bar&folder=inode-1');
        });
    });

    it('should encode the folder inode', () => {
        activatedRouteSnapshotMock.queryParamMap.get = () => 'a b/c';
        jest.spyOn(dotContentletEditorService, 'getActionUrl').mockReturnValue(of('urlTest'));

        dotCreateContentletResolver.resolve(activatedRouteSnapshotMock).subscribe((url: string) => {
            expect(url).toEqual('urlTest?folder=a%20b%2Fc');
        });
    });

    it('should not append anything when there is no folder query param', () => {
        jest.spyOn(dotContentletEditorService, 'getActionUrl').mockReturnValue(of('urlTest'));

        dotCreateContentletResolver.resolve(activatedRouteSnapshotMock).subscribe((url: string) => {
            expect(url).toEqual('urlTest');
        });
    });
});
