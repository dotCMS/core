import { ContentType } from '../../types/contentype';

/**
 * Utility function to transform content types to plain text format
 * This is the primary formatter for content type data used across content-type tools
 */
export function formatContentTypesAsText(contentTypes: ContentType[]): string {
    return contentTypes
        .map((contentType) => {
            const lines = [
                `Content Type: ${contentType.name}`,
                `Description: ${contentType.description || 'No description'}`,
                `Variable: ${contentType.variable}`,
                `URL Pattern: ${contentType.folderPath || 'No URL pattern'}`,
                `Icon: ${contentType.icon || 'No icon'}`,
                `Total Entries: ${contentType.nEntries || 0}`,
                '',
                'Fields:'
            ];

            // Use fields property directly
            const fields = contentType.fields || [];

            // Sort fields by sort order
            fields.sort((a, b) => a.sortOrder - b.sortOrder);

            // Format each field
            fields.forEach((field) => {
                const attributes = [];
                if (field.required) attributes.push('Required');
                if (field.system) attributes.push('System');
                if (field.searchable) attributes.push('Searchable');
                if (field.listed) attributes.push('Listed');
                if (field.unique) attributes.push('Unique');

                const attributesText = attributes.length > 0 ? ` [${attributes.join(', ')}]` : '';
                const optionalText = !field.required ? ' [Optional]' : '';

                lines.push(
                    `- ${field.name} (${field.variable}) - ${field.fieldTypeLabel || field.fieldType || 'Unknown'}${field.required ? attributesText : optionalText + attributesText}`
                );
            });

            // Add workflow information
            if (contentType.workflows && contentType.workflows.length > 0) {
                lines.push('');
                lines.push(`Workflow: ${contentType.workflows.map((w) => w.name).join(', ')}`);
            } else {
                lines.push('');
                lines.push('Workflow: No workflow assigned');
            }

            return lines.join('\n');
        })
        .join('\n\n' + '='.repeat(80) + '\n\n');
}
