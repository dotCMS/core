import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { MockDotMessageService } from 'src/app/test/dot-message-service.mock';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, DebugElement, Input } from '@angular/core';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { InputTextModule } from 'primeng/inputtext';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotAppsImportExportDialogComponent } from './dot-apps-import-export-dialog.component';
import { DotAutofocusModule } from 'projects/dot-rules/src/lib/directives/dot-autofocus/dot-autofocus.module';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import {
    DotApps,
    DotAppsExportConfiguration,
    DotAppsImportConfiguration,
    DotAppsSites
} from '@shared/models/dot-apps/dot-apps.model';
import { By } from '@angular/platform-browser';
import { Observable, of } from 'rxjs';

export class DotAppsServiceMock {
    exportConfiguration(_configuration: DotAppsExportConfiguration): Promise<string> {
        return Promise.resolve('');
    }
    importConfiguration(_configuration: DotAppsImportConfiguration): Observable<string> {
        return of('');
    }
}

@Component({
    selector: 'dot-host-component',
    template: `
        <dot-apps-import-export-dialog
            [action]="action"
            [app]="app"
            [site]="site"
            [show]="true"
            (resolved)="resolveHandler($event)"
        ></dot-apps-import-export-dialog>
    `
})
class HostTestComponent {
    @Input() action?: string;
    @Input() app?: DotApps;
    @Input() site?: DotAppsSites;
    resolveHandler(_$event) {}
}

