import { Component, Input, Output, EventEmitter, Injectable, DebugElement } from '@angular/core';
import { DotCardinalitySelectorComponent } from './dot-cardinality-selector.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@dotcms/app/test/dot-message-service.mock';
import { DotRelationshipCardinality } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/model/dot-relationship-cardinality.model';
import { Observable, of } from 'rxjs';
import { DotRelationshipService } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/services/dot-relationship.service';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

const cardinalities: DotRelationshipCardinality[] = [
    {
        label: 'Many to many',
        id: 0,
        name: 'MANY_TO_MANY'
    },
    {
        label: 'One to one',
        id: 1,
        name: 'ONE_TO_ONE'
    }
];

@Component({
    selector: 'dot-host-component',
    template: `<dot-cardinality-selector [value]="cardinalityIndex" [disabled]="disabled">
    </dot-cardinality-selector>`
})
class HostTestComponent {
    @Input()
    cardinalityIndex: number;

    @Input()
    disabled: boolean;

    @Output()
    switch: EventEmitter<DotRelationshipCardinality> = new EventEmitter();
}

@Injectable()
class MockRelationshipService {
    loadCardinalities(): Observable<DotRelationshipCardinality[]> {
        return of(cardinalities);
    }
}

describe('DotCardinalitySelectorComponent', () => {
    let fixtureHostComponent: ComponentFixture<HostTestComponent>;
    let comp: DotCardinalitySelectorComponent;
    let de: DebugElement;
    let dropdown: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.relationship.cardinality.placeholder': 'Select Cardinality'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotCardinalitySelectorComponent, HostTestComponent],
            imports: [],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotRelationshipService, useClass: MockRelationshipService }
            ]
        }).compileComponents();

        fixtureHostComponent = TestBed.createComponent(HostTestComponent);
        de = fixtureHostComponent.debugElement.query(By.css('dot-cardinality-selector'));
        comp = de.componentInstance;
        fixtureHostComponent.detectChanges();
        dropdown = de.query(By.css('[data-testId="dropdown"]'));
    });

    it('should have a p-dropdown with right attributes', () => {
        expect(dropdown.attributes.appendTo).toBe('body');
    });

    it('should disabled p-dropdown', () => {
        fixtureHostComponent.componentInstance.disabled = true;
        fixtureHostComponent.detectChanges();
        expect(dropdown.componentInstance.disabled).toBe(true);
    });

    it('should load cardinalities', () => {
        const options: Observable<DotRelationshipCardinality[]> =
            dropdown.componentInstance.options;
        options.subscribe((options) => {
            expect(options).toEqual(cardinalities);
        });
    });

    it('should trigger a change event p-dropdown', (done) => {
        comp.switch.subscribe((change) => {
            expect(change).toEqual(cardinalities[1].id);
            done();
        });

        dropdown.triggerEventHandler('onChange', {
            value: cardinalities[1].id
        });
    });
});
