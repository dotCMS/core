import { LoginServiceMock } from './../../../../../test/login-service.mock';
import { LoginService } from 'dotcms-js/core/login.service';
import { DotDialogService } from '../../../../../api/services/dot-dialog/dot-dialog.service';
import { async, ComponentFixture } from '@angular/core/testing';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { DotEditPageToolbarModule } from './dot-edit-page-toolbar.module';
import { DotEditPageWorkflowsActionsModule } from '../dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { DebugElement, Component, Input } from '@angular/core';
import { By } from '@angular/platform-browser';
import * as _ from 'lodash';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotGlobalMessageService } from '../../../../../view/components/_common/dot-global-message/dot-global-message.service';
import { DotEventsService } from '../../../../../api/services/dot-events/dot-events.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { PageMode } from '../../../shared/models/page-mode.enum';
import { DotRenderedPageState } from '../../../shared/models/dot-rendered-page-state.model';
import { mockUser } from '../../../../../test/login-service.mock';
import { DotWorkflowServiceMock } from '../../../../../test/dot-workflow-service.mock';
import { DotWorkflowService } from '../../../../../api/services/dot-workflow/dot-workflow.service';
import { mockDotPage, mockDotLayout } from '../../../../../test/dot-rendered-page.mock';

@Component({
    selector: 'dot-edit-page-workflows-actions',
    template: ''
})
class MockWorkflowActionsComponent {
    @Input() inode = '';
    @Input() label = 'Acciones';
}

@Component({
    selector: 'dot-test-host-component',
    template: `<dot-edit-page-toolbar [canSave]="canSave" [pageState]="pageState"></dot-edit-page-toolbar>`
})
class TestHostComponent {
    @Input() canSave: boolean;
    @Input() pageState: DotRenderedPageState;
}

