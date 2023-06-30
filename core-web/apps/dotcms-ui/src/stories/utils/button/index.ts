const SEVERITY_PLACEHOLDER_REGEX = /SEVERITY_TEMPLATE/g;
const BUTTONS_PLACEHOLDER_REGEX = /BUTTONS_TEMPLATE/g;
const SEVERITY_VALUE_REGEX = /SEVERITY_VALUE/g;
const SIZE_VALUE_REGEX = /SIZE_VALUE/g;
const BUTTON_TYPE_REGEX = /BUTTON_TYPE/g;

const MAIN_DIV_TEMPLATE = `<div style="display: flex; gap: 24px; flex-direction: column; align-items: center;">SEVERITY_TEMPLATE</div>`;
const SEVERITY_SEPARATOR_TEMPLATE = `<div style="display: flex; gap: 8px; justify-content: center; width: fit-content;">BUTTONS_TEMPLATE</div>`;
export const MAIN_BUTTONS_TEMPLATE = `<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE" pButton label="Submit"></button>
<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE" pButton label="Submit" icon="pi pi-plus"></button>
<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE" pButton label="Disabled" disabled="true"></button>`;

export const ICON_ONLY_BUTTONS_TEMPLATE = `<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE p-button-rounded" pButton icon="pi pi-ellipsis-v"></button>
<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE p-button-rounded" pButton icon="pi pi-ellipsis-v"></button>
<button class="SIZE_VALUE SEVERITY_VALUE BUTTON_TYPE p-button-rounded" pButton icon="pi pi-ellipsis-v" disabled="true"></button>`;

const severities = ['', 'p-button-secondary', 'p-button-danger'];
const sizes = ['p-button-lg', '', 'p-button-sm'];

/**
 * Creates a button template.
 *
 * @export
 * @param {string} [buttonType='']
 * @return {*}
 */
export function createButtonTemplate(
    buttonType: string = '',
    buttonTemplate: string = MAIN_BUTTONS_TEMPLATE
) {
    const mainDiv = MAIN_DIV_TEMPLATE;

    const buttonsBySeverity = severities
        .map((severity) => {
            let sizeDiv = '';
            sizes.forEach((size) => {
                const button = buttonTemplate
                    .replace(SEVERITY_VALUE_REGEX, severity)
                    .replace(SIZE_VALUE_REGEX, size)
                    .replace(BUTTON_TYPE_REGEX, buttonType);

                sizeDiv += SEVERITY_SEPARATOR_TEMPLATE.replace(BUTTONS_PLACEHOLDER_REGEX, button);
            });

            return sizeDiv;
        })
        .join('');

    return mainDiv.replace(SEVERITY_PLACEHOLDER_REGEX, buttonsBySeverity);
}
