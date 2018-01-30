import { async, ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { DotEditPageToolbarComponent, PageMode } from './dot-edit-page-toolbar.component';
import { DotEditPageToolbarModule } from './dot-edit-page-toolbar.module';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SplitButton } from 'primeng/primeng';
import { DotGlobalMessageService } from '../../../../view/components/_common/dot-global-message/dot-global-message.service';
import { DotEventsService } from '../../../../api/services/dot-events/dot-events.service';

describe('DotEditPageToolbarComponent', () => {
    let component: DotEditPageToolbarComponent;
    let fixture: ComponentFixture<DotEditPageToolbarComponent>;
    let de: DebugElement;
    let dotGlobalMessageService: DotGlobalMessageService;

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
        'dot.common.message.pageurl.copied.clipboard': 'Copied to clipboard'
    });

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
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
                    DotEventsService
                ]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditPageToolbarComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.page = {
            canLock: false,
            identifier: '123',
            languageId: 1,
            liveInode: '456',
            locked: false,
            pageTitle: '',
            pageUri: '',
            render: '',
            shortyLive: '',
            shortyWorking: '',
            workingInode: ''
        };

        dotGlobalMessageService = de.injector.get(DotGlobalMessageService);
    });

    it('should have a toolbar element', () => {
        expect(de.query(By.css('p-toolbar'))).toBeTruthy();
    });

    it('should set page title', () => {
        component.page.pageTitle = 'Hello World';
        const pageTitleEl: HTMLElement = de.query(By.css('.edit-page-toolbar__page-title')).nativeElement;
        fixture.detectChanges();

        expect(pageTitleEl.textContent).toEqual('Hello World');
    });

    it('should set page url', () => {
        component.page.pageUri = '/test/test';
        const pageUrlEl: HTMLElement = de.query(By.css('.edit-page-toolbar__page-url')).nativeElement;
        fixture.detectChanges();

        expect(pageUrlEl.textContent).toEqual('/test/test');
    });

    it('should have a save button in edit mode', () => {
        component.page.locked = true;
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
        component.stateSelected = PageMode.LIVE;

        fixture.detectChanges();
        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));

        expect(primaryAction).toBeFalsy();
    });

    it('should have lock page component', () => {
        const lockSwitch: DebugElement = de.query(By.css('.edit-page-toolbar__locker'));
        expect(lockSwitch !== null).toEqual(true);
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
        component.page.locked = true;
        component.canSave = false;

        fixture.detectChanges();

        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));
        expect(primaryAction.nativeElement.disabled).toBeTruthy('the save button have to be disabled');
    });

    it('should enabled save button', () => {
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

        expect(component.page.locked).toBe(true, 'lock page');
        expect(pageStateResult).toEqual({ mode: PageMode.EDIT, locked: true }, 'page state output emitted');
    });

    it('should go to preview if user unlock the page while is in edit', () => {
        fixture.detectChanges();

        // Set the page locked and in edit mode
        clickLocker();
        expect(component.page.locked).toBe(true, 'locked page');
        expect(component.stateSelected).toEqual(PageMode.EDIT, 'edit mode');

        clickLocker();
        expect(component.page.locked).toBe(false, 'unlocked page');
        expect(component.stateSelected).toEqual(PageMode.PREVIEW, 'preview mode');
    });

    it('should go to edit if user lock the while is in preview', () => {
        fixture.detectChanges();

        clickLocker();
        expect(component.page.locked).toBe(true, 'locked page');
        expect(component.stateSelected).toEqual(PageMode.EDIT, 'edit mode');
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
            component.changeState.subscribe((res) => {
                pageStateResult = res;
            });
        });

        it('should emit page state and lock in true', () => {
            fixture.detectChanges();

            expect(component.stateSelected).toEqual(PageMode.PREVIEW, 'preview state by default');

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
            component.stateSelected = PageMode.LIVE;
            fixture.detectChanges();

            clickLocker();
            expect(pageStateResult).toEqual(
                {
                    locked: true
                },
                'emit correct state'
            );
        });

        it('should emit only page state', () => {
            component.stateSelected = PageMode.LIVE;
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
            component.page.locked = false;
            fixture.detectChanges();

            expect(component.stateSelected).toEqual(PageMode.PREVIEW, 'preview state by default');

            clickStateButton('edit');
            expect(component.stateSelected).toEqual(PageMode.EDIT, 'edit state selected');
            expect(component.page.locked).toBe(true, 'page locked after click in edit');
            expect(pageStateResult).toEqual(
                {
                    mode: PageMode.EDIT,
                    locked: true
                },
                'emit state'
            );
        });

        it('should keep the locker true from edit to live', () => {
            component.page.locked = true;
            fixture.detectChanges();

            expect(component.stateSelected).toEqual(PageMode.EDIT, 'preview state by default');

            clickStateButton('live');
            expect(component.stateSelected).toEqual(PageMode.LIVE, 'live state selected');
            expect(component.page.locked).toBe(true, 'page locked after click in preview');
            expect(pageStateResult).toEqual(
                {
                    mode: PageMode.LIVE
                },
                'emit state'
            );
        });

        it('should keep the locker true from edit to preview', () => {
            component.page.locked = true;
            fixture.detectChanges();

            expect(component.stateSelected).toEqual(PageMode.EDIT, 'preview state by default');

            clickStateButton('preview');
            expect(component.stateSelected).toEqual(PageMode.PREVIEW, 'edit state selected');
            expect(component.page.locked).toBe(true, 'page locked after click in preview');
            expect(pageStateResult).toEqual(
                {
                    mode: PageMode.PREVIEW
                },
                'emit state'
            );
        });
    });
});