describe('DotAppsImportExportDialogComponent', () => {
    let hostFixture: ComponentFixture<HostTestComponent>;
    let hostComponent: HostTestComponent;
    let comp: DotAppsImportExportDialogComponent;
    let de: DebugElement;
    let dotAppsService: DotAppsService;

    const messageServiceMock = new MockDotMessageService({
        'apps.confirmation.export.error': 'Error',
        'dot.common.dialog.accept': 'Acept',
        'dot.common.dialog.reject': 'Cancel',
        'apps.confirmation.export.header': 'Export',
        'apps.confirmation.export.password.label': 'Enter Password',
        'apps.confirmation.import.password.label': 'Enter Password to decrypt',
        'apps.confirmation.import.header': 'Import Configuration'
    });

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotAppsImportExportDialogComponent, HostTestComponent],
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
                    { provide: DotMessageService, useValue: messageServiceMock }
                ]
            }).compileComponents();

            hostFixture = TestBed.createComponent(HostTestComponent);
            hostComponent = hostFixture.componentInstance;
            de = hostFixture.debugElement;
            comp = hostFixture.debugElement.query(By.css('dot-apps-import-export-dialog'))
                .componentInstance;
            dotAppsService = TestBed.inject(DotAppsService);
            comp.show = true;
        })
    );

    afterEach(() => {
        comp.show = false;
        hostFixture.detectChanges();
    });

    describe('Import dialog', () => {
        beforeEach(() => {
            hostComponent.action = 'Import';
        });

        it(`should have right labels and accept be disabled`, async () => {
            hostFixture.detectChanges();
            comp.form.setValue({
                password: '',
                importFile: null
            });
            await hostFixture.whenStable();
            const dialog = de.query(By.css('dot-dialog'));
            const inputPassword = de.query(By.css('input'));
            expect(dialog.componentInstance.header).toBe(
                messageServiceMock.get('apps.confirmation.import.header')
            );
            expect(dialog.componentInstance.appendToBody).toBe(true);
            expect(inputPassword.nativeElement.placeholder).toBe(
                messageServiceMock.get('apps.confirmation.import.password.label')
            );
            expect(dialog.componentInstance.actions.accept.label).toBe(
                messageServiceMock.get('dot.common.dialog.accept')
            );
            expect(dialog.componentInstance.actions.cancel.label).toBe(
                messageServiceMock.get('dot.common.dialog.reject')
            );
            expect(dialog.componentInstance.actions.accept.disabled).toBe(true);
        });

        it(`should send configuration to import apps and close dialog`, async () => {
            hostFixture.detectChanges();
            spyOn(dotAppsService, 'importConfiguration').and.returnValue(of(''));
            spyOn(comp, 'closeExportDialog').and.callThrough();
            spyOn(comp.resolved, 'emit').and.callThrough();
            const expectedConfiguration: DotAppsImportConfiguration = {
                file: undefined,
                json: { password: 'test' }
            };

            await hostFixture.whenStable();
            comp.form.setValue({
                password: 'test',
                importFile: 'test'
            });

            hostFixture.detectChanges();
            const acceptBtn = de.queryAll(By.css('footer button'))[1];
            acceptBtn.nativeElement.click();
            await hostFixture.whenStable();
            expect(dotAppsService.importConfiguration).toHaveBeenCalledWith(expectedConfiguration);
            expect(comp.closeExportDialog).toHaveBeenCalledTimes(1);
            expect(comp.resolved.emit).toHaveBeenCalledTimes(1);
        });
    });

    describe('Export dialog', () => {
        beforeEach(() => {
            hostComponent.action = 'Export';
        });

        it(`should have right params and accept be disabled`, async () => {
            hostFixture.detectChanges();
            comp.form.setValue({
                password: ''
            });
            await hostFixture.whenStable();
            const dialog = de.query(By.css('dot-dialog'));
            const inputPassword = de.query(By.css('input'));
            expect(dialog.componentInstance.header).toBe(
                messageServiceMock.get('apps.confirmation.export.header')
            );
            expect(dialog.componentInstance.appendToBody).toBe(true);
            expect(inputPassword.attributes['pPassword']).not.toBeUndefined();
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
            hostFixture.detectChanges();
            spyOn(comp.form, 'reset');
            await hostFixture.whenStable();
            comp.form.setValue({
                password: 'test'
            });

            hostFixture.detectChanges();
            const cancelBtn = de.queryAll(By.css('button'))[0];
            cancelBtn.nativeElement.click();

            expect(comp.errorMessage).toBe('');
            expect(comp.site).toBe(null);
            expect(comp.show).toBe(false);
            expect(comp.form.reset).toHaveBeenCalledTimes(1);
        });

        it(`should send configuration to export all apps and close dialog`, async () => {
            hostFixture.detectChanges();
            spyOn(dotAppsService, 'exportConfiguration').and.returnValue(Promise.resolve(''));
            spyOn(comp, 'closeExportDialog').and.callThrough();
            const expectedConfiguration: DotAppsExportConfiguration = {
                password: 'test',
                exportAll: true,
                appKeysBySite: {}
            };

            await hostFixture.whenStable();
            comp.form.setValue({
                password: 'test'
            });

            hostFixture.detectChanges();
            const acceptBtn = de.queryAll(By.css('footer button'))[1];
            acceptBtn.nativeElement.click();
            await hostFixture.whenStable();
            expect(dotAppsService.exportConfiguration).toHaveBeenCalledWith(expectedConfiguration);
            expect(comp.closeExportDialog).toHaveBeenCalledTimes(1);
        });

        it(`should send configuration to export all sites from a single app and close dialog`, async () => {
            hostComponent.app = {
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
            hostFixture.detectChanges();
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

            await hostFixture.whenStable();
            comp.form.setValue({
                password: 'test'
            });

            hostFixture.detectChanges();
            const acceptBtn = de.queryAll(By.css('footer button'))[1];
            acceptBtn.nativeElement.click();
            await hostFixture.whenStable();
            expect(dotAppsService.exportConfiguration).toHaveBeenCalledWith(expectedConfiguration);
            expect(comp.closeExportDialog).toHaveBeenCalledTimes(1);
        });

        it(`should send configuration to export a single site from a single app and close dialog`, async () => {
            hostComponent.app = {
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
            hostComponent.site = {
                id: 'Site1',
                name: 'Site 1',
                configured: true
            };
            hostFixture.detectChanges();
            spyOn(dotAppsService, 'exportConfiguration').and.returnValue(Promise.resolve(''));
            spyOn(comp, 'closeExportDialog').and.callThrough();
            const expectedConfiguration: DotAppsExportConfiguration = {
                password: 'test',
                exportAll: false,
                appKeysBySite: {
                    Site1: ['test-key']
                }
            };

            await hostFixture.whenStable();
            comp.form.setValue({
                password: 'test'
            });

            hostFixture.detectChanges();
            const acceptBtn = de.queryAll(By.css('footer button'))[1];
            acceptBtn.nativeElement.click();
            await hostFixture.whenStable();
            expect(dotAppsService.exportConfiguration).toHaveBeenCalledWith(expectedConfiguration);
            expect(comp.closeExportDialog).toHaveBeenCalledTimes(1);
        });
    });
});
