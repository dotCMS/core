import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { HttpClientTestingModule, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService, DotSystemConfigService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';
import {
    CoreWebServiceMock,
    dotcmsContentTypeBasicMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotAddToMenuComponent } from './dot-add-to-menu.component';

import {
    DotAddToMenuService,
    DotCreateCustomTool
} from '../../../../../api/services/add-to-menu/add-to-menu.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DotNavigationService } from '../../../../../view/components/dot-navigation/services/dot-navigation.service';
import { DotFormSelectorComponent } from '../../../../dot-edit-page/content/components/dot-form-selector/dot-form-selector.component';

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

class DotMenuServiceMock {
    loadMenu(_force?: boolean) {
        return of([
            {
                id: '123',
                name: 'Menu 1',
                tabName: 'Name',
                tabDescription: 'Description',
                tabIcon: 'icon',
                url: '/url/index',
                menuItems: []
            },
            {
                id: '456',
                name: 'Menu 2',
                tabName: 'Name 2',
                tabDescription: 'Description 2',
                tabIcon: 'icon2',
                url: '/url/456',
                menuItems: []
            }
        ]);
    }

    getDotMenuId(_portletId: string) {
        return of('123');
    }
}

describe('DotAddToMenuComponent', () => {
    let component: DotAddToMenuComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dotdialog: DebugElement;
    let dotAddToMenuService: DotAddToMenuService;
    let dotMenuService: DotMenuService;

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
            imports: [
                DotAddToMenuComponent,
                BrowserAnimationsModule,
                DotFormSelectorComponent,
                HttpClientTestingModule
            ],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotAddToMenuService, useClass: DotAddToMenuServiceMock },
                { provide: DotMenuService, useClass: DotMenuServiceMock },
                {
                    provide: DotSystemConfigService,
                    useValue: { getSystemConfig: () => of({}) }
                },
                GlobalStore,
                provideHttpClient(),
                provideHttpClientTesting(),
                DotNavigationService
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-add-to-menu'));
        component = de.componentInstance;
        dotAddToMenuService = TestBed.inject(DotAddToMenuService);
        dotMenuService = TestBed.inject(DotMenuService);

        dotdialog = de.query(By.css('dot-dialog'));
        jest.spyOn(dotMenuService, 'loadMenu');

        fixture.detectChanges();
    });

    it('should have a form', () => {
        const form: DebugElement = de.query(By.css('form'));
        expect(form).not.toBeNull();
        expect(component.form).toEqual(form.componentInstance.form);
    });

    it('should load labels and data when init', () => {
        expect(dotdialog.componentInstance.header).toBe(
            messageServiceMock.get('contenttypes.content.add_to_menu.header')
        );
        expect(
            dotdialog.query(By.css('[data-testId="titleMenuLabel"]')).nativeElement.innerHTML.trim()
        ).toBe(messageServiceMock.get('contenttypes.content.add_to_menu.name'));
        expect(
            dotdialog
                .query(By.css('[data-testId="menuOptionLabel"]'))
                .nativeElement.innerHTML.trim()
        ).toBe(messageServiceMock.get('contenttypes.content.add_to_menu.show_under'));
        expect(
            dotdialog.query(By.css('[data-testId="ViewModeLabel"]')).nativeElement.innerHTML.trim()
        ).toBe(messageServiceMock.get('contenttypes.content.add_to_menu.default_view'));
        expect(
            dotdialog.query(By.css('[data-testId="cardViewMode"]')).componentInstance.label
        ).toBe(messageServiceMock.get('custom.content.portlet.dataViewMode.card'));
        expect(
            dotdialog.query(By.css('[data-testId="listViewMode"]')).componentInstance.label
        ).toBe(messageServiceMock.get('custom.content.portlet.dataViewMode.list'));

        expect(dotdialog.query(By.css('[data-testId="titleMenu"]')).nativeElement.value).toBe(
            contentTypeVar.name
        );
        expect(
            dotdialog.query(By.css('[data-testId="menuOption"]')).componentInstance.options.length
        ).toBe(2);
        expect(
            dotdialog.query(By.css('[data-testId="dotDialogAcceptAction"]')).nativeElement
                .textContent
        ).toBe(messageServiceMock.get('Add'));
        expect(
            dotdialog.query(By.css('[data-testId="dotDialogCancelAction"]')).nativeElement
                .textContent
        ).toBe(messageServiceMock.get('Cancel'));
    });

    it('should load form values when init', () => {
        expect(component.form.get('defaultView').value).toEqual('list');
        expect(component.form.get('menuOption').value).toEqual('123');
        expect(component.form.get('title').value).toEqual(contentTypeVar.name);
        expect(component.form.valid).toEqual(true);
        expect(dotMenuService.loadMenu).toHaveBeenCalledWith(true);
        expect(dotMenuService.loadMenu).toHaveBeenCalledTimes(1);
    });

    it('should invalidate form and set Add button disabled, when name empty', () => {
        component.form.patchValue({
            title: null
        });
        fixture.detectChanges();
        expect(
            dotdialog.query(By.css('[data-testId="dotDialogAcceptAction"]')).nativeElement.disabled
        ).toBe(true);
        expect(component.form.valid).toEqual(false);
    });

    it('should submit form', () => {
        const addButton: DebugElement = dotdialog.query(
            By.css('[data-testId="dotDialogAcceptAction"]')
        );

        jest.spyOn(dotAddToMenuService, 'createCustomTool').mockReturnValue(of(''));
        jest.spyOn(dotAddToMenuService, 'addToLayout').mockReturnValue(of(''));
        jest.spyOn(component.cancel, 'emit');

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

        jest.spyOn(component.cancel, 'emit');
        cancelButton.nativeElement.click();

        expect(component.cancel.emit).toHaveBeenCalledTimes(1);
    });
});
