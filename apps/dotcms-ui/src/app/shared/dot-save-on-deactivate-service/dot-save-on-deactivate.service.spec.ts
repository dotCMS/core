import { of as observableOf, Observable } from 'rxjs';
import { DotSaveOnDeactivateService } from './dot-save-on-deactivate.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { DOTTestBed } from '../../test/dot-test-bed';
import { LoginService } from '@dotcms/dotcms-js';
import { LoginServiceMock } from '../../test/login-service.mock';
import { OnSaveDeactivate } from './save-on-deactivate';
import { DotAlertConfirm } from '../models/dot-alert-confirm/dot-alert-confirm.model';
import { Component } from '@angular/core';

@Component({
    selector: 'dot-test',
    template: '<h1>Test</h1>'
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
        spyOn(mockComponent, 'shouldSaveBefore').and.returnValue(false);

        dotSaveOnDeactivateService.canDeactivate(mockComponent, null, null).subscribe((val) => {
            expect(val).toBeTruthy();
        });
    });

    it('should return true AND call onDeactivateSave', () => {
        spyOn(mockComponent, 'onDeactivateSave').and.callThrough();
        spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
            conf.accept();
        });
        dotSaveOnDeactivateService.canDeactivate(mockComponent, null, null).subscribe((val) => {
            expect(val).toBeTruthy();
            expect(mockComponent.onDeactivateSave).toHaveBeenCalled();
        });
    });

    it('should return true if the user decide NOT to save the latest changes', () => {
        spyOn(mockComponent, 'onDeactivateSave').and.callThrough();
        spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
            conf.reject();
        });
        dotSaveOnDeactivateService.canDeactivate(mockComponent, null, null).subscribe((val) => {
            expect(val).toBeTruthy();
            expect(mockComponent.onDeactivateSave).toHaveBeenCalledTimes(0);
        });
    });

    it('should return false if the save fails and stay in the current route', () => {
        spyOn(mockComponent, 'onDeactivateSave').and.returnValue(observableOf(false));
        spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
            conf.accept();
        });

        dotSaveOnDeactivateService.canDeactivate(mockComponent, null, null).subscribe((val) => {
            expect(val).toBeFalsy();
            expect(mockComponent.onDeactivateSave).toHaveBeenCalledTimes(1);
        });
    });
});
