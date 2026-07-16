/**
 * Interface for custom field configuration options
 */
export interface CustomFieldConfig {
    showAsModal: boolean;
    width: string;
    height: string;
}

/**
 * Partial type for custom field options from field variables
 */
export type CustomFieldOptions = Partial<CustomFieldConfig>;

/**
 * Type for individual field variable keys used in custom field configuration
 */
export type CustomFieldVariableKeys = 'showAsModal' | 'width' | 'height';
