import { Injectable, Type, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ValidationErrors } from '@angular/forms';

import { map } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotDynamicFieldComponent,
    DotRenderModes,
    NEW_RENDER_MODE_VARIABLE_KEY,
    FeaturedFlags,
    FEATURE_FLAG_NOT_FOUND
} from '@dotcms/dotcms-models';

import { DATA_TYPE_PROPERTY_INFO } from './data-type-property-info';
import { PROPERTY_INFO } from './field-property-info';
import { FieldService } from './field.service';

import { FieldType } from '../models';

/**
 * Provide method to handle with the Field Types's properties
 */
@Injectable()
export class FieldPropertyService {
    /**
     * Map of field types keyed by their class name
     * @private
     */
    private fieldTypes = new Map<string, FieldType>();

    /**
     * Service for accessing dotCMS properties and feature flags
     * @private
     */
    private dotPropertiesService = inject(DotPropertiesService);

    /**
     * Signal containing the default render mode for new fields
     * Reads from feature flag or defaults to IFRAME mode
     * @readonly
     */
    readonly $newRenderModeDefault = toSignal(
        this.dotPropertiesService
            .getKey(FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_RENDER_MODE_DEFAULT)
            .pipe(
                map((value) => (value === FEATURE_FLAG_NOT_FOUND ? DotRenderModes.IFRAME : value))
            ),
        { initialValue: DotRenderModes.IFRAME }
    );

    /**
     * Initializes the service by loading field types from FieldService
     * and populating the fieldTypes map with enhanced properties for custom fields
     */
    constructor() {
        const fieldService = inject(FieldService);

        fieldService.loadFieldTypes().subscribe((fieldTypes) => {
            fieldTypes
                .map((fieldType) => {
                    if (fieldType.clazz === DotCMSClazzes.CUSTOM_FIELD) {
                        return {
                            ...fieldType,
                            properties: [...fieldType.properties, NEW_RENDER_MODE_VARIABLE_KEY]
                        };
                    }
                    return fieldType;
                })
                .forEach((fieldType) => {
                    this.fieldTypes.set(fieldType.clazz, fieldType);
                });
        });
    }

    /**
     * Checks if a property has an associated component
     * @param propertyName - The name of the property to check
     * @returns True if the property has a component, false otherwise
     */
    existsComponent(propertyName: string): boolean {
        return !!PROPERTY_INFO[propertyName];
    }

    /**
     * Gets the component type associated with a property name
     * @param propertyName - The name of the property
     * @returns The component type for the property, or null if not found
     */
    getComponent(propertyName: string): Type<DotDynamicFieldComponent> {
        return PROPERTY_INFO[propertyName] ? PROPERTY_INFO[propertyName].component : null;
    }

    /**
     * Gets the default value for a property
     * For dataType property, returns the default based on field type class
     * For other properties, returns the default from property info
     * @param propertyName - The name of the property
     * @param fieldTypeClass - Optional field type class (required for dataType property)
     * @returns The default value for the property, or null if not found
     */
    getDefaultValue(propertyName: string, fieldTypeClass?: string): unknown {
        return propertyName === 'dataType'
            ? this.getDataType(fieldTypeClass)
            : this.getPropInfo(propertyName);
    }

    /**
     * Gets the value of a property for a specific field
     * Handles special case for newRenderMode property which reads from field variables
     * @param field - The content type field to get the value from
     * @param propertyName - The name of the property to retrieve
     * @returns The property value, or the default render mode for newRenderMode if not set
     */
    getValue(field: DotCMSContentTypeField, propertyName: string): unknown {
        if (propertyName === NEW_RENDER_MODE_VARIABLE_KEY) {
            const fieldVariable = field?.fieldVariables?.find(
                (variable) => variable.key === NEW_RENDER_MODE_VARIABLE_KEY
            );
            return fieldVariable?.value || this.$newRenderModeDefault();
        }
        return field[propertyName];
    }

    /**
     * Gets the display order for a property
     * @param propertyName - The name of the property
     * @returns The order number for the property, or null if not found
     */
    getOrder(propertyName: string): number {
        return PROPERTY_INFO[propertyName] ? PROPERTY_INFO[propertyName].order : null;
    }

    /**
     * Gets the validation rules for a property
     * Returns an array of Angular validators (ValidationErrors)
     * @param propertyName - The name of the property
     * @returns Array of validation errors, or empty array if no validations are defined
     * @see https://angular.io/guide/form-validation
     */
    getValidations(propertyName: string): ValidationErrors[] {
        return PROPERTY_INFO[propertyName] ? PROPERTY_INFO[propertyName].validations || [] : [];
    }

    /**
     * Checks if a property should be disabled in edit mode
     * @param propertyName - The name of the property to check
     * @returns True if the property should be disabled in edit mode, null if not specified
     */
    isDisabledInEditMode(propertyName: string): boolean {
        return PROPERTY_INFO[propertyName] ? PROPERTY_INFO[propertyName].disabledInEdit : null;
    }

    /**
     * Gets the list of property names for a specific field type
     * @param fieldTypeClass - The field type's class identifier
     * @returns Array of property names for the field type, or undefined if field type not found
     */
    getProperties(fieldTypeClass: string): string[] {
        const fieldType = this.fieldTypes.get(fieldTypeClass);

        return fieldType !== undefined ? fieldType.properties : undefined;
    }

    /**
     * Gets the FieldType object for a specific field type class
     * @param fieldTypeClass - The field type's class identifier
     * @returns The FieldType object, or undefined if not found
     */
    getFieldType(fieldTypeClass: string): FieldType {
        return this.fieldTypes.get(fieldTypeClass);
    }

    /**
     * Gets the allowed values for the dataType property of a specific field type
     * @param fieldTypeClass - The field type's class identifier
     * @returns Array of allowed data type values for the field type
     */
    getDataTypeValues(fieldTypeClass: string): string[] {
        return DATA_TYPE_PROPERTY_INFO[fieldTypeClass];
    }

    /**
     * Gets the default data type value for a field type class
     * @param fieldTypeClass - The field type's class identifier
     * @returns The default data type value, or null if not found
     * @private
     */
    private getDataType(fieldTypeClass: string): unknown {
        return DATA_TYPE_PROPERTY_INFO[fieldTypeClass]
            ? DATA_TYPE_PROPERTY_INFO[fieldTypeClass][0].value
            : null;
    }

    /**
     * Gets the default value for a property from property info
     * @param propertyName - The name of the property
     * @returns The default value from property info, or null if not found
     * @private
     */
    private getPropInfo(propertyName: string): unknown {
        return PROPERTY_INFO[propertyName] ? PROPERTY_INFO[propertyName].defaultValue : null;
    }
}
