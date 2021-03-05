import { By } from '@angular/platform-browser';
import { Component, DebugElement } from '@angular/core';
import { waitForAsync, ComponentFixture } from '@angular/core/testing';

import { DOTTestBed } from '@tests/dot-test-bed';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotPageStateServiceMock } from '@tests/dot-page-state.service.mock';
import { DotPersonalizeServiceMock } from '@tests/dot-personalize-service.mock';

import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotEditPageLockInfoComponent } from './components/dot-edit-page-lock-info/dot-edit-page-lock-info.component';
import { DotEditPageStateControllerComponent } from './dot-edit-page-state-controller.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotPageStateService } from '../../services/dot-page-state/dot-page-state.service';
import { DotPersonalizeService } from '@services/dot-personalize/dot-personalize.service';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models';
import * as _ from 'lodash';
import { mockUser } from '@tests/login-service.mock';
import { mockDotRenderedPage } from '@tests/dot-page-render.mock';
import { dotcmsContentletMock } from '@tests/dotcms-contentlet.mock';
import { of } from 'rxjs';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { InputSwitchModule } from 'primeng/inputswitch';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';
import { DotPageRender } from '@models/dot-page/dot-rendered-page.model';
import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';

const mockDotMessageService = new MockDotMessageService({
    'editpage.toolbar.edit.page': 'Edit',
    'editpage.toolbar.live.page': 'Live',
    'editpage.toolbar.preview.page': 'Preview',
    'editpage.content.steal.lock.confirmation.message.header': 'Lock',
    'editpage.content.steal.lock.confirmation.message': 'Steal lock',
    'editpage.personalization.confirm.message': 'Are you sure?',
    'editpage.personalization.confirm.header': 'Personalization',
    'editpage.personalization.confirm.with.lock': 'Also steal lock',
    'editpage.toolbar.page.locked.by.user': 'Page locked by {0}'
});

const pageRenderStateMock: DotPageRenderState = new DotPageRenderState(
    mockUser(),
    new DotPageRender(mockDotRenderedPage())
);

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-edit-page-state-controller [pageState]="pageState"></dot-edit-page-state-controller>
    `
})
class TestHostComponent {
    pageState: DotPageRenderState = _.cloneDeep(pageRenderStateMock);
}

describe('DotEditPageStateControllerComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let component: DotEditPageStateControllerComponent;
    let de: DebugElement;
    let deHost: DebugElement;
    let dotPageStateService: DotPageStateService;
    let dialogService: DotAlertConfirmService;
    let personalizeService: DotPersonalizeService;

    beforeEach(
        waitForAsync(() => {
            DOTTestBed.configureTestingModule({
                declarations: [
                    TestHostComponent,
                    DotEditPageStateControllerComponent,
                    DotEditPageLockInfoComponent
                ],
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
                imports: [InputSwitchModule, SelectButtonModule, TooltipModule, DotPipesModule]
            });
        })
    );

    beforeEach(() => {
        fixtureHost = DOTTestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;
        de = deHost.query(By.css('dot-edit-page-state-controller'));
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
                fixtureHost.detectChanges();
                const selectButton = de.query(By.css('p-selectButton')).componentInstance;
                await fixtureHost.whenRenderingDone();

                expect(selectButton).toBeDefined();
                expect(selectButton.options).toEqual([
                    { label: 'Edit', value: 'EDIT_MODE', disabled: false },
                    { label: 'Preview', value: 'PREVIEW_MODE', disabled: false },
                    { label: 'Live', value: 'ADMIN_MODE', disabled: false }
                ]);
                expect(selectButton.value).toBe(DotPageMode.PREVIEW);
            });

            it('should have locker with right attributes', async () => {
                const pageRenderStateMocked: DotPageRenderState = new DotPageRenderState(
                    { ...mockUser(), userId: '456' },
                    new DotPageRender(mockDotRenderedPage())
                );
                fixtureHost.componentInstance.pageState = _.cloneDeep(pageRenderStateMocked);
                fixtureHost.detectChanges();
                const lockerDe = de.query(By.css('p-inputSwitch'));
                const locker = lockerDe.componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(lockerDe.classes.warn).toBe(true, 'warn class');
                expect(lockerDe.attributes.appendTo).toBe('target');
                expect(lockerDe.attributes['ng-reflect-text']).toBe('Page locked by Some One');
                expect(lockerDe.attributes['ng-reflect-tooltip-position']).toBe('top');
                expect(locker.checked).toBe(false, 'checked');
                expect(locker.disabled).toBe(false, 'disabled');
            });

            it('should have lock info', () => {
                fixtureHost.detectChanges();
                const message = de.query(By.css('dot-edit-page-lock-info')).componentInstance;
                expect(message.pageState).toEqual(pageRenderStateMock);
            });
        });

        describe('disable mode selector option', () => {
            it('should disable preview', async () => {
                componentHost.pageState.page.canRead = false;
                fixtureHost.detectChanges();
                const selectButton = de.query(By.css('p-selectButton')).componentInstance;

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
                fixtureHost.detectChanges();
                const selectButton = de.query(By.css('p-selectButton')).componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(selectButton).toBeDefined();
                expect(selectButton.options[0]).toEqual({
                    label: 'Edit',
                    value: 'EDIT_MODE',
                    disabled: true
                });
                expect(selectButton.value).toBe(DotPageMode.PREVIEW);
            });

            it('should disable live', async () => {
                componentHost.pageState.page.liveInode = null;
                fixtureHost.detectChanges();
                const selectButton = de.query(By.css('p-selectButton')).componentInstance;

                await fixtureHost.whenRenderingDone();

                expect(selectButton).toBeDefined();
                expect(selectButton.options[2]).toEqual({
                    label: 'Live',
                    value: 'ADMIN_MODE',
                    disabled: true
                });
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

        it('should update LOCK and MODE when confirmation dialog Canceled', async () => {
            spyOn<any>(dialogService, 'confirm').and.callFake((conf) => {
                conf.cancel();
            });

            fixtureHost.detectChanges();

            const selectButton = de.query(By.css('p-selectButton'));
            selectButton.triggerEventHandler('onChange', {
                value: DotPageMode.EDIT
            });

            await fixtureHost.whenStable();

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
