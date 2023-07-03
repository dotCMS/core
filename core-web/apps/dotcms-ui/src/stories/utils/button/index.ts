const SEVERITY_PLACEHOLDER_REGEX = /SEVERITY_TEMPLATE/g;
const BUTTONS_PLACEHOLDER_REGEX = /BUTTONS_TEMPLATE/g;

const SEVERITY_VALUE_REGEX = /SEVERITY_VALUE/g;
const SIZE_VALUE_REGEX = /SIZE_VALUE/g;
const BUTTON_TYPE_REGEX = /BUTTON_TYPE/g;

const MAIN_DIV_TEMPLATE = `<div style="display: flex; gap: 24px; flex-direction: column; align-items: center;">SEVERITY_TEMPLATE</div>`;
const SIZE_SEPARATOR_TEMPLATE = `<div style="display: flex; gap: 8px; justify-content: center; width: fit-content;">BUTTONS_TEMPLATE</div>`;

export const MAIN_BUTTONS_TEMPLATE = `<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE" pButton label="Button"></button>
<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE" pButton label="Button" icon="pi pi-plus"></button>
<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE" pButton label="Button" icon="pi pi-plus" iconPos="right"></button>
<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE" pButton label="Button" icon="pi pi-plus" disabled="true"></button>
<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE" pButton label="Button" disabled="true"></button>`;

export const ICON_ONLY_BUTTONS_TEMPLATE = `<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE p-button-rounded" pButton icon="pi pi-ellipsis-v"></button>
<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE p-button-rounded" pButton icon="pi pi-ellipsis-v"></button>
<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE p-button-rounded" pButton icon="pi pi-ellipsis-v" disabled="true"></button>`;

// Empty string represents basic buttons (no severity, no size)
const severities = ['', 'p-button-secondary', 'p-button-danger'];
const sizes = ['p-button-lg', '', 'p-button-sm'];

/**
 * Creates a button template with the given button type, template and severity quantity.
 *
 * @export
 * @param {string} [buttonType='']
 * @param {string} [buttonTemplate=MAIN_BUTTONS_TEMPLATE]
 * @param {number} [severityQuantity=3]
 * @return {*}  {string}
 */
export function createButtonTemplate(
    buttonType: string = '',
    buttonTemplate: string = MAIN_BUTTONS_TEMPLATE,
    severityQuantity: number = 3
): string {
    // All buttons separated by severity
    const buttonsBySeverity = severities
        .slice(0, severityQuantity)
        .map((severity) => {
            // All buttons separated by size but with the same severity
            return sizes
                .map((size) => {
                    const button = buttonTemplate
                        .replace(SEVERITY_VALUE_REGEX, severity)
                        .replace(SIZE_VALUE_REGEX, size)
                        .replace(BUTTON_TYPE_REGEX, buttonType);

                    // We insert the button group in the size template
                    return SIZE_SEPARATOR_TEMPLATE.replace(BUTTONS_PLACEHOLDER_REGEX, button);
                })
                .join('');
        })
        .join('');

    // All buttons inside the main div
    return MAIN_DIV_TEMPLATE.replace(SEVERITY_PLACEHOLDER_REGEX, buttonsBySeverity);
}
