import { RelationshipsPropertyComponent } from './relationships-property.component';
import { ComponentFixture } from '@angular/core/testing';
import { DebugElement, Component, Input, Output, EventEmitter } from '@angular/core';
import { MockDotMessageService } from 'src/app/test/dot-message-service.mock';
import { DOTTestBed } from 'src/app/test/dot-test-bed';
import { NgControl, FormGroup, FormControl } from '@angular/forms';
import { DotMessageService } from '@services/dot-messages-service';
import { By } from '@angular/platform-browser';

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
    cardinalityIndex: number;

    @Input()
    velocityVar: string;

    @Input()
    editing: boolean;

    @Output()
    change: EventEmitter<any> = new EventEmitter();
}

@Component({
    selector: 'dot-edit-relationships',
    template: ''
})
class TestEditRelationshipsComponent {

    @Output()
    change: EventEmitter<any> = new EventEmitter();
}

fdescribe('RelationshipsPropertyComponent', () => {

    let comp: RelationshipsPropertyComponent;
    let fixture: ComponentFixture<RelationshipsPropertyComponent>;
    let de: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.relationship.existing.label': 'Existing',
        'contenttypes.field.properties.relationship.new.label': 'New',
        'contenttypes.field.properties.relationships.new.error.required': 'New validation error',
        'contenttypes.field.properties.relationships.edit.error.required': 'Edit validation error'
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                RelationshipsPropertyComponent,
                TestFieldValidationMessageComponent,
                TestNewRelationshipsComponent,
                TestEditRelationshipsComponent
            ],
            imports: [],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });

        fixture = DOTTestBed.createComponent(RelationshipsPropertyComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;

        comp.property = {
            name: 'relationship',
            value: {},
            field: {}
        };

        comp.group = new FormGroup({
            'relationship': new FormControl('')
        });
    });

    fdescribe('not editing mode', () => {
        it('should have existing and new radio button', () => {
            fixture.detectChanges();

            const radios = de.queryAll(By.css('p-radioButton'));

            expect(radios.length).toBe(2);
            expect(radios.map(radio => radio.componentInstance.label)).toEqual(['New', 'Existing']);
        });

        it('should show dot-new-relationships in new state', () => {
            fixture.detectChanges();

            const newRadio = de.query(By.css('p-radioButton[ng-reflect-label="New"]'));
            newRadio.triggerEventHandler('click', {});

            fixture.detectChanges();

            expect(de.query(By.css('dot-new-relationships'))).not.toBeUndefined();
            expect(de.query(By.css('dot-edit-relationships '))).toBeNull();
        });

        it('should show dot-edit-relationships in new state', () => {
            fixture.detectChanges();

            const newRadio = de.query(By.css('p-radioButton[ng-reflect-label="Existing"]'));
            newRadio.triggerEventHandler('click', {});

            fixture.detectChanges();

            expect(de.query(By.css('dot-edit-relationships '))).not.toBeUndefined();
            expect(de.query(By.css('dot-new-relationships'))).toBeNull();
        });
    });

    describe('editing mode', () => {
        beforeEach(() => {
            comp.property = {
                name: 'relationship',
                value: {
                    velocityVar: 'velocityVar'
                },
                field: {}
            };

            comp.group = new FormGroup({
                'relationship': new FormControl(comp.property.value)
            });
        });

        it('should not have existing and new radio buttonand should show dot-new-relationships', () => {
            fixture.detectChanges();

            const radios = de.queryAll(By.css('p-radioButton'));

            expect(radios.length).toBe(0);
            expect(de.query(By.css('dot-new-relationships'))).not.toBeUndefined();
            expect(de.query(By.css('dot-edit-relationships '))).toBeNull();
        });
    });
});
