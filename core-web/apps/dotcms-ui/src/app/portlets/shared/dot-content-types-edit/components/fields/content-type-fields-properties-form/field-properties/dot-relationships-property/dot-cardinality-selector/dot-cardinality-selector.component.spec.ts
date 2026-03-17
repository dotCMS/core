import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { Observable, of } from 'rxjs';

import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCardinalitySelectorComponent } from './dot-cardinality-selector.component';

import { DotRelationshipCardinality } from '../model/dot-relationship-cardinality.model';
import { DotRelationshipService } from '../services/dot-relationship.service';

const cardinalities: DotRelationshipCardinality[] = [
    { label: 'Many to many', id: 0, name: 'MANY_TO_MANY' },
    { label: 'One to one', id: 1, name: 'ONE_TO_ONE' }
];

class MockRelationshipService {
    loadCardinalities(): Observable<DotRelationshipCardinality[]> {
        return of(cardinalities);
    }
}

describe('DotCardinalitySelectorComponent', () => {
    let spectator: Spectator<DotCardinalitySelectorComponent>;

    const createComponent = createComponentFactory({
        component: DotCardinalitySelectorComponent,
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            { provide: DotRelationshipService, useClass: MockRelationshipService }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: { value: 0, disabled: false },
            detectChanges: true
        });
    });

    function getDropdown() {
        return spectator.debugElement.query(By.css('[data-testId="dropdown"]'));
    }

    it('should have a p-select with right attributes', () => {
        const dropdown = getDropdown();
        expect(dropdown).toBeTruthy();
        expect(dropdown.attributes['appendto'] ?? dropdown.attributes['appendTo']).toBe('body');
    });

    it('should disabled p-select when disabled input is true', () => {
        spectator.setInput('disabled', true);
        spectator.detectChanges();

        const dropdown = getDropdown();
        const disabled = dropdown.componentInstance.disabled;
        const disabledValue = typeof disabled === 'function' ? disabled() : disabled;
        expect(disabledValue).toBe(true);
    });

    it('should load cardinalities', () => {
        const dropdown = getDropdown();
        const options = dropdown.componentInstance.options;
        expect(options).toEqual(cardinalities);
    });

    it('should trigger a change event on p-select', (done) => {
        spectator.component.switch.subscribe((change) => {
            expect(change).toEqual(cardinalities[1].id);
            done();
        });

        getDropdown().triggerEventHandler('onChange', {
            value: cardinalities[1].id
        });
    });
});
