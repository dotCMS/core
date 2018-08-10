import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { ContentTypesFieldDragabbleItemComponent } from './content-type-field-dragabble-item.component';
import { By } from '@angular/platform-browser';
import { ContentTypeField } from '../';
import { IconButtonTooltipModule } from '../../../../view/components/_common/icon-button-tooltip/icon-button-tooltip.module';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { FieldService } from '../service';
import { DotIconModule } from '../../../../view/components/_common/dot-icon/dot-icon.module';

describe('ContentTypesFieldDragabbleItemComponent', () => {
    let comp: ContentTypesFieldDragabbleItemComponent;
    let fixture: ComponentFixture<ContentTypesFieldDragabbleItemComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.action.edit': 'Edit',
        'contenttypes.action.delete': 'Delete',
        'contenttypes.field.atributes.required': 'Required',
        'contenttypes.field.atributes.indexed': 'Indexed',
        'contenttypes.field.atributes.listed': 'Show on list'
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [ContentTypesFieldDragabbleItemComponent],
            imports: [IconButtonTooltipModule, DotIconModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }, FieldService]
        });

        fixture = DOTTestBed.createComponent(ContentTypesFieldDragabbleItemComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;
        el = de.nativeElement;
    }));

    it('should have a name & variable', () => {
        const field = {
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
        expect(container.nativeElement.textContent.trim()).toEqual(`${field.name} (${field.variable})`);
    });

    it('should have field attributes label', () => {
        const field = {
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

        const container = de.query(By.css('.field__attribute'));
        expect(container).not.toBeNull();
        expect(container.nativeElement.textContent).toEqual(
            'Required\u00A0\u00A0•\u00A0\u00A0Indexed\u00A0\u00A0•\u00A0\u00A0Show on list'
        );
    });

    it(
        'should has a remove button',
        fakeAsync(() => {
            const field = {
                fieldType: 'fieldType',
                fixed: false,
                indexed: true,
                name: 'Field name',
                required: true,
                velocityVarName: 'velocityName'
            };

            comp.field = field;

            fixture.detectChanges();

            const button = de.query(By.css('.field__actions-delete'));
            expect(button).not.toBeNull();
            expect(button.attributes['icon']).toEqual('fa-trash');

            let resp: ContentTypeField;
            comp.remove.subscribe((fieldItem) => (resp = fieldItem));
            button.nativeElement.click();

            tick();

            expect(resp).toEqual(field);
        })
    );

    it(
        'should not has a remove button (Fixed Field)',
        fakeAsync(() => {
            const field = {
                fieldType: 'fieldType',
                fixed: true,
                indexed: true,
                name: 'Field name',
                required: true,
                velocityVarName: 'velocityName'
            };

            comp.field = field;

            fixture.detectChanges();

            const button = de.query(By.css('.field__actions-delete'));
            expect(button).toBeNull();
        })
    );

    it(
        'should has a edit button',
        fakeAsync(() => {
            const mockField = {
                fieldType: 'fieldType',
                fixed: true,
                indexed: true,
                name: 'Field name',
                required: true,
                velocityVarName: 'velocityName'
            };

            comp.field = mockField;

            fixture.detectChanges();

            const button = de.query(By.css('.field__actions-edit'));
            expect(button).not.toBeNull();
            expect(button.attributes['icon']).toEqual('fa-edit');

            let resp: ContentTypeField;
            comp.edit.subscribe((field) => (resp = field));
            button.nativeElement.click();

            tick();

            expect(resp).toEqual(mockField);
        })
    );

    it('should edit field on host click', () => {
        const mockField = {
            fieldType: 'fieldType',
            fixed: true,
            indexed: true,
            name: 'Field name',
            required: true,
            velocityVarName: 'velocityName'
        };

        comp.field = mockField;

        fixture.detectChanges();

        let resp: ContentTypeField;
        comp.edit.subscribe((field) => (resp = field));

        de.nativeElement.click();

        expect(resp).toEqual(mockField);
    });
});
