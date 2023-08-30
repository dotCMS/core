/* eslint-disable @typescript-eslint/no-explicit-any */

import * as _ from 'lodash';
import { of } from 'rxjs';

import { CommonModule, DecimalPipe } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';
import { InputSwitchModule } from 'primeng/inputswitch';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';

import {
    DotAlertConfirmService,
    DotCurrentUserService,
    DotDevicesService,
    DotMessageService,
    DotPersonalizeService
} from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import {
    DEFAULT_VARIANT_NAME,
    DotExperimentStatus,
    DotPageMode,
    DotPageRender,
    DotPageRenderState,
    DotVariantData
} from '@dotcms/dotcms-models';
import {
    CoreWebServiceMock,
    dotcmsContentletMock,
    DotDevicesServiceMock,
    DotPageStateServiceMock,
    DotPersonalizeServiceMock,
    getExperimentMock,
    MockDotMessageService,
    mockDotRenderedPage,
    mockUser
} from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditPageLockInfoSeoComponent } from './components/dot-edit-page-lock-info-seo/dot-edit-page-lock-info-seo.component';
import { DotEditPageStateControllerSeoComponent } from './dot-edit-page-state-controller-seo.component';

import { DotPageStateService } from '../../../content/services/dot-page-state/dot-page-state.service';
import { DotDeviceSelectorSeoComponent } from '../dot-device-selector-seo/dot-device-selector-seo.component';

const mockDotMessageService = new MockDotMessageService({
    'editpage.toolbar.edit.page': 'Edit',
    'editpage.toolbar.edit.page.clipboard': 'Edit Page Content',
    'editpage.toolbar.live.page': 'Live',
    'editpage.toolbar.preview.page': 'Preview',
    'editpage.toolbar.preview.page.clipboard': 'Preview Page',
    'editpage.content.steal.lock.confirmation.message.header': 'Lock',
    'editpage.content.steal.lock.confirmation.message': 'Steal lock',
    'editpage.personalization.confirm.message': 'Are you sure?',
    'editpage.personalization.confirm.header': 'Personalization',
    'editpage.personalization.confirm.with.lock': 'Also steal lock',
    'editpage.toolbar.page.locked.by.user': 'Page locked by {0}'
});

const EXPERIMENT_MOCK = getExperimentMock(1);

const dotVariantDataMock: DotVariantData = {
    variant: {
        id: EXPERIMENT_MOCK.trafficProportion.variants[1].id,
        url: EXPERIMENT_MOCK.trafficProportion.variants[1].url,
        title: EXPERIMENT_MOCK.trafficProportion.variants[1].name,
        isOriginal: EXPERIMENT_MOCK.trafficProportion.variants[1].name === DEFAULT_VARIANT_NAME
    },
    pageId: EXPERIMENT_MOCK.pageId,
    experimentId: EXPERIMENT_MOCK.id,
    experimentStatus: EXPERIMENT_MOCK.status,
    experimentName: EXPERIMENT_MOCK.name,
    mode: DotPageMode.PREVIEW
};

