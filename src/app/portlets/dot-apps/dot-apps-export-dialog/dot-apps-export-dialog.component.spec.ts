import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { MockDotMessageService } from 'src/app/test/dot-message-service.mock';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DebugElement } from '@angular/core';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { InputTextModule } from 'primeng/inputtext';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotAppsExportDialogComponent } from './dot-apps-export-dialog.component';
import { DotAutofocusModule } from 'projects/dot-rules/src/lib/directives/dot-autofocus/dot-autofocus.module';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotAppsExportConfiguration } from '@shared/models/dot-apps/dot-apps.model';
import { By } from '@angular/platform-browser';

export class DotAppsServiceMock {
    exportConfiguration(_configuration: DotAppsExportConfiguration): Promise<string> {
        return Promise.resolve('');
    }
}

describe('DotAppsExportDialogComponent', () => {
    let fixture: ComponentFixture<DotAppsExportDialogComponent>;
    let comp: DotAppsExportDialogComponent;
    let de: DebugElement;
    let dotAppsService: DotAppsService;

    const messageServiceMock = new MockDotMessageService({
        'apps.confirmation.export.error': 'Error',
        'dot.common.dialog.accept': 'Acept',
        'dot.common.dialog.reject': 'Cancel',
        'apps.confirmation.export.header': 'Export',
        'apps.confirmation.export.password.label': 'Enter Password'
    });

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotAppsExportDialogComponent],
                imports: [
                    InputTextModule,
                    DotAutofocusModule,
                    DotDialogModule,
                    CommonModule,
                    ReactiveFormsModule,
                    DotPipesModule,
                    HttpClientTestingModule
                ],
                providers: [
                    { provide: DotAppsService, useClass: DotAppsServiceMock },
                    { provide: DotMessageService, useValue: messageServiceMock },
                ]
            }).compileComponents();

            fixture = TestBed.createComponent(DotAppsExportDialogComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            dotAppsService = TestBed.inject(DotAppsService);

            comp.showExportDialog = true;
        })
    );

    afterEach(() => {
        comp.showExportDialog = false;
        fixture.detectChanges();
    });

    it(`should have right labels and accept be disabled`, async () => {
        fixture.detectChanges();
        comp.form.setValue({
            password: ''
        });
        await fixture.whenStable();
        const dialog = de.query(By.css('dot-dialog'));
        const inputPassword = de.query(By.css('input'));
        expect(dialog.componentInstance.header).toBe(
            messageServiceMock.get('apps.confirmation.export.header')
        );
        expect(dialog.componentInstance.appendToBody).toBe(true);
        expect(inputPassword.nativeElement.placeholder).toBe(
            messageServiceMock.get('apps.confirmation.export.password.label')
        );
        expect(dialog.componentInstance.actions.accept.label).toBe(
            messageServiceMock.get('dot.common.dialog.accept')
        );
        expect(dialog.componentInstance.actions.cancel.label).toBe(
            messageServiceMock.get('dot.common.dialog.reject')
        );
        expect(dialog.componentInstance.actions.accept.disabled).toBe(true);
    });

    it(`should clear values when dialog closed`, async () => {
        fixture.detectChanges();
        spyOn(comp.form, 'reset');
        await fixture.whenStable();
        comp.form.setValue({
            password: 'test'
        });

        fixture.detectChanges();
        const cancelBtn = de.queryAll(By.css('button'))[0];
        cancelBtn.nativeElement.click();

        expect(comp.exportErrorMessage).toBe('');
        expect(comp.site).toBe(null);
        expect(comp.showExportDialog).toBe(false);
        expect(comp.form.reset).toHaveBeenCalledTimes(1);
    });

    it(`should send configuration to export all apps and close dialog`, async () => {
        fixture.detectChanges();
        spyOn(dotAppsService, 'exportConfiguration').and.returnValue(Promise.resolve(''));
        spyOn(comp, 'closeExportDialog').and.callThrough();
        const expectedConfiguration: DotAppsExportConfiguration = {
            password: 'test',
            exportAll: true,
            appKeysBySite: {}
        };

        await fixture.whenStable();
        comp.form.setValue({
            password: 'test'
        });

        fixture.detectChanges();
        const acceptBtn = de.queryAll(By.css('footer button'))[1];
        acceptBtn.nativeElement.click();
        await fixture.whenStable();
        expect(dotAppsService.exportConfiguration).toHaveBeenCalledWith(expectedConfiguration);
        expect(comp.closeExportDialog).toHaveBeenCalledTimes(1);
    });

    it(`should send configuration to export all sites from a single app and close dialog`, async () => {
        comp.app = {
            allowExtraParams: false,
            key: 'test-key',
            name: 'test',
            sites: [
                {
                    id: 'Site1',
                    name: 'Site 1',
                    configured: true
                },
                {
                    id: 'Site2',
                    name: 'Site 2',
                    configured: true
                }
            ]
        };
        fixture.detectChanges();
        spyOn(dotAppsService, 'exportConfiguration').and.returnValue(Promise.resolve(''));
        spyOn(comp, 'closeExportDialog').and.callThrough();
        const expectedConfiguration: DotAppsExportConfiguration = {
            password: 'test',
            exportAll: false,
            appKeysBySite: {
                Site1: ['test-key'],
                Site2: ['test-key']
            }
        };

        await fixture.whenStable();
        comp.form.setValue({
            password: 'test'
        });

        fixture.detectChanges();
        const acceptBtn = de.queryAll(By.css('footer button'))[1];
        acceptBtn.nativeElement.click();
        await fixture.whenStable();
        expect(dotAppsService.exportConfiguration).toHaveBeenCalledWith(expectedConfiguration);
        expect(comp.closeExportDialog).toHaveBeenCalledTimes(1);
    });

    it(`should send configuration to export a single site from a single app and close dialog`, async () => {
        comp.app = {
            allowExtraParams: false,
            key: 'test-key',
            name: 'test',
            sites: [
                {
                    id: 'Site1',
                    name: 'Site 1',
                    configured: true
                },
                {
                    id: 'Site2',
                    name: 'Site 2',
                    configured: true
                }
            ]
        };
        comp.site = {
            id: 'Site1',
            name: 'Site 1',
            configured: true
        };
        fixture.detectChanges();
        spyOn(dotAppsService, 'exportConfiguration').and.returnValue(Promise.resolve(''));
        spyOn(comp, 'closeExportDialog').and.callThrough();
        const expectedConfiguration: DotAppsExportConfiguration = {
            password: 'test',
            exportAll: false,
            appKeysBySite: {
                Site1: ['test-key']
            }
        };

        await fixture.whenStable();
        comp.form.setValue({
            password: 'test'
        });

        fixture.detectChanges();
        const acceptBtn = de.queryAll(By.css('footer button'))[1];
        acceptBtn.nativeElement.click();
        await fixture.whenStable();
        expect(dotAppsService.exportConfiguration).toHaveBeenCalledWith(expectedConfiguration);
        expect(comp.closeExportDialog).toHaveBeenCalledTimes(1);
    });
});
