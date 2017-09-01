import { Injectable, ComponentFactory, Type } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { RequestMethod } from '@angular/http';
import { CoreWebService } from '../../../../api/services/core-web-service';
import { PROPERTY_INFO } from './field-property-info';
import { DATA_TYPE_PROPERTY_INFO } from './data-type-property-info';
import { ValidationErrors } from '@angular/forms';
import { FieldService } from './field.service';

/**
 * Provide method to handle with the Field Types's properties
 */
@Injectable()
export class FieldPropertyService {
    private fieldTypesProperties = new Map<string, string[]>();

    constructor(fieldService: FieldService) {
        fieldService.loadFieldTypes().subscribe(fieldTypes => {
            fieldTypes.forEach(fieldType => {
                this.fieldTypesProperties.set(fieldType.clazz, fieldType.properties);
            });
        });
    }

    /**
     * Return true is a property has a Componente, otherwise return false
     * @param {string} propertyName
     * @returns {boolean}
     * @memberof FieldPropertyService
     */
    existsComponent(propertyName: string): boolean {
        return PROPERTY_INFO[propertyName] ? true : false;
    }

    /**
     * Return the component linked whit propertyName
     * @param {string} propertyName
     * @returns {Type<any>}
     * @memberof FieldPropertyService
     */
    getComponent(propertyName: string): Type<any> {
        return PROPERTY_INFO[propertyName] ? PROPERTY_INFO[propertyName].component : null;
    }

    /**
     * Return a properties's default value
     * @param {string} propertyName
     * @param {string} [fieldTypeClass] Field's class, it define the Field's type
     * @returns {*} default value
     * @memberof FieldPropertyService
     */
    getDefaultValue(propertyName: string, fieldTypeClass?: string): any {
        if (propertyName === 'dataType') {
            return DATA_TYPE_PROPERTY_INFO[fieldTypeClass][0].value;
        } else {
            return PROPERTY_INFO[propertyName] ? PROPERTY_INFO[propertyName].defaultValue : null;
        }
    }

    /**
     * Return the order in which a property must be display
     * @param {string} propertyName
     * @returns {*} property's order
     * @memberof FieldPropertyService
     */
    getOrder(propertyName: string): any {
        return PROPERTY_INFO[propertyName] ? PROPERTY_INFO[propertyName].order : null;
    }

    /**
     * Return the Validations for a property, this has to be ValidationError objects.
     * to see more abour ValidationError: https://angular.io/guide/form-validation
     * @param {string} propertyName
     * @returns {ValidationErrors[]}
     * @memberof FieldPropertyService
     */
    getValidations(propertyName: string): ValidationErrors[] {
        return PROPERTY_INFO[propertyName] ? PROPERTY_INFO[propertyName].validations || [] : [];
    }

    /**
     * Return true if a property have to been disable in edit mode, in otherwise return null
     * @param {string} propertyName
     * @returns {boolean} true if the property have to been disable in edit mode
     * @memberof FieldPropertyService
     */
    isDisabledInEditMode(propertyName: string): boolean {
        return PROPERTY_INFO[propertyName] ? PROPERTY_INFO[propertyName].disabledInEdit : null;
    }

    /**
     * Return the properties's name for a specific field type
     * @param {string} fieldTypeClass Field type's class
     * @returns {string[]} properties's name
     * @memberof FieldPropertyService
     */
    getProperties(fieldTypeClass: string): string[] {
        return this.fieldTypesProperties.get(fieldTypeClass);
    }

    /**
     * Return the allow values for the data types's property
     * @param {string} fieldTypeClass Field type's class
     * @returns {string[]} Allow data types values
     * @memberof FieldPropertyService
     */
    getDataTypeValues(fieldTypeClass: string): string[] {
        return DATA_TYPE_PROPERTY_INFO[fieldTypeClass];
    }
}
