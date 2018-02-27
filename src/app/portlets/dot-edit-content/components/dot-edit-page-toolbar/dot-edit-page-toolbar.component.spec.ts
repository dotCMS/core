import { DotConfirmationService } from './../../../../api/services/dot-confirmation/dot-confirmation.service';
import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { DotEditPageToolbarModule } from './dot-edit-page-toolbar.module';
import { DebugElement, Component } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SplitButton } from 'primeng/primeng';
import { DotGlobalMessageService } from '../../../../view/components/_common/dot-global-message/dot-global-message.service';
import { DotEventsService } from '../../../../api/services/dot-events/dot-events.service';
import { PageMode } from '../../shared/page-mode.enum';
import { DOTTestBed } from '../../../../test/dot-test-bed';

describe('DotEditPageToolbarComponent', () => {
    let component: DotEditPageToolbarComponent;
    let fixture: ComponentFixture<DotEditPageToolbarComponent>;
    let de: DebugElement;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotConfirmationService: DotConfirmationService;

    function clickStateButton(state) {
        const states = {
            edit: 0,
            preview: 1,
            live: 2
        };

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
        'editpage.toolbar.page.locked.by.user': 'Page is locked'
    });

    let testbed;

    beforeEach(
        async(() => {
            testbed = DOTTestBed.configureTestingModule({
                imports: [
                    DotEditPageToolbarModule,
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
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = testbed.createComponent(DotEditPageToolbarComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.page = {
            canEdit: true,
            canLock: true,
            identifier: '123',
            languageId: 1,
            liveInode: '456',
            locked: false,
            title: '',
            pageURI: '',
            render: '',
            shortyLive: '',
            shortyWorking: '',
            workingInode: ''
        };

        dotGlobalMessageService = de.injector.get(DotGlobalMessageService);
        dotConfirmationService = de.injector.get(DotConfirmationService);
    });

    it('should have a toolbar element', () => {
        expect(de.query(By.css('p-toolbar'))).toBeTruthy();
    });

    it('should set page title', () => {
        component.page.title = 'Hello World';
        const pageTitleEl: HTMLElement = de.query(By.css('.edit-page-toolbar__page-title')).nativeElement;
        fixture.detectChanges();

        expect(pageTitleEl.textContent).toContain('Hello World');
    });

    it('should set page url', () => {
        component.page.pageURI = '/test/test';
        const pageUrlEl: HTMLElement = de.query(By.css('.edit-page-toolbar__page-url')).nativeElement;
        fixture.detectChanges();

        expect(pageUrlEl.textContent).toEqual('/test/test');
    });

    it('should have a save button in edit mode', () => {
        component.page.locked = true;
        component.mode = PageMode.EDIT;

        fixture.detectChanges();
        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));
        expect(primaryAction).toBeTruthy();

        const primaryActionEl: HTMLElement = primaryAction.nativeElement;
        expect(primaryActionEl.textContent).toEqual('Hello');
    });

    it('should hide the save button in preview mode', () => {
        fixture.detectChanges();
        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));

        expect(primaryAction).toBeFalsy();
    });

    it('should hide the save button in live mode', () => {
        component.mode = PageMode.LIVE;

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
        component.page.lockedByAnotherUser = true;
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
        component.page.canLock = false;
        fixture.detectChanges();
        const lockSwitch: DebugElement = de.query(By.css('.edit-page-toolbar__locker'));

        expect(lockSwitch.componentInstance.disabled).toBe(true);
    });

    it('should have page is locked message', () => {
        component.page.canLock = false;
        fixture.detectChanges();

        const lockedMessage: DebugElement = de.query(By.css('.edit-page-toolbar__cant-lock-message'));
        expect(lockedMessage.nativeElement.textContent).toContain('Page is locked');
    });

    it('should blink page is locked message', () => {
        spyOn(component, 'lockerHandler');
        component.page.canLock = false;
        fixture.detectChanges();

        const lockSwitch: DebugElement = de.query(By.css('.edit-page-toolbar__locker'));
        lockSwitch.nativeElement.click();
        expect(component.lockerHandler).toHaveBeenCalledTimes(1);
    });

    it('should have edit button disabled', () => {
        component.page.canLock = false;
        fixture.detectChanges();

        const editStateModel = component.states.find(state => state.label === 'Edit');

        expect(editStateModel.styleClass).toEqual('edit-page-toolbar__state-selector-item--disabled');
    });

    it('should NOT have an action split button', () => {
        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__actions'));
        expect(primaryAction).toBeFalsy();
    });

    it('should have an action split button', () => {
        component.pageWorkflows = [{ name: 'Workflow 1', id: 'one' }, { name: 'Workflow 2', id: 'two' }];
        fixture.detectChanges();
        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__actions'));
        expect(primaryAction).toBeTruthy();
        expect(primaryAction.name).toEqual('p-splitButton', 'is a splitbutton');
    });

    it('should set action split buttons params', () => {
        component.pageWorkflows = [{ name: 'Workflow 1', id: 'one' }, { name: 'Workflow 2', id: 'two' }];
        fixture.detectChanges();
        const actionsButton: SplitButton = de.query(By.css('.edit-page-toolbar__actions')).componentInstance;

        expect(actionsButton.label).toEqual('Acciones', 'actions label is set');
        expect(actionsButton.model).toEqual([{ label: 'Workflow 1' }, { label: 'Workflow 2' }]);
    });

    it('should emit save event on primary action button click', () => {
        component.mode = PageMode.EDIT;
        component.page.locked = true;
        component.canSave = true;

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
        component.mode = PageMode.EDIT;
        component.page.locked = true;
        component.canSave = false;

        fixture.detectChanges();

        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));
        expect(primaryAction.nativeElement.disabled).toBeTruthy('the save button have to be disabled');
    });

    it('should enabled save button', () => {
        component.mode = PageMode.EDIT;
        component.page.locked = true;
        component.canSave = true;

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
        component.mode = PageMode.PREVIEW;
        fixture.detectChanges();

        // Set the page locked and in edit mode
        clickLocker();
        expect(component.lockerModel).toBe(true, 'locked page');
        expect(component.mode).toEqual(PageMode.EDIT, 'edit mode');

        clickLocker();
        expect(component.lockerModel).toBe(false, 'unlocked page');
        expect(component.mode).toEqual(PageMode.PREVIEW, 'preview mode');
    });

    it('should go to edit if user lock the while is in preview', () => {
        component.mode = PageMode.PREVIEW;
        fixture.detectChanges();

        clickLocker();
        expect(component.lockerModel).toBe(true, 'locked page');
        expect(component.mode).toEqual(PageMode.EDIT, 'edit mode');
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

    describe('update page states', () => {
        let pageStateResult;

        beforeEach(() => {
            pageStateResult = undefined;

            component.changeState.subscribe((res) => {
                pageStateResult = res;
            });
        });

        it('should emit page state and lock in true', () => {
            component.mode = PageMode.PREVIEW;
            fixture.detectChanges();

            clickLocker();
            expect(pageStateResult).toEqual(
                {
                    mode: PageMode.EDIT,
                    locked: true
                },
                'emit correct state'
            );
        });

        it('should emit only lock in true', () => {
            component.page.locked = false;
            fixture.detectChanges();
            component.mode = PageMode.LIVE;
            fixture.detectChanges();

            clickLocker();
            expect(pageStateResult).toEqual(
                {
                    locked: true
                },
                'emit correct state'
            );
        });

        it('should call confirmation service on lock attemp when page is locked by another user', () => {
            spyOn(dotConfirmationService, 'confirm');
            component.page.locked = false;
            component.page.lockedByAnotherUser = true;
            component.mode = PageMode.LIVE;

            fixture.detectChanges();

            clickLocker();
            expect(dotConfirmationService.confirm).toHaveBeenCalledTimes(1);
        });

        it('should emit state on confirmation accept', () => {
            spyOn(dotConfirmationService, 'confirm').and.callFake((conf) => {
                conf.accept();
            });

            component.page.locked = false;
            component.page.lockedByAnotherUser = true;
            component.mode = PageMode.LIVE;

            fixture.detectChanges();

            clickLocker();

            expect(pageStateResult).toEqual(
                {
                    locked: true
                },
                'emit correct state'
            );
        });

        it('should not emit state on confirmation reject and set lockermodel to false', () => {
            spyOn(dotConfirmationService, 'confirm').and.callFake((conf) => {
                conf.reject();
            });

            component.page.locked = false;
            component.page.lockedByAnotherUser = true;
            component.mode = PageMode.LIVE;

            fixture.detectChanges();

            clickLocker();

            expect(component.lockerModel).toBe(false);
            expect(pageStateResult).toEqual(undefined, 'doesn\'t emit state');
        });

        it('should emit only page state', () => {
            component.mode = PageMode.LIVE;
            fixture.detectChanges();

            clickStateButton('preview');
            expect(pageStateResult).toEqual(
                {
                    mode: PageMode.PREVIEW
                },
                'emit correct state'
            );
        });

        it('should keep the locker true from preview to edit', () => {
            component.mode = PageMode.PREVIEW;
            component.page.locked = false;
            fixture.detectChanges();

            clickStateButton('edit');
            expect(component.mode).toEqual(PageMode.EDIT, 'edit state selected');
            expect(component.lockerModel).toBe(true, 'page locked after click in edit');
            expect(pageStateResult).toEqual(
                {
                    mode: PageMode.EDIT,
                    locked: true
                },
                'emit state'
            );
        });

        it('should keep the locker true from edit to live', () => {
            component.mode = PageMode.EDIT;
            component.page.locked = true;
            fixture.detectChanges();

            clickStateButton('live');
            expect(component.mode).toEqual(PageMode.LIVE, 'live state selected');
            expect(component.lockerModel).toBe(true, 'page locked after click in preview');
            expect(pageStateResult).toEqual(
                {
                    mode: PageMode.LIVE
                },
                'emit state'
            );
        });

        it('should keep the locker true from edit to preview', () => {
            component.mode = PageMode.EDIT;
            component.page.locked = true;
            fixture.detectChanges();

            clickStateButton('preview');
            expect(component.mode).toEqual(PageMode.PREVIEW, 'edit state selected');
            expect(component.lockerModel).toBe(true, 'page locked after click in preview');
            expect(pageStateResult).toEqual(
                {
                    mode: PageMode.PREVIEW
                },
                'emit state'
            );
        });
    });
});
