import { By } from '@angular/platform-browser';
import { Component, DebugElement } from '@angular/core';
import { async, ComponentFixture } from '@angular/core/testing';

import { DOTTestBed } from '@tests/dot-test-bed';
import { mockDotRenderedPageState } from '@tests/dot-rendered-page-state.mock';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotPageStateServiceMock } from '@tests/dot-page-state.service.mock';
import { DotPersonalizeServiceMock } from '@tests/dot-personalize-service.mock';

import { InputSwitchModule, SelectButtonModule } from 'primeng/primeng';

import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotEditPageLockInfoComponent } from './components/dot-edit-page-lock-info/dot-edit-page-lock-info.component';
import { DotEditPageStateControllerComponent } from './dot-edit-page-state-controller.component';
import { DotMessageService } from '@services/dot-messages-service';
import { DotPageStateService } from '../../services/dot-page-state/dot-page-state.service';
import { DotPersonalizeService } from '@services/dot-personalize/dot-personalize.service';
import { DotPageRenderState, DotPageMode } from '@portlets/dot-edit-page/shared/models';
import * as _ from 'lodash';

const mockDotMessageService = new MockDotMessageService({
    'editpage.toolbar.edit.page': 'Edit',
    'editpage.toolbar.live.page': 'Live',
    'editpage.toolbar.preview.page': 'Preview',
    'editpage.content.steal.lock.confirmation.message.header': 'Lock',
    'editpage.content.steal.lock.confirmation.message': 'Steal lock',
    'editpage.personalization.confirm.message': 'Are you sure?',
    'editpage.personalization.confirm.header': 'Personalization',
    'editpage.personalization.confirm.with.lock': 'Also steal lcok'
});

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-edit-page-state-controller [pageState]="pageState"></dot-edit-page-state-controller>
    `
})
class TestHostComponent {
    pageState: DotPageRenderState = _.cloneDeep(mockDotRenderedPageState);
}

describe('DotEditPageStateControllerComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let component: DotEditPageStateControllerComponent;
    let de: DebugElement;
    let deHost: DebugElement;

    beforeEach(
        async(() => {
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
                    {
                        provide: DotAlertConfirmService,
                        useValue: {
                            confirm: jasmine.createSpy()
                        }
                    }
                ],
                imports: [InputSwitchModule, SelectButtonModule]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixtureHost = DOTTestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;
        de = deHost.query(By.css('dot-edit-page-state-controller'));
        component = de.componentInstance;
    });

    describe('elements', () => {
        describe('default', () => {
            beforeEach(() => {
                fixtureHost.detectChanges();
            });
            it('should have mode selector', () => {
                const selectButton = de.query(By.css('p-selectButton')).componentInstance;

                fixtureHost.whenRenderingDone().then(() => {
                    expect(selectButton).toBeDefined();
                    expect(selectButton.options).toEqual([
                        { label: 'Edit', value: 'EDIT_MODE', disabled: false },
                        { label: 'Preview', value: 'PREVIEW_MODE', disabled: false },
                        { label: 'Live', value: 'ADMIN_MODE', disabled: false }
                    ]);
                    expect(selectButton.value).toBe(DotPageMode.PREVIEW);
                });
            });

            it('should have locker', () => {
                const lockerDe = de.query(By.css('p-inputSwitch'));
                const locker = lockerDe.componentInstance;
                fixtureHost.whenRenderingDone().then(() => {
                    expect(lockerDe.classes.warn).toBe(false, 'warn class');
                    expect(locker.checked).toBe(true, 'checked');
                    expect(locker.disabled).toBe(false, 'disabled');
                });
            });

            it('should have lock info', () => {
                const message = de.query(By.css('dot-edit-page-lock-info')).componentInstance;
                expect(message.pageState).toEqual(mockDotRenderedPageState);
            });
        });

        describe('disable mode selector option', () => {
            it('should disable preview', () => {
                componentHost.pageState.page.canRead = false;
                fixtureHost.detectChanges();
                const selectButton = de.query(By.css('p-selectButton')).componentInstance;

                fixtureHost.whenRenderingDone().then(() => {
                    expect(selectButton).toBeDefined();
                    expect(selectButton.options[1]).toEqual({
                        label: 'Preview',
                        value: 'PREVIEW_MODE',
                        disabled: true
                    });
                    expect(selectButton.value).toBe(DotPageMode.PREVIEW);
                });
            });

            it('should disable edit', () => {
                componentHost.pageState.page.canEdit = false;
                componentHost.pageState.page.canLock = false;
                fixtureHost.detectChanges();
                const selectButton = de.query(By.css('p-selectButton')).componentInstance;

                fixtureHost.whenRenderingDone().then(() => {
                    expect(selectButton).toBeDefined();
                    expect(selectButton.options[0]).toEqual({
                        label: 'Edit',
                        value: 'EDIT_MODE',
                        disabled: true
                    });
                    expect(selectButton.value).toBe(DotPageMode.PREVIEW);
                });
            });

            it('should disable live', () => {
                componentHost.pageState.page.liveInode = null;
                fixtureHost.detectChanges();
                const selectButton = de.query(By.css('p-selectButton')).componentInstance;

                fixtureHost.whenRenderingDone().then(() => {
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
    });

    describe('events', () => {
        it('should emit modeChange', () => {
            fixtureHost.detectChanges();
            spyOn(component.modeChange, 'emit');

            const selectButton = de.query(By.css('p-selectButton'));
            selectButton.triggerEventHandler('onChange', {
                value: DotPageMode.EDIT
            });

            expect(component.modeChange.emit).toHaveBeenCalledWith(DotPageMode.EDIT);
        });
    });
});
