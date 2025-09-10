/* eslint-disable @typescript-eslint/no-explicit-any */

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
    DotHttpErrorManagerService,
    DotMessageService,
    DotPageStateService,
    DotPersonalizeService,
    DotPropertiesService
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
import { DotDeviceSelectorSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotSafeHtmlPipe, DotTabButtonsComponent } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    createFakeEvent,
    dotcmsContentletMock,
    DotDevicesServiceMock,
    DotPageStateServiceMock,
    DotPersonalizeServiceMock,
    getExperimentMock,
    MockDotHttpErrorManagerService,
    MockDotMessageService,
    mockDotRenderedPage,
    mockUser
} from '@dotcms/utils-testing';

import { DotEditPageLockInfoSeoComponent } from './components/dot-edit-page-lock-info-seo/dot-edit-page-lock-info-seo.component';
import { DotEditPageStateControllerSeoComponent } from './dot-edit-page-state-controller-seo.component';

import { DotContentletEditorService } from '../../../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';

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

export const dotVariantDataMock: DotVariantData = {
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

const getPageRenderStateMock = () =>
    new DotPageRenderState(mockUser(), new DotPageRender(mockDotRenderedPage()));

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-edit-page-state-controller-seo
            [pageState]="pageState"
            [variant]="variant"></dot-edit-page-state-controller-seo>
    `,
    standalone: false
})
class TestHostComponent {
    pageState: DotPageRenderState = getPageRenderStateMock();
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
    let propertiesService: DotPropertiesService;
    let editContentletService: DotContentletEditorService;
    let dotTabButtons: DotTabButtonsComponent;
    let deDotTabButtons: DebugElement;

    let featFlagMock: jest.SpyInstance;

    let pointerEvent: PointerEvent;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            providers: [
                DecimalPipe,
                ConfirmationService,
                DotCurrentUserService,
                DotAlertConfirmService,
                DotContentletEditorService,
                DotPropertiesService,
                {
                    provide: CoreWebService,
                    useClass: CoreWebServiceMock
                },
                {
                    provide: DotDevicesService,
                    useClass: DotDevicesServiceMock
                },
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
                {
                    provide: DotHttpErrorManagerService,
                    useClass: MockDotHttpErrorManagerService
                },
                { provide: LOCALE_ID, useValue: 'en-US' }
            ],
            imports: [
                InputSwitchModule,
                SelectButtonModule,
                TooltipModule,
                DotSafeHtmlPipe,
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
        propertiesService = de.injector.get(DotPropertiesService);
        editContentletService = de.injector.get(DotContentletEditorService);

        jest.spyOn(component.modeChange, 'emit');
        jest.spyOn(dotPageStateService, 'setLock');
        jest.spyOn(personalizeService, 'personalized').mockReturnValue(of(null));
        featFlagMock = jest.spyOn(propertiesService, 'getFeatureFlag').mockReturnValue(of(false));

        deDotTabButtons = de.query(By.css('[data-testId="dot-tabs-buttons"]'));
        dotTabButtons = deDotTabButtons.componentInstance;
    });

    describe('elements', () => {
        describe('default', () => {
            it('should have mode selector', async () => {
                componentHost.variant = null;
                fixtureHost.detectChanges();
                await fixtureHost.whenRenderingDone();
                expect(dotTabButtons).toBeDefined();
                expect(dotTabButtons.options).toEqual([
                    {
                        label: 'Edit',
                        value: {
                            id: 'EDIT_MODE',
                            showDropdownButton: false,
                            shouldRefresh: false
                        },
                        disabled: false
                    },
                    {
                        label: 'Preview',
                        value: {
                            id: 'PREVIEW_MODE',
                            showDropdownButton: true,
                            shouldRefresh: true
                        },
                        disabled: false
                    }
                ]);
            });

            it('should have locker with right attributes', async () => {
                const pageRenderStateMocked: DotPageRenderState = new DotPageRenderState(
                    { ...mockUser(), userId: '456' },
                    new DotPageRender(mockDotRenderedPage())
                );
                fixtureHost.componentInstance.pageState = pageRenderStateMocked;
                componentHost.variant = null;
                fixtureHost.detectChanges();
                const lockerDe = de.query(By.css('p-inputSwitch'));
                const lockerContainerDe = de.query(By.css('[data-testId="lock-container"]'));
                const locker = lockerDe.componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(lockerDe.classes.warn).toBe(true, 'warn class');
                expect(lockerDe.attributes.appendTo).toBe('target');
                expect(lockerContainerDe.attributes['ng-reflect-content']).toBe(
                    'Page locked by Some One'
                );
                expect(lockerContainerDe.attributes['ng-reflect-tooltip-position']).toBe('bottom');
                expect(locker.modelValue).toBe(true, 'checked');
                expect(locker.disabled).toBe(false, 'disabled');
            });

            it('should have the lock switch in the "on" state', async () => {
                const pageRenderStateMocked: DotPageRenderState = new DotPageRenderState(
                    { ...mockUser(), userId: '456' },
                    new DotPageRender(mockDotRenderedPage())
                );
                fixtureHost.componentInstance.pageState = pageRenderStateMocked;
                componentHost.variant = null;
                componentHost.pageState.page.locked = true;
                fixtureHost.detectChanges();
                const lockerDe = de.query(By.css('[data-testId="lock-switch"]'));

                const locker = lockerDe.componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(locker.modelValue).toBeTruthy();
            });

            it('should have the lock switch in the "off" state', async () => {
                const pageRenderStateMocked: DotPageRenderState = new DotPageRenderState(
                    { ...mockUser(), userId: '456' },
                    new DotPageRender(mockDotRenderedPage())
                );
                fixtureHost.componentInstance.pageState = pageRenderStateMocked;
                componentHost.variant = null;
                componentHost.pageState.state.locked = false;
                fixtureHost.detectChanges();
                const lockerDe = de.query(By.css('[data-testId="lock-switch"]'));

                const locker = lockerDe.componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(locker.modelValue).toBeFalsy();
            });

            it('should have lock info', () => {
                fixtureHost.detectChanges();
                const message = de.query(By.css('[data-testId="lockInfo"]')).componentInstance;
                expect(message.pageState).toEqual(getPageRenderStateMock());
            });
        });

        describe('disable mode selector option', () => {
            it('should disable preview', async () => {
                componentHost.pageState.page.canRead = false;
                componentHost.variant = null;
                fixtureHost.detectChanges();

                fixtureHost.whenRenderingDone();

                await expect(dotTabButtons).toBeDefined();
                expect(dotTabButtons.options[1]).toEqual({
                    label: 'Preview',
                    value: {
                        id: 'PREVIEW_MODE',
                        showDropdownButton: true,
                        shouldRefresh: true
                    },
                    disabled: true
                });
                expect(dotTabButtons.activeId).toBe(DotPageMode.PREVIEW);
            });

            it('should disable edit', async () => {
                componentHost.pageState.page.canEdit = false;
                componentHost.pageState.page.canLock = false;
                componentHost.variant = null;
                fixtureHost.detectChanges();

                await fixtureHost.whenRenderingDone();

                expect(dotTabButtons).toBeDefined();
                expect(dotTabButtons.options[0]).toEqual({
                    label: 'Edit',
                    value: {
                        id: 'EDIT_MODE',
                        showDropdownButton: false,
                        shouldRefresh: false
                    },
                    disabled: true
                });
                expect(dotTabButtons.activeId).toBe(DotPageMode.PREVIEW);
            });

            it('should enable edit and preview when variant id different than original and draft', async () => {
                fixtureHost.detectChanges();
                componentHost.variant = dotVariantDataMock;

                await fixtureHost.whenRenderingDone();

                expect(dotTabButtons).toBeDefined();

                const editOption = dotTabButtons.options[0];
                const previewOption = dotTabButtons.options[1];

                expect(editOption.disabled).toEqual(false);
                expect(previewOption.disabled).toEqual(false);

                expect(dotTabButtons.activeId).toBe(DotPageMode.PREVIEW);
            });

            it('should show only the preview tab when experiment is not Draft', async () => {
                componentHost.variant = {
                    ...dotVariantDataMock,
                    experimentStatus: DotExperimentStatus.RUNNING
                };
                fixtureHost.detectChanges();

                await fixtureHost.whenRenderingDone();

                expect(dotTabButtons).toBeDefined();

                const previewOption = dotTabButtons.options[0];

                expect(dotTabButtons.options.length).toEqual(1);
                expect(previewOption.disabled).toEqual(false);

                expect(dotTabButtons.activeId).toBe(DotPageMode.PREVIEW);
            });

            it('should show only the preview tab when variant is the default one', async () => {
                componentHost.variant = {
                    ...dotVariantDataMock,
                    variant: { ...dotVariantDataMock.variant, isOriginal: true }
                };
                fixtureHost.detectChanges();

                await fixtureHost.whenRenderingDone();

                expect(dotTabButtons).toBeDefined();

                const previewOption = dotTabButtons.options[0];

                expect(dotTabButtons.options.length).toEqual(1);
                expect(previewOption.disabled).toEqual(false);

                expect(dotTabButtons.activeId).toBe(DotPageMode.PREVIEW);
            });
            it('should show only the preview tab when the page is blocked by another user', async () => {
                componentHost.variant = {
                    ...dotVariantDataMock
                };
                componentHost.pageState.state.lockedByAnotherUser = true;
                fixtureHost.detectChanges();

                await fixtureHost.whenRenderingDone();

                const previewOption = dotTabButtons.options[0];

                expect(dotTabButtons.options.length).toEqual(1);
                expect(previewOption.disabled).toEqual(false);

                expect(dotTabButtons.activeId).toBe(DotPageMode.PREVIEW);
            });
        });
    });

    describe('events', () => {
        it('should without confirmation dialog emit modeChange and update pageState service', async () => {
            fixtureHost.detectChanges();

            deDotTabButtons.triggerEventHandler('clickOption', {
                event: pointerEvent,
                optionId: DotPageMode.EDIT
            });

            await fixtureHost.whenStable();

            expect(dotTabButtons).toBeTruthy();

            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(component.modeChange.emit).toHaveBeenCalledTimes(1);
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

            fixtureHost.componentInstance.pageState = pageRenderStateMocked;
        });

        it('should update pageState service when confirmation dialog Success', async () => {
            jest.spyOn(dialogService, 'confirm').mockImplementation((conf) => {
                conf.accept();
            });

            fixtureHost.detectChanges();

            deDotTabButtons.triggerEventHandler('clickOption', {
                event: pointerEvent,
                optionId: DotPageMode.EDIT
            });

            await fixtureHost.whenStable();

            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(component.modeChange.emit).toHaveBeenCalledTimes(1);
            expect(dialogService.confirm).toHaveBeenCalledTimes(1);
            expect(personalizeService.personalized).not.toHaveBeenCalled();
            expect(dotPageStateService.setLock).toHaveBeenCalledWith(
                { mode: DotPageMode.EDIT },
                true
            );
        });

        it('should update LOCK and MODE when confirmation dialog Canceled', () => {
            jest.spyOn<any>(dialogService, 'confirm').mockImplementation((conf) => {
                conf.cancel();
            });

            fixtureHost.detectChanges();

            deDotTabButtons.triggerEventHandler('clickOption', {
                event: pointerEvent,
                optionId: DotPageMode.EDIT
            });

            fixtureHost.whenStable();

            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(component.modeChange.emit).toHaveBeenCalledTimes(1);
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

            fixtureHost.componentInstance.pageState = pageRenderStateMocked;
            jest.spyOn(dialogService, 'confirm').mockImplementation((conf) => {
                conf.accept();
            });

            fixtureHost.detectChanges();

            deDotTabButtons.triggerEventHandler('clickOption', {
                event: pointerEvent,
                optionId: DotPageMode.EDIT
            });

            await fixtureHost.whenStable();
            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(component.modeChange.emit).toHaveBeenCalledTimes(1);
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

            fixtureHost.componentInstance.pageState = pageRenderStateMocked;
        });

        it('should update pageState service when confirmation dialog Success', async () => {
            jest.spyOn(dialogService, 'confirm').mockImplementation((conf) => {
                conf.accept();
            });
            fixtureHost.detectChanges();

            deDotTabButtons.triggerEventHandler('clickOption', {
                event: pointerEvent,
                optionId: DotPageMode.EDIT
            });

            await fixtureHost.whenStable();
            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(component.modeChange.emit).toHaveBeenCalledTimes(1);
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
            jest.spyOn(dotPageStateService, 'setSeoMedia');
            const dotSelector = de.query(By.css('[data-testId="dot-device-selector"]'));

            dotSelector.triggerEventHandler('changeSeoMedia', 'Google');

            expect(dotPageStateService.setSeoMedia).toHaveBeenCalledWith('Google');
            expect(dotPageStateService.setSeoMedia).toHaveBeenCalledTimes(1);
        });

        it('should call selected event', async () => {
            jest.spyOn(dotPageStateService, 'setDevice');
            jest.spyOn(dotPageStateService, 'setSeoMedia');
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
            expect(dotPageStateService.setDevice).toHaveBeenCalledTimes(1);
            expect(dotPageStateService.setSeoMedia).toHaveBeenCalledWith(null);
            expect(dotPageStateService.setSeoMedia).toHaveBeenCalledTimes(1);
        });
    });
    describe('page does not have URLContentMap and feature flag is on', () => {
        beforeEach(() => {
            featFlagMock.mockReturnValue(of(true));

            const pageRenderStateMocked: DotPageRenderState = new DotPageRenderState(
                { ...mockUser(), userId: '486' },
                mockDotRenderedPage()
            );
            fixtureHost.componentInstance.pageState = pageRenderStateMocked;

            fixtureHost.detectChanges();
        });

        it('should not have menuItems if page does not have URLContentMap', async () => {
            expect(component.menuItems.length).toBe(0);
        });
    });

    describe('feature flag edit URLContentMap is on', () => {
        beforeEach(() => {
            featFlagMock.mockReturnValue(of('true'));

            const pageRenderStateMocked: DotPageRenderState = new DotPageRenderState(
                { ...mockUser(), userId: '457' },
                {
                    ...mockDotRenderedPage(),
                    urlContentMap: {
                        title: 'Title',
                        inode: '123',
                        contentType: 'test'
                    }
                }
            );
            fixtureHost.componentInstance.pageState = pageRenderStateMocked;
            fixtureHost.detectChanges();
        });

        it('should have menuItems if page has URLContentMap', async () => {
            await fixtureHost.whenStable();
            expect(component.menuItems.length).toBe(2);
        });

        it('should have preview and edit options with showDropdownButton setted to true', () => {
            expect(component.options[0].value.showDropdownButton).toBe(true);
            expect(component.options[1].value.showDropdownButton).toBe(true);
        });

        it("should change the mode when the user clicks on the 'Edit' option", () => {
            component.menuItems[0].command({
                originalEvent: createFakeEvent('click')
            });

            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(component.modeChange.emit).toHaveBeenCalledTimes(1);
        });

        it("should call editContentlet when clicking on the 'ContentType Content' option", () => {
            jest.spyOn(editContentletService, 'edit');
            component.menuItems[1].command({
                originalEvent: createFakeEvent('click')
            });
            expect(editContentletService.edit).toHaveBeenCalledWith({
                data: {
                    inode: '123'
                }
            });
        });

        it('should trigger resetDropdownById when menu hides', () => {
            jest.spyOn(dotTabButtons, 'resetDropdownById');

            component.menu.onHide.emit();

            expect(dotTabButtons.resetDropdownById).toHaveBeenCalledWith(DotPageMode.EDIT);
            expect(dotTabButtons.resetDropdownById).toHaveBeenCalledTimes(1);
        });

        it('should trigger resetDropdownById when device selector hides', () => {
            jest.spyOn(dotTabButtons, 'resetDropdownById');

            component.deviceSelector.hideOverlayPanel.emit();

            expect(dotTabButtons.resetDropdownById).toHaveBeenCalledWith(DotPageMode.PREVIEW);
            expect(dotTabButtons.resetDropdownById).toHaveBeenCalledTimes(1);
        });

        it('should have menuItems if the page goes from not having urlContentMap to having it', async () => {
            let pageRenderStateMocked: DotPageRenderState = new DotPageRenderState(
                { ...mockUser(), userId: '457' },
                {
                    ...mockDotRenderedPage()
                }
            );

            fixtureHost.componentInstance.pageState = pageRenderStateMocked;
            fixtureHost.detectChanges();

            await fixtureHost.whenStable();
            expect(component.menuItems.length).toBe(0);

            pageRenderStateMocked = new DotPageRenderState(
                { ...mockUser(), userId: '457' },
                {
                    ...mockDotRenderedPage(),
                    urlContentMap: {
                        title: 'Title',
                        inode: '123',
                        contentType: 'test'
                    }
                }
            );

            fixtureHost.componentInstance.pageState = pageRenderStateMocked;
            fixtureHost.detectChanges();

            await fixtureHost.whenStable();
            expect(component.menuItems.length).toBe(2);
        });
    });
});
