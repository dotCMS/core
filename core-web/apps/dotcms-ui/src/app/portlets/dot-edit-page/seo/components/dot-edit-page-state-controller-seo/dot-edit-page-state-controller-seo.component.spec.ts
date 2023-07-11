/* eslint-disable @typescript-eslint/no-explicit-any */

import * as _ from 'lodash';
import { of } from 'rxjs';

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { InputSwitchModule } from 'primeng/inputswitch';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';

import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import {
    DotAlertConfirmService,
    DotMessageService,
    DotPersonalizeService
} from '@dotcms/data-access';
import {
    DEFAULT_VARIANT_NAME,
    DotExperimentStatus,
    DotPageMode,
    DotPageRender,
    DotPageRenderState,
    DotVariantData
} from '@dotcms/dotcms-models';
import {
    dotcmsContentletMock,
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

const mockDotMessageService = new MockDotMessageService({
    'editpage.toolbar.edit.page': 'Edit',
    'editpage.toolbar.preview.page': 'Preview',
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
        <dot-edit-page-state-controller-seo
            [pageState]="pageState"
            [variant]="variant"
        ></dot-edit-page-state-controller-seo>
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
        DOTTestBed.configureTestingModule({
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
                DotAlertConfirmService
            ],
            imports: [
                InputSwitchModule,
                SelectButtonModule,
                TooltipModule,
                DotPipesModule,
                DotEditPageStateControllerSeoComponent,
                DotEditPageLockInfoSeoComponent
            ]
        });
    }));

    beforeEach(() => {
        fixtureHost = DOTTestBed.createComponent(TestHostComponent);
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
                const selectButton = de.query(
                    By.css('[data-testId="selectButton"]')
                ).componentInstance;
                await fixtureHost.whenRenderingDone();

                expect(selectButton).toBeDefined();
                expect(selectButton.options).toEqual([
                    { label: 'Edit', value: 'EDIT_MODE', disabled: false },
                    { label: 'Preview', value: 'PREVIEW_MODE', disabled: false }
                ]);
                expect(selectButton.value).toBe(DotPageMode.PREVIEW);
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
                const locker = lockerDe.componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(lockerDe.classes.warn).toBe(true, 'warn class');
                expect(lockerDe.attributes.appendTo).toBe('target');
                expect(lockerDe.attributes['ng-reflect-text']).toBe('Page locked by Some One');
                expect(lockerDe.attributes['ng-reflect-tooltip-position']).toBe('top');
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
                const selectButton = de.query(
                    By.css('[data-testId="selectButton"]')
                ).componentInstance;

                fixtureHost.whenRenderingDone();

                await expect(selectButton).toBeDefined();
                expect(selectButton.options[1]).toEqual({
                    label: 'Preview',
                    value: 'PREVIEW_MODE',
                    disabled: true
                });
                expect(selectButton.value).toBe(DotPageMode.PREVIEW);
            });

            it('should disable edit', async () => {
                componentHost.pageState.page.canEdit = false;
                componentHost.pageState.page.canLock = false;
                componentHost.variant = null;
                fixtureHost.detectChanges();
                const selectButton = de.query(
                    By.css('[data-testId="selectButton"]')
                ).componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(selectButton).toBeDefined();
                expect(selectButton.options[0]).toEqual({
                    label: 'Edit',
                    value: 'EDIT_MODE',
                    disabled: true
                });
                expect(selectButton.value).toBe(DotPageMode.PREVIEW);
            });

            it('should enable edit and preview when variant id different than original and draft', async () => {
                fixtureHost.detectChanges();
                componentHost.variant = dotVariantDataMock;
                const selectButton = de.query(
                    By.css('[data-testId="selectButton"]')
                ).componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(selectButton).toBeDefined();

                const editOption = selectButton.options[0];
                const previewOption = selectButton.options[1];

                expect(editOption.disabled).toEqual(false);
                expect(previewOption.disabled).toEqual(false);

                expect(selectButton.value).toBe(DotPageMode.PREVIEW);
            });

            it('should show only the preview tab when experiment is not Draft', async () => {
                componentHost.variant = {
                    ...dotVariantDataMock,
                    experimentStatus: DotExperimentStatus.RUNNING
                };
                fixtureHost.detectChanges();

                const selectButton = de.query(
                    By.css('[data-testId="selectButton"]')
                ).componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(selectButton).toBeDefined();

                const previewOption = selectButton.options[0];

                expect(selectButton.options.length).toEqual(1);
                expect(previewOption.disabled).toEqual(false);

                expect(selectButton.value).toBe(DotPageMode.PREVIEW);
            });

            it('should show only the preview tab when variant is the default one', async () => {
                componentHost.variant = {
                    ...dotVariantDataMock,
                    variant: { ...dotVariantDataMock.variant, isOriginal: true }
                };
                fixtureHost.detectChanges();

                const selectButton = de.query(
                    By.css('[data-testId="selectButton"]')
                ).componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(selectButton).toBeDefined();

                const previewOption = selectButton.options[0];

                expect(selectButton.options.length).toEqual(1);
                expect(previewOption.disabled).toEqual(false);

                expect(selectButton.value).toBe(DotPageMode.PREVIEW);
            });
        });
    });

    describe('events', () => {
        it('should without confirmation dialog emit modeChange and update pageState service', async () => {
            fixtureHost.detectChanges();

            const selectButton = de.query(By.css('p-selectButton'));
            selectButton.triggerEventHandler('onChange', {
                value: DotPageMode.EDIT
            });

            await fixtureHost.whenStable();
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

            const selectButton = de.query(By.css('p-selectButton'));
            selectButton.triggerEventHandler('onChange', {
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

            const selectButton = de.query(By.css('p-selectButton'));
            selectButton.triggerEventHandler('onChange', {
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

            const selectButton = de.query(By.css('p-selectButton'));
            selectButton.triggerEventHandler('onChange', {
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
});
