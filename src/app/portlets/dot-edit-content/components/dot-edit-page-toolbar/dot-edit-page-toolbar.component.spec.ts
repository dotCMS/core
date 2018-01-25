import { async, ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { DotEditPageToolbarComponent, PageState } from './dot-edit-page-toolbar.component';
import { DotEditPageToolbarModule } from './dot-edit-page-toolbar.module';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SplitButton } from 'primeng/primeng';

describe('DotEditPageToolbarComponent', () => {
    let component: DotEditPageToolbarComponent;
    let fixture: ComponentFixture<DotEditPageToolbarComponent>;
    let de: DebugElement;

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
        'editpage.toolbar.primary.workflow.actions': 'Acciones'
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
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditPageToolbarComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
    });

    it('should have a toolbar element', () => {
        expect(de.query(By.css('p-toolbar'))).toBeTruthy();
    });

    it('should set page title', () => {
        component.pageTitle = 'Hello World';
        const pageTitleEl: HTMLElement = de.query(By.css('.edit-page-toolbar__page-title')).nativeElement;
        fixture.detectChanges();

        expect(pageTitleEl.textContent).toEqual('Hello World');
    });

    it('should set page url', () => {
        component.pageUrl = '/test/test';
        const pageUrlEl: HTMLElement = de.query(By.css('.edit-page-toolbar__page-url')).nativeElement;
        fixture.detectChanges();

        expect(pageUrlEl.textContent).toEqual('/test/test');
    });

    it('should have a save button', () => {
        fixture.detectChanges();
        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));
        expect(primaryAction).toBeTruthy();

        const primaryActionEl: HTMLElement = primaryAction.nativeElement;
        expect(primaryActionEl.textContent).toEqual('Hello');
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
        component.canSave = false;

        fixture.detectChanges();

        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));
        expect(primaryAction.nativeElement.disabled).toBeTruthy('the save button have to be disabled');
    });

    it('should enabled save button', () => {
        component.canSave = true;

        fixture.detectChanges();

        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__save'));
        expect(primaryAction.nativeElement.disabled).toBeFalsy('the save button have to be enable');
    });

    it('should turn on the locker and emit page state and lock state when user click on edit mode', () => {
        fixture.detectChanges();

        let pageStateResult;
        component.pageState.subscribe((res) => {
            pageStateResult = res;
        });

        let lockPageResult;
        component.lockPage.subscribe((res) => {
            lockPageResult = res;
        });

        clickStateButton('edit');

        expect(component.pageLocked).toBe(true, 'lock page');
        expect(lockPageResult).toBe(true, 'lock page output emitted');
        expect(pageStateResult).toEqual(PageState.EDIT, 'page state output emitted');
    });

    it('should go to preview if user unlock the page while is in edit', () => {
        fixture.detectChanges();

        // Set the page locked and in edit mode
        clickLocker();
        expect(component.pageLocked).toBe(true, 'locked page');
        expect(component.stateSelected).toEqual(PageState.EDIT, 'edit mode');

        clickLocker();
        expect(component.pageLocked).toBe(false, 'unlocked page');
        expect(component.stateSelected).toEqual(PageState.PREVIEW, 'preview mode');
    });

    it('should go to edit if user lock the while is in preview', () => {
        fixture.detectChanges();

        clickLocker();
        expect(component.pageLocked).toBe(true, 'locked page');
        expect(component.stateSelected).toEqual(PageState.EDIT, 'edit mode');
    });

    describe('moving through page states', () => {
        let pageStateResult;
        let lockPageResult;

        beforeEach(() => {
            component.pageState.subscribe((res) => {
                pageStateResult = res;
            });

            component.lockPage.subscribe((res) => {
                lockPageResult = res;
            });

            component.pageLocked = true;
            fixture.detectChanges();
        });

        it('should keep the locker true from preview to edit', () => {
            expect(component.stateSelected).toEqual(PageState.PREVIEW, 'preview state by default');

            clickStateButton('edit');
            expect(component.stateSelected).toEqual(PageState.EDIT, 'edit state selected');
            expect(component.pageLocked).toBe(true, 'page locked after click in edit');
            expect(pageStateResult).toEqual(PageState.EDIT, 'emit edit');
            expect(lockPageResult).toBeUndefined('should not emit lock output');
        });

        it('should keep the locker true from preview to live', () => {
            expect(component.stateSelected).toEqual(PageState.PREVIEW, 'preview state by default');

            clickStateButton('live');
            expect(component.stateSelected).toEqual(PageState.LIVE, 'live state selected');
            expect(component.pageLocked).toBe(true, 'page locked after click in preview');
            expect(pageStateResult).toEqual(PageState.LIVE, 'emit live');
            expect(lockPageResult).toBeUndefined('should not emit lock output');
        });

        it('should keep the locker true from edit to preview', () => {
            clickStateButton('edit');
            expect(component.stateSelected).toEqual(PageState.EDIT, 'edit state by default');

            clickStateButton('preview');
            expect(component.stateSelected).toEqual(PageState.PREVIEW, 'edit state selected');
            expect(component.pageLocked).toBe(true, 'page locked after click in preview');
            expect(pageStateResult).toEqual(PageState.PREVIEW, 'emit preview');
            expect(lockPageResult).toBeUndefined('should not emit lock output');
        });
    });
});
