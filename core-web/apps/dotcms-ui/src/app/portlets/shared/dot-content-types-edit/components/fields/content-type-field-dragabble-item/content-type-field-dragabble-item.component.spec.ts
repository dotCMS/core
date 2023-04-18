import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { TooltipModule } from 'primeng/tooltip';

import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotCopyLinkModule } from '@components/dot-copy-link/dot-copy-link.module';
import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotMessageService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotIconModule } from '@dotcms/ui';
import { dotcmsContentTypeFieldBasicMock, MockDotMessageService } from '@dotcms/utils-testing';

import { ContentTypesFieldDragabbleItemComponent } from './content-type-field-dragabble-item.component';

import { FieldService } from '../service';

/**
 * @description This function resizes the iframe that karma uses for testing
 * @param context This targets the iframe that karma uses to run tests, this can change depending on the test runner config
 * @param width
 * @param height
 */
function resize(context: HTMLIFrameElement, width: string, height: string = '100%') {
    context.style.width = width;
    context.style.height = height;

    // This propagates the styles to all the document in the iframe
    context.contentDocument.body.getBoundingClientRect();
}

describe('ContentTypesFieldDragabbleItemComponent', () => {
    let comp: ContentTypesFieldDragabbleItemComponent;
    let fixture: ComponentFixture<ContentTypesFieldDragabbleItemComponent>;
    let de: DebugElement;
    let context: HTMLIFrameElement;

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
                UiDotIconButtonTooltipModule,
                DotIconModule,
                DotCopyLinkModule,
                HttpClientTestingModule,
                DotMessagePipeModule,
                ChipModule,
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

        // This targets the iframe that karma uses to run tests
        context = window.parent.document.querySelector('iframe');

        // Be sure that the window gets resized to 100% before running the tests
        resize(context, '100%');
    }));

    afterAll(() => {
        // This resets the size of the iframe to the original size
        resize(context, '100%', '100%');
    });

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

        const container = de.query(By.css('.field__name'));
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

        const attrs = ['Required', 'Indexed', 'Show on list'];

        comp.field = field;

        fixture.detectChanges();

        const attrsChips = de.queryAll(By.css('p-chip')).map((e) => e.nativeElement.textContent);
        expect(attrsChips).toEqual(attrs);
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

        const button = de.query(By.css('#field__actions-delete'));
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

        const button = de.query(By.css('#field__actions-delete'));
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

    it('should not have primeng down button on default', () => {
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
        expect(de.query(By.css('.pi.pi-angle-down'))).toBeFalsy();
    });

    it('should set small to true on resizing', (done) => {
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

        resize(context, '250px');

        fixture.detectChanges();

        setTimeout(() => {
            expect(comp.small).toBe(true);
            done();
        }, 1000);
    });

    it('should have primeng down button when small', (done) => {
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

        resize(context, '250px');

        fixture.detectChanges();

        setTimeout(() => {
            expect(de.query(By.css('.pi.pi-angle-down'))).toBeTruthy();
            done();
        }, 1000);
    });
});
