import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotIconComponent, DotMessagePipe } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { ContentTypesFieldDragabbleItemComponent } from './content-type-field-dragabble-item.component';

import { DotCopyLinkComponent } from '../../../../../../view/components/dot-copy-link/dot-copy-link.component';
import { FieldService } from '../service';

describe('ContentTypesFieldDragabbleItemComponent', () => {
    let comp: ContentTypesFieldDragabbleItemComponent;
    let fixture: ComponentFixture<ContentTypesFieldDragabbleItemComponent>;
    let de: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.action.edit': 'Edit',
        'contenttypes.action.delete': 'Delete',
        'contenttypes.field.atributes.required': 'Required',
        'contenttypes.field.atributes.indexed': 'Indexed',
        'contenttypes.field.atributes.listed': 'Show on list'
    });

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [ContentTypesFieldDragabbleItemComponent],
            imports: [
                DotIconComponent,
                DotCopyLinkComponent,
                HttpClientTestingModule,
                DotMessagePipe,
                OverlayPanelModule,
                ButtonModule,
                TooltipModule
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: CoreWebService,
                    useClass: CoreWebServiceMock
                },
                FieldService
            ]
        });

        fixture = TestBed.createComponent(ContentTypesFieldDragabbleItemComponent);

        comp = fixture.componentInstance;
        de = fixture.debugElement;
    }));

    it('should have a name & variable', () => {
        const field = {
            ...dotcmsContentTypeFieldBasicMock,
            fieldType: 'fieldType',
            fixed: true,
            indexed: true,
            name: 'Field name',
            required: false,
            variable: 'test',
            velocityVarName: 'velocityName'
        };

        comp.field = field;

        fixture.detectChanges();

        const container = de.query(By.css('.info-container__name'));
        expect(container).not.toBeNull();
        expect(container.nativeElement.textContent.trim().replace('  ', ' ')).toEqual('Field name');
    });

    it('should have copy variable button', () => {
        const field = {
            ...dotcmsContentTypeFieldBasicMock,
            fieldType: 'fieldType',
            fixed: true,
            indexed: true,
            listed: true,
            name: 'Field name',
            required: true,
            variable: 'test',
            velocityVarName: 'velocityName'
        };

        comp.field = field;

        fixture.detectChanges();

        const copyButton: DebugElement = de.query(By.css('dot-copy-link'));
        expect(copyButton.componentInstance.copy).toBe('test');
        expect(copyButton.componentInstance.label).toBe('test');
    });

    it('should have field attributes label', () => {
        const field = {
            ...dotcmsContentTypeFieldBasicMock,
            fieldType: 'fieldType',
            fieldTypeLabel: 'FieldLabel',
            fixed: true,
            indexed: true,
            listed: true,
            name: 'Field name',
            required: true,
            variable: 'test',
            velocityVarName: 'velocityName'
        };
        comp.field = field;

        fixture.detectChanges();
        const attrs = ['FieldLabel', 'Required', 'Indexed', 'Show on list'];

        const attrsString = de.query(
            By.css(
                '.field-properties > .field-properties__actions-container > .field-properties__attributes-container'
            )
        ).nativeElement.textContent;

        expect(attrs.every((attr) => attrsString.includes(attr))).toBe(true);
    });

    it('should has a drag button', () => {
        const field = {
            ...dotcmsContentTypeFieldBasicMock,
            fieldType: 'fieldType',
            fixed: false,
            indexed: true,
            name: 'Field name',
            required: true,
            velocityVarName: 'velocityName'
        };

        comp.field = field;

        fixture.detectChanges();

        const button = de.query(By.css('.field-drag'));
        expect(button).not.toBeNull();
    });

    it('should has a remove button', () => {
        const field = {
            ...dotcmsContentTypeFieldBasicMock,
            fieldType: 'fieldType',
            fixed: false,
            indexed: true,
            name: 'Field name',
            required: true,
            velocityVarName: 'velocityName'
        };

        comp.field = field;

        fixture.detectChanges();

        const button = de.query(By.css('#info-container__delete'));
        expect(button).not.toBeNull();
        expect(button.attributes['icon']).toEqual('pi pi-trash');

        let resp: DotCMSContentTypeField;
        comp.remove.subscribe((fieldItem) => (resp = fieldItem));
        button.triggerEventHandler('click', {
            stopPropagation: () => {
                //
            }
        });

        expect(resp).toEqual(field);
    });

    it('should not has a remove button (Fixed Field)', () => {
        const field = {
            ...dotcmsContentTypeFieldBasicMock,
            fieldType: 'fieldType',
            fixed: true,
            indexed: true,
            name: 'Field name',
            required: true,
            velocityVarName: 'velocityName'
        };

        comp.field = field;

        fixture.detectChanges();

        const button = de.query(By.css('#info-container__delete'));
        expect(button).toBeNull();
    });

    it('should edit field on host click', () => {
        const mockField = {
            ...dotcmsContentTypeFieldBasicMock,
            fieldType: 'fieldType',
            fixed: true,
            indexed: true,
            name: 'Field name',
            required: true,
            velocityVarName: 'velocityName'
        };

        comp.field = mockField;

        fixture.detectChanges();

        let resp: DotCMSContentTypeField;
        comp.edit.subscribe((field) => (resp = field));

        de.triggerEventHandler('click', {
            stopPropagation: () => {
                //
            }
        });

        expect(resp).toEqual(mockField);
    });

    it('should not have info button on default', () => {
        const mockField = {
            ...dotcmsContentTypeFieldBasicMock,
            fieldType: 'fieldType',
            fixed: true,
            indexed: true,
            name: 'Field name',
            required: true,
            velocityVarName: 'velocityName'
        };

        comp.field = mockField;

        fixture.detectChanges();
        expect(de.query(By.css('[data-testid="field-info-button"]'))).toBeFalsy();
    });

    it('should have info button when row has more than one column', () => {
        const mockField = {
            ...dotcmsContentTypeFieldBasicMock,
            fieldType: 'fieldType',
            fixed: true,
            indexed: true,
            name: 'Field name',
            required: true,
            velocityVarName: 'velocityName'
        };

        comp.field = mockField;
        comp.isSmall = true;

        fixture.detectChanges();
        expect(de.query(By.css('[data-testid="field-info-button"]'))).toBeTruthy();
    });
});
