/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import {
    Component,
    Type,
    SimpleChange,
    ViewContainerRef,
    ComponentFactoryResolver
} from '@angular/core';
import { DynamicFieldPropertyDirective } from './dynamic-field-property.directive';
import { FieldPropertyService } from '../../../service';
import { FieldProperty } from '../field-properties.model';
import { UntypedFormGroup } from '@angular/forms';
import { FieldType } from '@portlets/shared/dot-content-types-edit/components/fields';

@Component({
    selector: 'dot-test',
    template: '<h1>Testing</h1>'
})
class TestComponent {
    property: FieldProperty;
    group: UntypedFormGroup;
}

class TestFieldPropertyService {
    getFieldType(_clazz: string): FieldType {
        return null;
    }

    getComponent(_propertyName: string): Type<any> {
        return null;
    }
}

class TestComponentFactoryResolver {
    resolveComponentFactory<T>(_component: Type<T>): any {}
}

class TestViewContainerRef {
    createComponent(
        _componentFactory: any,
        _index?: number,
        _injector?: any,
        _projectableNodes?: any[][],
        _ngModule?: any
    ): any {}
}

let testComponent;
let componentFactory;
let getComponent;
let resolveComponentFactory;
let createComponent;
let propertyName;
let group;
let field;

xdescribe('Directive: DynamicFieldPropertyDirective', () => {
    beforeEach(() => {
        testComponent = new TestComponent();

        propertyName = 'name';
        group = new UntypedFormGroup({});
        field = {
            name: 'FieldName',
            clazz: 'testClazz'
        };

        const viewContainerRef = new TestViewContainerRef();
        const resolver = new TestComponentFactoryResolver();
        const fieldPropertyService = new TestFieldPropertyService();
        componentFactory = {};

        getComponent = spyOn(fieldPropertyService, 'getComponent').and.returnValue(TestComponent);
        resolveComponentFactory = spyOn(resolver, 'resolveComponentFactory').and.returnValue(
            componentFactory
        );
        createComponent = spyOn(viewContainerRef, 'createComponent').and.returnValue({
            instance: testComponent
        });

        const dynamicFieldPropertyDirective = new DynamicFieldPropertyDirective(
            <ViewContainerRef>viewContainerRef,
            <ComponentFactoryResolver>resolver,
            <FieldPropertyService>fieldPropertyService
        );

        dynamicFieldPropertyDirective.propertyName = propertyName;
        dynamicFieldPropertyDirective.field = field;
        dynamicFieldPropertyDirective.group = group;

        dynamicFieldPropertyDirective.ngOnChanges({
            field: new SimpleChange(null, field, true)
        });
    });

    it('Should create a element', () => {
        expect(getComponent).toHaveBeenCalledWith(propertyName);
        expect(resolveComponentFactory).toHaveBeenCalledWith(TestComponent);
        expect(createComponent).toHaveBeenCalledWith(componentFactory);
    });

    it('Should set component properties', () => {
        expect(testComponent.property).toEqual({
            field: field,
            name: propertyName,
            value: 'FieldName'
        });
        expect(testComponent.group).toEqual(group);
        expect(testComponent.helpText).toEqual('helpText');
    });
});
