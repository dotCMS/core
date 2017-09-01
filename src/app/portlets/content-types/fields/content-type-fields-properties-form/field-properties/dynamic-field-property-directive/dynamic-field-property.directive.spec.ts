import { Component, DebugElement, Type, SimpleChange, ViewContainerRef, ComponentFactoryResolver } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { DOTTestBed } from '../../../../../../test/dot-test-bed';
import { DynamicFieldPropertyDirective } from './dynamic-field-property.directive';
import { FieldPropertyService } from '../../../service';
import { FieldProperty } from '../field-properties.model';
import { FormGroup } from '@angular/forms';
import { Field } from '../../../';
import { By } from '@angular/platform-browser';

@Component({
    selector: 'test',
    template: '<h1>Testing</h1>'
})
class TestComponent {
    property: FieldProperty;
    group: FormGroup;
}

class TestFieldPropertyService {
    getComponent(propertyName: string): Type<any> {
        return null;
    }
}

class TestComponentFactoryResolver {
    resolveComponentFactory<T>(component: Type<T>): any {}
}

class TestViewContainerRef {
    // tslint:disable-next-line:max-line-length
    createComponent<C>(componentFactory: any, index?: number, injector?: any, projectableNodes?: any[][], ngModule?: any): any {}
}


describe('Directive: DynamicFieldPropertyDirective', () => {

    beforeEach(() => {
        this.component = new TestComponent();

        this.propertyName = 'name';
        this.group = new FormGroup({});
        this.field = {
            name: 'FieldName'
        };

        const viewContainerRef = new TestViewContainerRef();
        const resolver = new TestComponentFactoryResolver();
        const fieldPropertyService = new TestFieldPropertyService();
        this.componentFactory = {};

        this.getComponent = spyOn(fieldPropertyService, 'getComponent').and.returnValue(TestComponent);
        this.resolveComponentFactory = spyOn(resolver, 'resolveComponentFactory').and.returnValue(this.componentFactory);
        this.createComponent = spyOn(viewContainerRef, 'createComponent').and.returnValue({
            instance: this.component
        });

        const dynamicFieldPropertyDirective = new DynamicFieldPropertyDirective(<ViewContainerRef> viewContainerRef,
            <ComponentFactoryResolver> resolver, <FieldPropertyService> fieldPropertyService);

        dynamicFieldPropertyDirective.propertyName = this.propertyName;
        dynamicFieldPropertyDirective.field = this.field;
        dynamicFieldPropertyDirective.group = this.group;

        dynamicFieldPropertyDirective.ngOnChanges({
            field: new SimpleChange(null, this.field, true),
        });
    });

    it('Should create a element', () => {
        expect(this.getComponent).toHaveBeenCalledWith(this.propertyName);
        expect(this.resolveComponentFactory).toHaveBeenCalledWith(TestComponent);
        expect(this.createComponent).toHaveBeenCalledWith(this.componentFactory);
    });

    it('Should set component properties', () => {
        expect(this.component.property).toEqual({
            field: this.field,
            name: this.propertyName,
            value: 'FieldName',
        });
        expect(this.component.group).toEqual(this.group);
    });
});
