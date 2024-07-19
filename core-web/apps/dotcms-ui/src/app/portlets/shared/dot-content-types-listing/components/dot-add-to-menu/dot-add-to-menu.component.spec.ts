import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { RadioButtonModule } from 'primeng/radiobutton';

import { DotMenuServiceMock } from '@components/dot-navigation/services/dot-navigation.service.spec';
import {
    DotAddToMenuService,
    DotCreateCustomTool
} from '@dotcms/app/api/services/add-to-menu/add-to-menu.service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { DotMessageService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import {
    DotDialogModule,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import {
    CoreWebServiceMock,
    dotcmsContentTypeBasicMock,
    MockDotMessageService
} from '@dotcms/utils-testing';
import { DotFormSelectorModule } from '@portlets/dot-edit-page/content/components/dot-form-selector/dot-form-selector.module';

import { DotAddToMenuComponent } from './dot-add-to-menu.component';

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
    `
})
class TestHostComponent {
    contentType = contentTypeVar;
}

export class DotAddToMenuServiceMock {
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
            declarations: [DotAddToMenuComponent, TestHostComponent],
            imports: [
                BrowserAnimationsModule,
                DotFormSelectorModule,
                DotDialogModule,
                DropdownModule,
                InputTextModule,
                ButtonModule,
                RadioButtonModule,
                ReactiveFormsModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                HttpClientTestingModule,
                DotFieldValidationMessageComponent
            ],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotAddToMenuService, useClass: DotAddToMenuServiceMock },
                { provide: DotMenuService, useClass: DotMenuServiceMock }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-add-to-menu'));
        component = de.componentInstance;
        dotAddToMenuService = TestBed.inject(DotAddToMenuService);
        dotMenuService = TestBed.inject(DotMenuService);

        dotdialog = de.query(By.css('dot-dialog'));
        spyOn(dotMenuService, 'loadMenu').and.callThrough();

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
            dotdialog.query(By.css('[data-testId="dotDialogAcceptAction"]')).nativeElement.innerText
        ).toBe(messageServiceMock.get('Add'));
        expect(
            dotdialog.query(By.css('[data-testId="dotDialogCancelAction"]')).nativeElement.innerText
        ).toBe(messageServiceMock.get('Cancel'));
    });

    it('should load form values when init', () => {
        expect(component.form.get('defaultView').value).toEqual('list');
        expect(component.form.get('menuOption').value).toEqual('123');
        expect(component.form.get('title').value).toEqual(contentTypeVar.name);
        expect(component.form.valid).toEqual(true);
        expect(dotMenuService.loadMenu).toHaveBeenCalledWith(true);
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

        spyOn(dotAddToMenuService, 'createCustomTool').and.returnValue(of(''));
        spyOn(dotAddToMenuService, 'addToLayout').and.returnValue(of(''));
        spyOn(component.cancel, 'emit');

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

        spyOn(component.cancel, 'emit');
        cancelButton.nativeElement.click();

        expect(component.cancel.emit).toHaveBeenCalledTimes(1);
    });
});
