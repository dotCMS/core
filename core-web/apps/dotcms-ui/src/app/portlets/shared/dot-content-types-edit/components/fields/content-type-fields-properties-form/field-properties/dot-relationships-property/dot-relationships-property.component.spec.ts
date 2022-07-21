/* eslint-disable @typescript-eslint/no-explicit-any */

import { DotRelationshipsPropertyComponent } from './dot-relationships-property.component';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { DebugElement, Component, Input, Output, EventEmitter } from '@angular/core';
import { MockDotMessageService } from '@dotcms/app/test/dot-message-service.mock';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { NgControl, UntypedFormGroup, UntypedFormControl } from '@angular/forms';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { By } from '@angular/platform-browser';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';

@Component({
    selector: 'dot-field-validation-message',
    template: ''
})
class TestFieldValidationMessageComponent {
    @Input()
    field: NgControl;
    @Input()
    message: string;
}

@Component({
    selector: 'dot-new-relationships',
    template: ''
})
class TestNewRelationshipsComponent {
    @Input()
    cardinality: number;

    @Input()
    velocityVar: string;

    @Input()
    editing: boolean;

    @Output()
    switch: EventEmitter<any> = new EventEmitter();
}

@Component({
    selector: 'dot-edit-relationships',
    template: ''
})
class TestEditRelationshipsComponent {
    @Output()
    switch: EventEmitter<any> = new EventEmitter();
}

describe('DotRelationshipsPropertyComponent', () => {
    let comp: DotRelationshipsPropertyComponent;
    let fixture: ComponentFixture<DotRelationshipsPropertyComponent>;
    let de: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.relationship.existing.label': 'Existing',
        'contenttypes.field.properties.relationship.new.label': 'New',
        'contenttypes.field.properties.relationships.new.error.required': 'New validation error',
        'contenttypes.field.properties.relationships.edit.error.required': 'Edit validation error'
    });

    beforeEach(
        waitForAsync(() => {
            DOTTestBed.configureTestingModule({
                declarations: [
                    DotRelationshipsPropertyComponent,
                    TestFieldValidationMessageComponent,
                    TestNewRelationshipsComponent,
                    TestEditRelationshipsComponent
                ],
                imports: [],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            });

            fixture = DOTTestBed.createComponent(DotRelationshipsPropertyComponent);
            de = fixture.debugElement;
            comp = fixture.componentInstance;

            comp.property = {
                name: 'relationship',
                value: {},
                field: {
                    ...dotcmsContentTypeFieldBasicMock
                }
            };

            comp.group = new UntypedFormGroup({
                relationship: new UntypedFormControl('')
            });
        })
    );

    describe('not editing mode', () => {
        beforeEach(() => {
            fixture.detectChanges();
        });

        it('should have existing and new radio button', () => {
            const radios = de.queryAll(By.css('p-radioButton'));

            expect(radios.length).toBe(2);
            expect(radios.map((radio) => radio.componentInstance.label)).toEqual([
                'New',
                'Existing'
            ]);
        });

        it('should show dot-new-relationships in new state', () => {
            const newRadio = de.query(By.css('.relationships__new'));
            newRadio.triggerEventHandler('click', {});

            fixture.detectChanges();

            expect(de.query(By.css('dot-new-relationships'))).toBeDefined();
            expect(de.query(By.css('dot-edit-relationships'))).toBeNull();
        });

        it('should show dot-edit-relationships in existing state', () => {
            comp.status = 'EXISTING';

            fixture.detectChanges();

            expect(de.query(By.css('dot-edit-relationships'))).toBeDefined();
            expect(de.query(By.css('dot-new-relationships'))).toBeNull();
        });

        it('should clean the relationships property value', () => {
            comp.group.setValue({
                relationship: new UntypedFormControl({
                    velocityVar: 'velocityVar'
                })
            });

            const radio = de.query(By.css('p-radioButton'));
            radio.triggerEventHandler('click', {});

            expect(comp.group.get('relationship').value).toEqual('');
        });
    });

    describe('editing mode', () => {
        beforeEach(() => {
            comp.property = {
                name: 'relationship',
                value: {
                    velocityVar: 'velocityVar',
                    cardinality: 1
                },
                field: {
                    ...dotcmsContentTypeFieldBasicMock
                }
            };

            comp.group = new UntypedFormGroup({
                relationship: new UntypedFormControl(comp.property.value)
            });
        });

        it('should not have existing and new radio buttonand should show dot-new-relationships', () => {
            fixture.detectChanges();

            const radios = de.queryAll(By.css('p-radioButton'));

            const dotNewRelationships = de.query(By.css('dot-new-relationships'));

            expect(radios.length).toBe(0);
            expect(dotNewRelationships).toBeDefined();
            expect(de.query(By.css('dot-edit-relationships'))).toBeNull();

            expect(dotNewRelationships.componentInstance.velocityVar).toEqual('velocityVar');
            expect(dotNewRelationships.componentInstance.cardinality).toEqual(1);
        });

        describe('with inverse relationship', () => {
            it('should not have existing and new radio buttonand should show dot-new-relationships', () => {
                comp.property.value.velocityVar = 'contentType.fieldName';

                fixture.detectChanges();

                const radios = de.queryAll(By.css('p-radioButton'));
                const dotNewRelationships = de.query(By.css('dot-new-relationships'));

                expect(radios.length).toBe(0);
                expect(dotNewRelationships).toBeDefined();
                expect(de.query(By.css('dot-edit-relationships'))).toBeNull();

                expect(dotNewRelationships.componentInstance.velocityVar).toEqual(
                    'contentType.fieldName'
                );
                expect(dotNewRelationships.componentInstance.cardinality).toEqual(1);
            });
        });
    });
});
