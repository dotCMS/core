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
import { FormGroup } from '@angular/forms';
import { mockFieldType } from '@portlets/shared/dot-content-types-edit/components/fields/service/field.service.spec';
import { FieldType } from '@portlets/shared/dot-content-types-edit/components/fields';

@Component({
    selector: 'dot-test',
    template: '<h1>Testing</h1>'
})
class TestComponent {
    property: FieldProperty;
    group: FormGroup;
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

describe('Directive: DynamicFieldPropertyDirective', () => {
    beforeEach(() => {
        this.component = new TestComponent();

        this.propertyName = 'name';
        this.group = new FormGroup({});
        this.field = {
            name: 'FieldName',
            clazz: 'testClazz'
        };

        const viewContainerRef = new TestViewContainerRef();
        const resolver = new TestComponentFactoryResolver();
        const fieldPropertyService = new TestFieldPropertyService();
        this.componentFactory = {};

        this.getComponent = spyOn(fieldPropertyService, 'getComponent').and.returnValue(
            TestComponent
        );
        this.getFieldType = spyOn(fieldPropertyService, 'getFieldType').and.returnValue(
            mockFieldType
        );
        this.resolveComponentFactory = spyOn(resolver, 'resolveComponentFactory').and.returnValue(
            this.componentFactory
        );
        this.createComponent = spyOn(viewContainerRef, 'createComponent').and.returnValue({
            instance: this.component
        });

        const dynamicFieldPropertyDirective = new DynamicFieldPropertyDirective(
            <ViewContainerRef>viewContainerRef,
            <ComponentFactoryResolver>resolver,
            <FieldPropertyService>fieldPropertyService
        );

        dynamicFieldPropertyDirective.propertyName = this.propertyName;
        dynamicFieldPropertyDirective.field = this.field;
        dynamicFieldPropertyDirective.group = this.group;

        dynamicFieldPropertyDirective.ngOnChanges({
            field: new SimpleChange(null, this.field, true)
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
            value: 'FieldName'
        });
        expect(this.component.group).toEqual(this.group);
        expect(this.component.helpText).toEqual('helpText');
    });
});
