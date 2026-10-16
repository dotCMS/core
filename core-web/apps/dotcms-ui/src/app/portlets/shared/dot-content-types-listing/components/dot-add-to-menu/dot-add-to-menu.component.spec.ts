import { of } from 'rxjs';
import { vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import {
    DotCurrentUserService,
    DotMessageService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';
import {
    DotCurrentUserServiceMock,
    dotcmsContentTypeBasicMock,
    LoginServiceMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotAddToMenuComponent } from './dot-add-to-menu.component';

import {
    DotAddToMenuService,
    DotCreateCustomTool
} from '../../../../../api/services/add-to-menu/add-to-menu.service';

const contentTypeVar = {
    ...dotcmsContentTypeBasicMock,
    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
    id: '1234567890',
    name: 'Nuevo',
    variable: 'Nuevo',
    defaultType: false,
    fixed: false,
    folder: 'SYSTEM_FOLDER',
    host: null,
    owner: '123',
    system: false
};

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-add-to-menu [contentType]="contentType"></dot-add-to-menu>
    `,
    standalone: false
})
class TestHostComponent {
    contentType = contentTypeVar;
}

class DotAddToMenuServiceMock {
    cleanUpPorletId(_portletName: string) {
        /* */
    }

    createCustomTool(_params: DotCreateCustomTool) {
        /* */
    }

    addToLayout(_portletName: string, _layoutId: string) {
        /* */
    }
}

const menuResponse = {
    entity: [
        {
            id: '123',
            name: 'Menu 1',
            tabName: 'Name',
            tabIcon: 'icon',
            url: '/url/index',
            menuItems: [{ id: 'one', label: 'One', url: '/one', angular: true, ajax: false }]
        },
        {
            id: '456',
            name: 'Menu 2',
            tabName: 'Name 2',
            tabIcon: 'icon2',
            url: '/url/456',
            menuItems: [{ id: 'two', label: 'Two', url: '/two', angular: true, ajax: false }]
        }
    ]
};

describe('DotAddToMenuComponent', () => {
    let component: DotAddToMenuComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dotdialog: DebugElement;
    let dotAddToMenuService: DotAddToMenuService;
    let httpMock: HttpTestingController;
    let globalStore: InstanceType<typeof GlobalStore>;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.content.add_to_menu.header': 'Add to Menu',
        'contenttypes.content.add_to_menu.name': 'Name',
        'contenttypes.content.add_to_menu.show_under': 'Show under',
        'contenttypes.content.add_to_menu.default_view': 'Default view',
        'custom.content.portlet.dataViewMode.card': 'card',
        'custom.content.portlet.dataViewMode.list': 'list',
        add: 'Add',
        cancel: 'Cancel'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [DotAddToMenuComponent, BrowserAnimationsModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotAddToMenuService, useClass: DotAddToMenuServiceMock },
                {
                    provide: DotSystemConfigService,
                    useValue: { getSystemConfig: () => of({}) }
                },
                { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                GlobalStore,
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-add-to-menu'));
        component = de.componentInstance;
        dotAddToMenuService = TestBed.inject(DotAddToMenuService);
        httpMock = TestBed.inject(HttpTestingController);
        globalStore = TestBed.inject(GlobalStore);

        // The shell has already loaded the menu: the dropdown reads that snapshot.
        globalStore.reload();
        httpMock.expectOne('/api/v1/menu').flush(menuResponse);

        fixture.detectChanges();
        dotdialog = de.query(By.css('p-dialog'));
    });

    it('should have a form', () => {
        const form: DebugElement = de.query(By.css('form'));
        expect(form).not.toBeNull();
        expect(component.form).toEqual(form.componentInstance.form);
    });

    it('should load labels and data when init', () => {
        // Check title label
        expect(
            dotdialog.query(By.css('[data-testId="titleMenuLabel"]')).nativeElement.textContent
        ).toContain(messageServiceMock.get('contenttypes.content.add_to_menu.name'));
        // Check menu option label
        expect(
            dotdialog.query(By.css('[data-testId="menuOptionLabel"]')).nativeElement.textContent
        ).toContain(messageServiceMock.get('contenttypes.content.add_to_menu.show_under'));
        // Check view mode label
        expect(
            dotdialog.query(By.css('[data-testId="ViewModeLabel"]')).nativeElement.textContent
        ).toContain(messageServiceMock.get('contenttypes.content.add_to_menu.default_view'));
        // Check radio button labels (they are in sibling <label> elements inside .form-radio div)
        expect(
            dotdialog.query(By.css('.form-radio label[for="cardViewMode"]')).nativeElement
                .textContent
        ).toContain(messageServiceMock.get('custom.content.portlet.dataViewMode.card'));
        expect(
            dotdialog.query(By.css('.form-radio label[for="listViewMode"]')).nativeElement
                .textContent
        ).toContain(messageServiceMock.get('custom.content.portlet.dataViewMode.list'));

        expect(dotdialog.query(By.css('[data-testId="titleMenu"]')).nativeElement.value).toBe(
            contentTypeVar.name
        );
        // Check buttons text
        expect(
            dotdialog.query(By.css('[data-testId="dotDialogAcceptAction"]')).nativeElement
                .textContent
        ).toContain(messageServiceMock.get('add'));
        expect(
            dotdialog.query(By.css('[data-testId="dotDialogCancelAction"]')).nativeElement
                .textContent
        ).toContain(messageServiceMock.get('cancel'));
    });

    it('should load form values when init', () => {
        expect(component.form.get('defaultView').value).toEqual('list');
        expect(component.form.get('menuOption').value).toEqual('123');
        expect(component.form.get('title').value).toEqual(contentTypeVar.name);
        expect(component.form.valid).toEqual(true);
    });

    describe('layouts dropdown', () => {
        it('lists the groups of the navigation, the same ones the bar shows', () => {
            expect(component.$menuOptions()).toEqual([
                { id: '123', label: 'Name' },
                { id: '456', label: 'Name 2' }
            ]);
            expect(component.$menuOptions().map((option) => option.label)).toEqual(
                globalStore.menuGroup().map((group) => group.label)
            );
        });

        it('makes no menu request of its own when the menu is already loaded', () => {
            httpMock.expectNone('/api/v1/menu');
        });

        it('follows a later layout update without asking again', () => {
            globalStore.reload();
            httpMock.expectOne('/api/v1/menu').flush({ entity: [menuResponse.entity[1]] });

            expect(component.$menuOptions()).toEqual([{ id: '456', label: 'Name 2' }]);
        });

        it('does not refetch the menu after a tool is added', () => {
            vi.spyOn(dotAddToMenuService, 'createCustomTool').mockReturnValue(of(''));
            vi.spyOn(dotAddToMenuService, 'addToLayout').mockReturnValue(of(''));

            dotdialog.query(By.css('[data-testId="dotDialogAcceptAction"]')).nativeElement.click();

            httpMock.expectNone('/api/v1/menu');
        });
    });

    it('should invalidate form and set Add button disabled, when name empty', () => {
        component.form.patchValue({
            title: null
        });
        fixture.detectChanges();
        const acceptButton = dotdialog.query(By.css('[data-testId="dotDialogAcceptAction"]'));
        expect(acceptButton.componentInstance.disabled).toBe(true);
        expect(component.form.valid).toEqual(false);
    });

    it('should submit form', () => {
        const addButton: DebugElement = dotdialog.query(
            By.css('[data-testId="dotDialogAcceptAction"]')
        );

        vi.spyOn(dotAddToMenuService, 'createCustomTool').mockReturnValue(of(''));
        vi.spyOn(dotAddToMenuService, 'addToLayout').mockReturnValue(of(''));
        vi.spyOn(component.cancel, 'emit');

        addButton.nativeElement.click();

        expect(dotAddToMenuService.createCustomTool).toHaveBeenCalledWith({
            portletName: contentTypeVar.name,
            contentTypes: contentTypeVar.variable,
            dataViewMode: 'list'
        });
        expect(dotAddToMenuService.addToLayout).toHaveBeenCalledWith({
            portletName: 'Nuevo',
            dataViewMode: 'list',
            layoutId: component.form.get('menuOption').value
        });
        expect(component.cancel.emit).toHaveBeenCalledTimes(1);
    });

    it('should emit Cancel event on close button click', () => {
        const cancelButton: DebugElement = dotdialog.query(
            By.css('[data-testId="dotDialogCancelAction"]')
        );

        vi.spyOn(component.cancel, 'emit');
        cancelButton.nativeElement.click();

        expect(component.cancel.emit).toHaveBeenCalledTimes(1);
    });
});
