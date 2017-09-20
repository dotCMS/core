import { async, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { ContentTypesFieldDragabbleItemComponent } from './content-type-field-dragabble-item.component';
import { By } from '@angular/platform-browser';
import { Field } from '../';
import { IconButtonTooltipModule } from '../../../../view/components/_common/icon-button-tooltip/icon-button-tooltip.module';
import { MockMessageService } from '../../../../test/message-service.mock';
import { MessageService } from '../../../../api/services/messages-service';
import { FieldService } from '../service';

describe('ContentTypesFieldDragabbleItemComponent', () => {
    let comp: ContentTypesFieldDragabbleItemComponent;
    let fixture: ComponentFixture<ContentTypesFieldDragabbleItemComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    const messageServiceMock = new MockMessageService({
        'contenttypes.action.edit': 'Edit',
        'contenttypes.action.delete': 'Delete'
    });

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                declarations: [ContentTypesFieldDragabbleItemComponent],
                imports: [IconButtonTooltipModule],
                providers: [
                    { provide: MessageService, useValue: messageServiceMock },
                    FieldService
                ]
            });

            fixture = DOTTestBed.createComponent(ContentTypesFieldDragabbleItemComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            el = de.nativeElement;
        })
    );

    it('should have a name', () => {
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

        const span = de.query(By.css('.field__name'));
        expect(span).not.toBeNull();
        expect(span.nativeElement.textContent).toEqual(field.name);
    });

    it(
        'should has a remove button',
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
            expect(button).not.toBeNull();
            expect(button.attributes['icon']).toEqual('fa-trash');

            let resp: Field;
            comp.remove.subscribe(field => (resp = field));
            button.nativeElement.click();

            tick();

            expect(resp).toEqual(field);
        })
    );

    it(
        'should has a edit button',
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

            const button = de.query(By.css('.field__actions-edit'));
            expect(button).not.toBeNull();
            expect(button.attributes['icon']).toEqual('fa-edit');

            let resp: Field;
            comp.edit.subscribe(field => (resp = field));
            button.nativeElement.click();

            tick();

            expect(resp).toEqual(field);
        })
    );
});
