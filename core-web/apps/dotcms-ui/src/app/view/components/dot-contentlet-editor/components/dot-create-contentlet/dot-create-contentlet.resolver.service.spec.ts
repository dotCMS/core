/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';

import { DotCreateContentletResolver } from './dot-create-contentlet.resolver.service';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';

const activatedRouteSnapshotMock: any = jasmine.createSpyObj<ActivatedRouteSnapshot>(
    'ActivatedRouteSnapshot',
    ['toString']
);
activatedRouteSnapshotMock.paramMap = {};

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
    }));

    it('should get and return apps with configurations', () => {
        activatedRouteSnapshotMock.paramMap.get = () => '123';
        spyOn<any>(dotContentletEditorService, 'getActionUrl').and.returnValue(of('urlTest'));

        dotCreateContentletResolver.resolve(activatedRouteSnapshotMock).subscribe((url: string) => {
            expect(url).toEqual('urlTest');
        });
    });
});
