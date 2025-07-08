import { McpSuccessResponse } from '../../utils/response';

/**
 * Entity data structure for workflow responses
 */
export interface WorkflowEntity {
    identifier?: string;
    inode?: string;
    contentType?: string;
    languageId?: string | number;
}

/**
 * Creates formatted entity response for workflow operations
 * This utility is specific to workflow tools and formats entity information
 * in a consistent way for both save and action operations.
 */
export function createEntitySuccessResponse(
    actionMessage: string,
    entity: WorkflowEntity
): McpSuccessResponse {
    const text = `${actionMessage}

Identifier: ${entity.identifier}
Inode: ${entity.inode}
Content Type: ${entity.contentType}
Language ID: ${entity.languageId}`;

    return {
        content: [
            {
                type: 'text',
                text
            }
        ]
    };
}

/**
 * Validates entity response data
 * Ensures the entity has required fields for proper formatting
 */
export function validateEntityResponse(entity: unknown): entity is WorkflowEntity {
    return (
        typeof entity === 'object' &&
        entity !== null &&
        ('identifier' in entity || 'inode' in entity)
    );
}
