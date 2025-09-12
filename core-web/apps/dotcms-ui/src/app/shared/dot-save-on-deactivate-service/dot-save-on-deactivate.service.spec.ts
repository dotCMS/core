import { Observable, of as observableOf } from 'rxjs';

import { Component } from '@angular/core';

import { DotAlertConfirmService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotAlertConfirm } from '@dotcms/dotcms-models';
import { LoginServiceMock } from '@dotcms/utils-testing';

import { DotSaveOnDeactivateService } from './dot-save-on-deactivate.service';
import { OnSaveDeactivate } from './save-on-deactivate';

import { DOTTestBed } from '../../test/dot-test-bed';

@Component({
    selector: 'dot-test',
    template: '<h1>Test</h1>',
    standalone: false
})
class MockComponent implements OnSaveDeactivate {
    shouldSaveBefore(): boolean {
        return true;
    }

    onDeactivateSave(): Observable<boolean> {
        return observableOf(true);
    }

    getSaveWarningMessages(): DotAlertConfirm {
        return { header: 'Header', message: 'message' };
    }
}

describe('DotSaveOnDeactivateService', () => {
    let dotSaveOnDeactivateService: DotSaveOnDeactivateService;
    let mockComponent: MockComponent;
    let dotDialogService: DotAlertConfirmService;
    beforeEach(() => {
        const testbed = DOTTestBed.configureTestingModule({
            declarations: [MockComponent],
            providers: [
                DotSaveOnDeactivateService,
                DotAlertConfirmService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ],
            imports: []
        });
        dotSaveOnDeactivateService = testbed.get(DotSaveOnDeactivateService);
        dotDialogService = testbed.get(DotAlertConfirmService);
        mockComponent = new MockComponent();
    });

    it('should return true if there is not changes in the model', () => {
        jest.spyOn(mockComponent, 'shouldSaveBefore').mockReturnValue(false);

        dotSaveOnDeactivateService.canDeactivate(mockComponent, null, null).subscribe((val) => {
            expect(val).toBeTruthy();
        });
    });

    it('should return true AND call onDeactivateSave', () => {
        jest.spyOn(mockComponent, 'onDeactivateSave');
        jest.spyOn(dotDialogService, 'confirm').mockImplementation((conf) => {
            conf.accept();
        });
        dotSaveOnDeactivateService.canDeactivate(mockComponent, null, null).subscribe((val) => {
            expect(val).toBeTruthy();
            expect(mockComponent.onDeactivateSave).toHaveBeenCalled();
        });
    });

    it('should return true if the user decide NOT to save the latest changes', () => {
        jest.spyOn(mockComponent, 'onDeactivateSave');
        jest.spyOn(dotDialogService, 'confirm').mockImplementation((conf) => {
            conf.reject();
        });
        dotSaveOnDeactivateService.canDeactivate(mockComponent, null, null).subscribe((val) => {
            expect(val).toBeTruthy();
            expect(mockComponent.onDeactivateSave).toHaveBeenCalledTimes(0);
        });
    });

    it('should return false if the save fails and stay in the current route', () => {
        jest.spyOn(mockComponent, 'onDeactivateSave').mockReturnValue(observableOf(false));
        jest.spyOn(dotDialogService, 'confirm').mockImplementation((conf) => {
            conf.accept();
        });

        dotSaveOnDeactivateService.canDeactivate(mockComponent, null, null).subscribe((val) => {
            expect(val).toBeFalsy();
            expect(mockComponent.onDeactivateSave).toHaveBeenCalledTimes(1);
        });
    });
});
