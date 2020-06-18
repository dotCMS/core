import { Component, Input, Output, EventEmitter, Injectable, DebugElement } from '@angular/core';
import { DOTTestBed } from 'src/app/test/dot-test-bed';
import { DotCardinalitySelectorComponent } from './dot-cardinality-selector.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from 'src/app/test/dot-message-service.mock';
import { DotRelationshipCardinality } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/model/dot-relationship-cardinality.model';
import { Observable, of } from 'rxjs';
import { DotRelationshipService } from '@portlets/shared/dot-content-types-edit/components/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/services/dot-relationship.service';
import { ComponentFixture, async } from '@angular/core/testing';
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
    template: `<dot-cardinality-selector [value] = "cardinalityIndex"
                                         [disabled] = "disabled">
               </dot-cardinality-selector>`
})
class HostTestComponent {
    @Input()
    cardinalityIndex: number;

    @Input()
    disabled: boolean;

    @Output()
    change: EventEmitter<DotRelationshipCardinality> = new EventEmitter();
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

    const messageServiceMock = new MockDotMessageService({
        'contenttypes.field.properties.relationship.cardinality.placeholder': 'Select Cardinality',
    });

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                DotCardinalitySelectorComponent,
                HostTestComponent
            ],
            imports: [],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotRelationshipService, useClass: MockRelationshipService }
            ]
        });

        fixtureHostComponent = DOTTestBed.createComponent(HostTestComponent);
        de = fixtureHostComponent.debugElement.query(By.css('dot-cardinality-selector'));
        comp = de.componentInstance;
    }));

    it('should have a p-dropdown with right attributes', () => {
        fixtureHostComponent.detectChanges();

        const dropdown = de.query(By.css('p-dropdown'));

        expect(dropdown).toBeDefined();

        expect(dropdown.componentInstance.appendTo).toBe('body');
        expect(dropdown.componentInstance.optionLabel).toBe('label');
    });

    it('should disabled p-dropdown', () => {
        fixtureHostComponent.componentInstance.disabled = true;
        fixtureHostComponent.detectChanges();

        const dropdown = de.query(By.css('p-dropdown'));

        expect(dropdown.componentInstance.disabled).toBe(true);
    });

    it('should load cardinalities', () => {
        fixtureHostComponent.componentInstance.disabled = true;
        fixtureHostComponent.detectChanges();

        const  dropdown = de.query(By.css('p-dropdown'));

        expect(comp.options).toEqual(cardinalities);
        expect(dropdown.componentInstance.options.map(option => option.value))
            .toEqual(cardinalities);
    });

    it('should trigger a change event p-dropdown', (done) => {
        fixtureHostComponent.detectChanges();

        comp.change.subscribe((change) => {
            expect(change).toEqual(cardinalities[1].id);
            done();
        });

        const dropdown = de.query(By.css('p-dropdown'));
        dropdown.triggerEventHandler('onChange', {
            value: cardinalities[1]
        });
    });
});