const pageRenderStateMock: DotPageRenderState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage())
);

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-edit-page-state-controller-seo [pageState]="pageState" [variant]="variant">
        </dot-edit-page-state-controller-seo>
    `
})
class TestHostComponent {
    pageState: DotPageRenderState = _.cloneDeep(pageRenderStateMock);
    variant: DotVariantData;
}

describe('DotEditPageStateControllerSeoComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let component: DotEditPageStateControllerSeoComponent;
    let de: DebugElement;
    let deHost: DebugElement;
    let dotPageStateService: DotPageStateService;
    let dialogService: DotAlertConfirmService;
    let personalizeService: DotPersonalizeService;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: mockDotMessageService
                },
                {
                    provide: DotPageStateService,
                    useClass: DotPageStateServiceMock
                },
                {
                    provide: DotPersonalizeService,
                    useClass: DotPersonalizeServiceMock
                },
                DotAlertConfirmService,
                DecimalPipe,
                { provide: LOCALE_ID, useValue: 'en-US' },
                ConfirmationService,
                {
                    provide: CoreWebService,
                    useClass: CoreWebServiceMock
                },
                {
                    provide: DotDevicesService,
                    useClass: DotDevicesServiceMock
                },
                DotCurrentUserService
            ],
            imports: [
                InputSwitchModule,
                SelectButtonModule,
                TooltipModule,
                DotPipesModule,
                DotEditPageStateControllerSeoComponent,
                DotEditPageLockInfoSeoComponent,
                DotDeviceSelectorSeoComponent,
                RouterTestingModule,
                CommonModule,
                FormsModule,
                HttpClientTestingModule,
                OverlayPanelModule,
                BrowserAnimationsModule
            ]
        });
    }));

    beforeEach(() => {
        fixtureHost = TestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;
        de = deHost.query(By.css('dot-edit-page-state-controller-seo'));
        component = de.componentInstance;
        dotPageStateService = de.injector.get(DotPageStateService);
        dialogService = de.injector.get(DotAlertConfirmService);
        personalizeService = de.injector.get(DotPersonalizeService);

        spyOn(component.modeChange, 'emit');
        spyOn(dotPageStateService, 'setLock');
        spyOn(personalizeService, 'personalized').and.returnValue(of(null));
    });

    describe('elements', () => {
        describe('default', () => {
            it('should have mode selector', async () => {
                componentHost.variant = null;
                fixtureHost.detectChanges();
                const dotTabButtons = de.query(
                    By.css('[data-testId="dot-tabs-buttons"]')
                ).componentInstance;
                await fixtureHost.whenRenderingDone();
                expect(dotTabButtons).toBeDefined();
                expect(dotTabButtons.options).toEqual([
                    {
                        label: 'Edit',
                        value: 'EDIT_MODE',
                        disabled: false
                    },
                    {
                        label: 'Preview',
                        value: 'PREVIEW_MODE',
                        disabled: false
                    }
                ]);
            });

            it('should have locker with right attributes', async () => {
                const pageRenderStateMocked: DotPageRenderState = new DotPageRenderState(
                    { ...mockUser(), userId: '456' },
                    new DotPageRender(mockDotRenderedPage())
                );
                fixtureHost.componentInstance.pageState = _.cloneDeep(pageRenderStateMocked);
                componentHost.variant = null;
                fixtureHost.detectChanges();
                const lockerDe = de.query(By.css('p-inputSwitch'));
                const lockerContainerDe = de.query(By.css('[data-testId="lock-container"]'));
                const locker = lockerDe.componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(lockerDe.classes.warn).toBe(true, 'warn class');
                expect(lockerDe.attributes.appendTo).toBe('target');
                expect(lockerContainerDe.attributes['ng-reflect-text']).toBe(
                    'Page locked by Some One'
                );
                expect(lockerContainerDe.attributes['ng-reflect-tooltip-position']).toBe('bottom');
                expect(locker.modelValue).toBe(false, 'checked');
                expect(locker.disabled).toBe(false, 'disabled');
            });

            it('should have lock info', () => {
                fixtureHost.detectChanges();
                const message = de.query(By.css('[data-testId="lockInfo"]')).componentInstance;
                expect(message.pageState).toEqual(pageRenderStateMock);
            });
        });

        describe('disable mode selector option', () => {
            it('should disable preview', async () => {
                componentHost.pageState.page.canRead = false;
                componentHost.variant = null;
                fixtureHost.detectChanges();
                const dotTabButtons = de.query(
                    By.css('[data-testId="dot-tabs-buttons"]')
                ).componentInstance;

                fixtureHost.whenRenderingDone();

                await expect(dotTabButtons).toBeDefined();
                expect(dotTabButtons.options[1]).toEqual({
                    label: 'Preview',
                    value: 'PREVIEW_MODE',
                    disabled: true
                });
                expect(dotTabButtons.mode).toBe(DotPageMode.PREVIEW);
            });

            it('should disable edit', async () => {
                componentHost.pageState.page.canEdit = false;
                componentHost.pageState.page.canLock = false;
                componentHost.variant = null;
                fixtureHost.detectChanges();
                const dotTabButtons = de.query(
                    By.css('[data-testId="dot-tabs-buttons"]')
                ).componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(dotTabButtons).toBeDefined();
                expect(dotTabButtons.options[0]).toEqual({
                    label: 'Edit',
                    value: 'EDIT_MODE',
                    disabled: true
                });
                expect(dotTabButtons.mode).toBe(DotPageMode.PREVIEW);
            });

            it('should enable edit and preview when variant id different than original and draft', async () => {
                fixtureHost.detectChanges();
                componentHost.variant = dotVariantDataMock;
                const dotTabButtons = de.query(
                    By.css('[data-testId="dot-tabs-buttons"]')
                ).componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(dotTabButtons).toBeDefined();

                const editOption = dotTabButtons.options[0];
                const previewOption = dotTabButtons.options[1];

                expect(editOption.disabled).toEqual(false);
                expect(previewOption.disabled).toEqual(false);

                expect(dotTabButtons.mode).toBe(DotPageMode.PREVIEW);
            });

            it('should show only the preview tab when experiment is not Draft', async () => {
                componentHost.variant = {
                    ...dotVariantDataMock,
                    experimentStatus: DotExperimentStatus.RUNNING
                };
                fixtureHost.detectChanges();

                const dotTabButtons = de.query(
                    By.css('[data-testId="dot-tabs-buttons"]')
                ).componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(dotTabButtons).toBeDefined();

                const previewOption = dotTabButtons.options[0];

                expect(dotTabButtons.options.length).toEqual(1);
                expect(previewOption.disabled).toEqual(false);

                expect(dotTabButtons.mode).toBe(DotPageMode.PREVIEW);
            });

            it('should show only the preview tab when variant is the default one', async () => {
                componentHost.variant = {
                    ...dotVariantDataMock,
                    variant: { ...dotVariantDataMock.variant, isOriginal: true }
                };
                fixtureHost.detectChanges();

                const dotTabButtons = de.query(
                    By.css('[data-testId="dot-tabs-buttons"]')
                ).componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(dotTabButtons).toBeDefined();

                const previewOption = dotTabButtons.options[0];

                expect(dotTabButtons.options.length).toEqual(1);
                expect(previewOption.disabled).toEqual(false);

                expect(dotTabButtons.mode).toBe(DotPageMode.PREVIEW);
            });
        });
    });

    describe('events', () => {
        it('should without confirmation dialog emit modeChange and update pageState service', async () => {
            fixtureHost.detectChanges();

            const dotTabButtons = de.query(By.css('[data-testId="dot-tab-container"]'));
            dotTabButtons.triggerEventHandler('click', {
                target: { value: 'EDIT_MODE' },
                value: DotPageMode.EDIT
            });

            await fixtureHost.whenStable();

            expect(dotTabButtons).toBeTruthy();

            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(dotPageStateService.setLock).toHaveBeenCalledWith(
                { mode: DotPageMode.EDIT },
                true
            );
        });
    });

    describe('should emit modeChange when ask to LOCK confirmation', () => {
        beforeEach(() => {
            const pageRenderStateMocked: DotPageRenderState = new DotPageRenderState(
                { ...mockUser(), userId: '456' },
                new DotPageRender(mockDotRenderedPage())
            );

            fixtureHost.componentInstance.pageState = _.cloneDeep(pageRenderStateMocked);
        });

        it('should update pageState service when confirmation dialog Success', async () => {
            spyOn(dialogService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });

            fixtureHost.detectChanges();

            const dotTabButtons = de.query(By.css('[data-testId="dot-tab-container"]'));
            dotTabButtons.triggerEventHandler('click', {
                target: { value: 'EDIT_MODE' },
                value: DotPageMode.EDIT
            });
            await fixtureHost.whenStable();

            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(dialogService.confirm).toHaveBeenCalledTimes(1);
            expect(personalizeService.personalized).not.toHaveBeenCalled();
            expect(dotPageStateService.setLock).toHaveBeenCalledWith(
                { mode: DotPageMode.EDIT },
                true
            );
        });

        it('should update LOCK and MODE when confirmation dialog Canceled', () => {
            spyOn<any>(dialogService, 'confirm').and.callFake((conf) => {
                conf.cancel();
            });

            fixtureHost.detectChanges();

            const dotTabButtons = de.query(By.css('[data-testId="dot-tab-container"]'));
            dotTabButtons.triggerEventHandler('click', {
                target: { value: 'EDIT_MODE' },
                value: DotPageMode.EDIT
            });

            fixtureHost.whenStable();

            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(dialogService.confirm).toHaveBeenCalledTimes(1);
            expect(component.lock).toBe(true);
            expect(component.mode).toBe(DotPageMode.PREVIEW);
        });
    });

    describe('should emit modeChange when ask to PERSONALIZE confirmation', () => {
        it('should update pageState service when confirmation dialog Success', async () => {
            const pageRenderStateMocked: DotPageRenderState = new DotPageRenderState(
                mockUser(),
                new DotPageRender({
                    ...mockDotRenderedPage(),
                    viewAs: {
                        ...mockDotRenderedPage().viewAs,
                        persona: {
                            ...dotcmsContentletMock,
                            name: 'John',
                            personalized: false,
                            keyTag: 'Other'
                        }
                    }
                })
            );

            fixtureHost.componentInstance.pageState = _.cloneDeep(pageRenderStateMocked);
            spyOn(dialogService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });

            fixtureHost.detectChanges();

            const dotTabButtons = de.query(By.css('[data-testId="dot-tab-container"]'));
            dotTabButtons.triggerEventHandler('click', {
                target: { value: 'EDIT_MODE' },
                value: DotPageMode.EDIT
            });

            await fixtureHost.whenStable();
            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(dialogService.confirm).toHaveBeenCalledTimes(1);
            expect(personalizeService.personalized).toHaveBeenCalledWith(
                mockDotRenderedPage().page.identifier,
                pageRenderStateMocked.viewAs.persona.keyTag
            );
            expect(dotPageStateService.setLock).toHaveBeenCalledWith(
                { mode: DotPageMode.EDIT },
                true
            );
        });
    });

    describe('running experiment confirmation', () => {
        beforeEach(() => {
            const pageRenderStateMocked: DotPageRenderState = new DotPageRenderState(
                mockUser(),
                new DotPageRender(mockDotRenderedPage()),
                null,
                EXPERIMENT_MOCK
            );

            fixtureHost.componentInstance.pageState = _.cloneDeep(pageRenderStateMocked);
        });

        it('should update pageState service when confirmation dialog Success', async () => {
            spyOn(dialogService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });
            fixtureHost.detectChanges();

            const dotTabButtons = de.query(By.css('[data-testId="dot-tab-container"]'));
            dotTabButtons.triggerEventHandler('click', {
                target: { value: 'EDIT_MODE' },
                value: DotPageMode.EDIT
            });

            await fixtureHost.whenStable();
            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(dialogService.confirm).toHaveBeenCalledTimes(1);

            expect(dotPageStateService.setLock).toHaveBeenCalledWith(
                { mode: DotPageMode.EDIT },
                true
            );
        });
    });

    describe('Dot Device Selector events', () => {
        it('should call  changeSeoMedia event', async () => {
            fixtureHost.detectChanges();
            spyOn(dotPageStateService, 'setSeoMedia');
            const dotSelector = de.query(By.css('[data-testId="dot-device-selector"]'));

            dotSelector.triggerEventHandler('changeSeoMedia', 'Google');

            expect(dotPageStateService.setSeoMedia).toHaveBeenCalledWith('Google');
        });

        it('should call selected event', async () => {
            spyOn(dotPageStateService, 'setDevice');
            const dotSelector = de.query(By.css('[data-testId="dot-device-selector"]'));
            const event = {
                identifier: 'string',
                cssHeight: 'string',
                cssWidth: 'string',
                name: 'string',
                inode: 'string',
                stInode: 'string'
            };
            dotSelector.triggerEventHandler('selected', event);

            expect(dotPageStateService.setDevice).toHaveBeenCalledWith(event);
        });
    });
});