describe('DotEditPageToolbarComponent', () => {
    let component: DotEditPageToolbarComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotDialogService: DotDialogService;

    const states = {
        edit: 0,
        preview: 1,
        live: 2
    };

    function clickStateButton(state) {
        const stateSelectorButtons: DebugElement[] = de.queryAll(
            By.css('.edit-page-toolbar__state-selector .ui-button')
        );
        const button = stateSelectorButtons[states[state]].nativeElement;
        button.click();
    }

    function clickLocker() {
        const lockerSwitch: DebugElement = de.query(By.css('.edit-page-toolbar__locker .ui-inputswitch'));
        lockerSwitch.nativeElement.click();
    }

    const messageServiceMock = new MockDotMessageService({
        'editpage.toolbar.primary.action': 'Hello',
        'editpage.toolbar.edit.page': 'Edit',
        'editpage.toolbar.preview.page': 'Preview',
        'editpage.toolbar.live.page': 'Live',
        'editpage.toolbar.primary.workflow.actions': 'Acciones',
        'dot.common.message.pageurl.copied.clipboard': 'Copied to clipboard',
        'editpage.toolbar.page.locked.by.user': 'Page is locked',
        'editpage.toolbar.page.cant.edit': 'You dont have permissions'
    });

    let testbed;

    beforeEach(
        async(() => {
            testbed = DOTTestBed.configureTestingModule({
                declarations: [
                    MockWorkflowActionsComponent,
                    TestHostComponent
                ],
                imports: [
                    DotEditPageToolbarModule,
                    DotEditPageWorkflowsActionsModule,
                    RouterTestingModule.withRoutes([
                        {
                            component: DotEditPageToolbarComponent,
                            path: 'test'
                        }
                    ]),
                    BrowserAnimationsModule
                ],
                providers: [
                    { provide: DotMessageService, useValue: messageServiceMock },
                    DotGlobalMessageService,
                    DotEventsService,
                    {
                        provide: DotWorkflowService,
                        useClass: DotWorkflowServiceMock
                    },
                    {
                        provide: LoginService,
                        useClass: LoginServiceMock
                    }
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = testbed.createComponent(TestHostComponent);
        de = fixture.debugElement;
        component = de.query(By.css('dot-edit-page-toolbar')).componentInstance;
        fixture.componentInstance.pageState = new DotRenderedPageState(
            mockUser,
            {
                page: {
                    ...mockDotPage,
                    canEdit: true,
                    canLock: true,
                    identifier: '123',
                    languageId: 1,
                    liveInode: '456',
                    title: '',
                    pageURI: '',
                    shortyLive: '',
                    shortyWorking: '',
                    workingInode: '',
                    lockedBy: null
                },
                html: '',
                layout: mockDotLayout,
                canCreateTemplate: true,
                viewAs: null
            },
            PageMode.PREVIEW
        );

        dotGlobalMessageService = de.injector.get(DotGlobalMessageService);
        dotDialogService = de.injector.get(DotDialogService);
    });

    it('should have a toolbar element', () => {
        expect(de.query(By.css('p-toolbar'))).toBeTruthy();
    });

    it('should set page title', () => {
        fixture.componentInstance.pageState.page.title = 'Hello World';
        const pageTitleEl: HTMLElement = de.query(By.css('.edit-page-toolbar__page-title')).nativeElement;
        fixture.detectChanges();

        expect(pageTitleEl.textContent).toContain('Hello World');
    });

    it('should set page url', () => {
        fixture.componentInstance.pageState.page.pageURI = '/test/test';
        const pageUrlEl: HTMLElement = de.query(By.css('.edit-page-toolbar__page-url')).nativeElement;
        fixture.detectChanges();

        expect(pageUrlEl.textContent).toEqual('/test/test');
    });

    it('should have a save button in edit mode', () => {
        fixture.componentInstance.pageState.state.locked = true;
        fixture.componentInstance.pageState.state.mode = PageMode.EDIT;

        fixture.detectChanges();
        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));
        expect(primaryAction).toBeTruthy();

        const primaryActionEl: HTMLElement = primaryAction.nativeElement;
        expect(primaryActionEl.textContent).toEqual('Hello');
    });

    it('should have lockerModel in true when the page state is LIVE and the page is locked', () => {
        fixture.componentInstance.pageState.state.locked = true;
        fixture.componentInstance.pageState.state.mode = PageMode.LIVE;

        fixture.detectChanges();

        expect(component.lockerModel).toBeTruthy();
    });

    it('should have lockerModel in true when the page state is PREVIEW and the page is locked', () => {
        fixture.componentInstance.pageState.state.locked = true;
        fixture.componentInstance.pageState.state.mode = PageMode.PREVIEW;

        fixture.detectChanges();

        expect(component.lockerModel).toBeTruthy();
    });

    it('should have lockerModel in false when the page state is LIVE and the page is unlocked', () => {
        fixture.componentInstance.pageState.state.locked = false;
        fixture.componentInstance.pageState.state.mode = PageMode.LIVE;

        fixture.detectChanges();

        expect(component.lockerModel).toBeFalsy();
    });

    it('should have lockerModel in false when the page state is PREVIEW and the page is unlocked', () => {
        fixture.componentInstance.pageState.state.locked = false;
        fixture.componentInstance.pageState.state.mode = PageMode.PREVIEW;

        fixture.detectChanges();

        expect(component.lockerModel).toBeFalsy();
    });

    it('should hide the save button in preview mode', () => {
        fixture.detectChanges();
        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));

        expect(primaryAction).toBeFalsy();
    });

    it('should hide the save button in live mode', () => {
        fixture.componentInstance.pageState.state.mode = PageMode.LIVE;

        fixture.detectChanges();
        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));

        expect(primaryAction).toBeFalsy();
    });

    it('should have lock page component and no warn class', () => {
        fixture.detectChanges();
        const lockSwitch: DebugElement = de.query(By.css('.edit-page-toolbar__locker'));
        expect(lockSwitch !== null).toEqual(true);
        expect(lockSwitch.classes.warn).toBe(false);
    });

    it('should warn class in the locker', () => {
        fixture.componentInstance.pageState.state.lockedByAnotherUser = true;
        fixture.detectChanges();

        const lockSwitch: DebugElement = de.query(By.css('.edit-page-toolbar__locker'));
        expect(lockSwitch.classes.warn).toBe(true);
    });

    it('should have locker enabled', () => {
        fixture.detectChanges();
        const lockSwitch: DebugElement = de.query(By.css('.edit-page-toolbar__locker'));

        expect(lockSwitch.componentInstance.disabled).toBe(false);
    });

    it('should have locker disabled', () => {
        fixture.componentInstance.pageState.page.canLock = false;
        fixture.detectChanges();
        const lockSwitch: DebugElement = de.query(By.css('.edit-page-toolbar__locker'));

        expect(lockSwitch.componentInstance.disabled).toBe(true);
    });

    it('should have page is locked by another user message and disabled edit button', () => {
        fixture.componentInstance.pageState.state.lockedByAnotherUser = true;
        fixture.componentInstance.pageState.page.canLock = false;
        fixture.detectChanges();

        const lockedMessage: DebugElement = de.query(By.css('.edit-page-toolbar__locked-by-message'));
        expect(lockedMessage.nativeElement.textContent).toContain('Page is locked');

        const editStateModel = component.states.find((state) => state.label === 'Edit');
        expect(editStateModel.styleClass).toEqual('edit-page-toolbar__state-selector-item--disabled');
    });

    it('should have page can\'t edit message and disabled edit button', () => {
        fixture.componentInstance.pageState.page.canEdit = false;
        fixture.detectChanges();

        const lockedMessage: DebugElement = de.query(By.css('.edit-page-toolbar__cant-edit-message'));
        expect(lockedMessage.nativeElement.textContent).toContain('You dont have permissions');
        const editStateModel = component.states.find((state) => state.label === 'Edit');
        expect(editStateModel.styleClass).toEqual('edit-page-toolbar__state-selector-item--disabled');
    });

    it('should not have have any locked messages', () => {
        fixture.detectChanges();
        const cantEditMessage: DebugElement = de.query(By.css('.edit-page-toolbar__cant-edit-message'));
        const lockedMessage: DebugElement = de.query(By.css('.edit-page-toolbar__locked-by-message'));
        expect(cantEditMessage === null).toBe(true);
        expect(lockedMessage === null).toBe(true);
    });

    it('should blink page is locked message', () => {
        spyOn(component, 'onLockerClick');
        fixture.componentInstance.pageState.page.canLock = false;
        fixture.detectChanges();

        const lockSwitch: DebugElement = de.query(By.css('.edit-page-toolbar__locker'));
        lockSwitch.nativeElement.click();
        expect(component.onLockerClick).toHaveBeenCalledTimes(1);
    });

    it('should have an action split button', () => {
        const primaryAction: DebugElement = de.query(By.css('dot-edit-page-workflows-actions'));
        expect(primaryAction === null).toBe(false);
    });

    it('should have right inputs in WorkflowActions component', () => {
        const mtest: DebugElement = fixture.debugElement.query(By.css('dot-edit-page-workflows-actions'));
        fixture.componentInstance.pageState.page.workingInode = 'cc2cdf9c-a20d-4862-9454-2a76c1132123';
        fixture.detectChanges();

        expect(mtest.componentInstance.label).toEqual(messageServiceMock.get('editpage.toolbar.primary.workflow.actions'));
        expect(mtest.componentInstance.page.workingInode).toEqual(component.pageState.page.workingInode);
    });

    it('should emit save event on primary action button click', () => {
        fixture.componentInstance.pageState.state.mode = PageMode.EDIT;
        fixture.componentInstance.pageState.state.locked = true;
        fixture.componentInstance.canSave = true;

        fixture.detectChanges();

        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));

        let result;
        component.save.subscribe((event) => {
            result = event;
        });
        primaryAction.nativeElement.click();

        expect(result).toBeDefined();
    });

    it('should disabled save button', () => {
        fixture.componentInstance.pageState.state.mode = PageMode.EDIT;
        fixture.componentInstance.pageState.state.locked = true;
        fixture.componentInstance.canSave = false;

        fixture.detectChanges();

        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));
        expect(primaryAction.nativeElement.disabled).toBeTruthy('the save button have to be disabled');
    });

    it('should enabled save button', () => {
        fixture.componentInstance.pageState.state.mode = PageMode.EDIT;
        fixture.componentInstance.pageState.state.locked = true;
        fixture.componentInstance.canSave = true;

        fixture.detectChanges();

        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));
        expect(primaryAction.nativeElement.disabled).toBeFalsy('the save button have to be enable');
    });

    it('should turn on the locker and emit page state and lock state when user click on edit mode', () => {
        fixture.detectChanges();

        let pageStateResult;
        component.changeState.subscribe((res) => {
            pageStateResult = res;
        });

        clickStateButton('edit');

        expect(component.lockerModel).toBe(true, 'lock page');
        expect(pageStateResult).toEqual({ mode: PageMode.EDIT, locked: true }, 'page state output emitted');
    });

    it('should go to preview if user unlock the page while is in edit', () => {
        fixture.componentInstance.pageState.state.mode = PageMode.PREVIEW;
        fixture.detectChanges();

        // Set the page locked and in edit mode
        clickLocker();
        expect(component.lockerModel).toBe(true, 'locked page');

        clickLocker();
        expect(component.lockerModel).toBe(false, 'unlocked page');
    });

    it('should go to edit if user lock the while is in preview', () => {
        fixture.componentInstance.pageState.state.mode = PageMode.PREVIEW;
        fixture.detectChanges();

        clickLocker();
        expect(component.lockerModel).toBe(true, 'locked page');
    });

    it('should copy to clipboard url', () => {
        spyOn(dotGlobalMessageService, 'display');
        spyOn(component, 'copyUrlToClipboard').and.callThrough();
        spyOn(document, 'execCommand');
        fixture.detectChanges();

        const copyUrlButton: DebugElement = de.query(By.css('.edit-page-toolbar__copy-url'));

        copyUrlButton.nativeElement.click();

        expect(component.copyUrlToClipboard).toHaveBeenCalledTimes(1);
        expect(document.execCommand).toHaveBeenCalledWith('copy');

        /*
            I want to test the global message being called but for some reason in the context of the test, the
            document.execCommand('copy') returns undefined.
        */
        // expect(dotGlobalMessageService.display).toHaveBeenCalledWith('Copied to clipboard');
    });

    describe('emit page state', () => {
        let pageStateResult;

        beforeEach(() => {
            pageStateResult = undefined;

            component.changeState.subscribe((res) => {
                pageStateResult = res;
            });
        });

        it('should emit preview page state and lock in true', () => {
            fixture.componentInstance.pageState.state.mode = PageMode.PREVIEW;
            fixture.detectChanges();

            clickLocker();
            expect(pageStateResult).toEqual(
                {
                    locked: true,
                    mode: PageMode.PREVIEW
                },
                'emit correct state'
            );
        });

        it('should call confirmation service on lock attemp when page is locked by another user', () => {
            spyOn(dotDialogService, 'confirm');
            fixture.componentInstance.pageState.state.locked = false;
            fixture.componentInstance.pageState.state.lockedByAnotherUser = true;
            fixture.componentInstance.pageState.state.mode = PageMode.LIVE;

            fixture.detectChanges();

            clickLocker();
            expect(dotDialogService.confirm).toHaveBeenCalledTimes(1);
        });

        it('should emit state on lock attemp when confirmation accept', () => {
            spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });

            fixture.componentInstance.pageState.state.lockedByAnotherUser = true;
            fixture.componentInstance.pageState.state.mode = PageMode.LIVE;

            fixture.detectChanges();

            clickLocker();

            expect(pageStateResult).toEqual(
                {
                    locked: true,
                    mode: PageMode.LIVE
                },
                'emit correct state'
            );
        });

        it('should not emit state on lock attemp when confirmation reject and set lockermodel to false', () => {
            spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
                conf.reject();
            });

            fixture.componentInstance.pageState.state.lockedByAnotherUser = true;
            fixture.componentInstance.pageState.state.mode = PageMode.LIVE;

            fixture.detectChanges();

            clickLocker();

            expect(component.lockerModel).toBe(false);
            expect(component.mode ).toBe(PageMode.LIVE, 'The mode should be the same');
            expect(pageStateResult).toEqual(undefined, 'doesn\'t emit state');
        });

        it('should call confirmation service on edit attemp when page is locked by another user', () => {
            spyOn(dotDialogService, 'confirm');
            fixture.componentInstance.pageState.state.lockedByAnotherUser = true;
            fixture.componentInstance.pageState.state.mode = PageMode.PREVIEW;

            fixture.detectChanges();

            clickStateButton('edit');
            expect(dotDialogService.confirm).toHaveBeenCalledTimes(1);
        });

        it('should emit state on edit attemp when confirmation accept', () => {
            spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });

            fixture.componentInstance.pageState.state.locked = true;
            fixture.componentInstance.pageState.state.lockedByAnotherUser = true;
            fixture.componentInstance.pageState.state.mode = PageMode.PREVIEW;

            fixture.detectChanges();

            clickStateButton('edit');

            expect(pageStateResult).toEqual(
                {
                    locked: true,
                    mode: PageMode.EDIT
                },
                'emit correct state'
            );
        });

        it('should not emit state on edit attemp when confirmation reject', () => {
            spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
                conf.reject();
            });

            fixture.componentInstance.pageState.state.lockedByAnotherUser = true;
            fixture.componentInstance.pageState.state.mode = PageMode.PREVIEW;

            fixture.detectChanges();

            clickStateButton('edit');

            expect(pageStateResult).toEqual(undefined, 'doesn\'t emit state');
            expect(component.lockerModel).toBe(false);
        });

        it('should not change mode on edit attemp when confirmation reject', () => {
            spyOn(dotDialogService, 'confirm').and.callFake((conf) => {
                conf.reject();
            });

            fixture.componentInstance.pageState.state.lockedByAnotherUser = true;
            fixture.componentInstance.pageState.state.mode = PageMode.LIVE;

            fixture.detectChanges();
            clickStateButton('edit');

            expect(component.mode).toEqual(PageMode.LIVE);
        });

        it('should set the locker true from preview to edit', () => {
            fixture.componentInstance.pageState.state.mode = PageMode.PREVIEW;
            fixture.componentInstance.pageState.state.locked = false;
            fixture.detectChanges();

            clickStateButton('edit');
            expect(component.lockerModel).toBe(true, 'page locked after click in edit');
        });

        it('should keep the locker true from edit to live', () => {
            fixture.componentInstance.pageState.state.mode = PageMode.EDIT;
            fixture.componentInstance.pageState.state.locked = true;
            fixture.detectChanges();

            clickStateButton('live');
            expect(component.lockerModel).toBe(true, 'page locked after click in preview');
            expect(pageStateResult.locked).toBeUndefined();
        });

        it('should keep the locker true from edit to preview', () => {
            fixture.componentInstance.pageState.state.mode = PageMode.EDIT;
            fixture.componentInstance.pageState.state.locked = true;
            fixture.detectChanges();

            clickStateButton('preview');
            expect(component.lockerModel).toBe(true, 'page locked after click in preview');
            expect(pageStateResult.locked).toBeUndefined();
        });

        it('should not change locker when change from preview to live mode', () => {
            fixture.componentInstance.pageState.state.mode = PageMode.PREVIEW;
            fixture.componentInstance.pageState.state.locked = false;
            fixture.detectChanges();

            clickStateButton('live');
            expect(component.lockerModel).toBe(false);
            expect(pageStateResult.locked).toBeUndefined();
        });

        it('should edit tab don\'t be called twice', () => {
            spyOn(_, 'debounce').and.callFake(function(cb) {
                return function() {
                    cb();
                };
            });
            spyOn(component.changeState, 'emit');
            fixture.detectChanges();

            clickStateButton('live');
            fixture.detectChanges();

            clickStateButton('edit');
            fixture.detectChanges();

            clickStateButton('edit');
            fixture.detectChanges();

            clickStateButton('edit');
            fixture.detectChanges();

            expect(component.changeState.emit).toHaveBeenCalledTimes(1);
        });
    });
});
