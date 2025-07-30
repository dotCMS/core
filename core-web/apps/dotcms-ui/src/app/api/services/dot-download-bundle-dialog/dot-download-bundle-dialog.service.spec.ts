import { TestBed } from '@angular/core/testing';

import { DotDownloadBundleDialogService } from './dot-download-bundle-dialog.service';

describe('DotDownloadBundleDialogService', () => {
    let service: DotDownloadBundleDialogService;
    let bundleID: string;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [DotDownloadBundleDialogService] });
        service = TestBed.inject(DotDownloadBundleDialogService);
        service.showDialog$.subscribe((data: string) => (bundleID = data));
    });

    it('should receive bundleID', () => {
        service.open('ZXC!2');
        expect(bundleID).toEqual('ZXC!2');
    });
});
