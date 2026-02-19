import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { RequestHandlerExtra } from '@modelcontextprotocol/sdk/shared/protocol';
import { ServerNotification, ServerRequest } from '@modelcontextprotocol/sdk/types';

import { getContextStore } from './context-store';
import { Logger } from './logger';

const logger = new Logger('CONTEXT_CHECKING_SERVER');

/**
 * Middleware function that enforces context initialization before tool execution
 * @param toolName - Name of the tool being executed
 * @throws Error if context is not initialized and tool is not exempt
 */
function enforceInitialContextMiddleware(toolName: string): void {
    // Allow context_initialization tool to run without checking
    if (toolName === 'context_initialization') {
        return;
    }

    const contextStore = getContextStore();

    if (!contextStore.getIsInitialized()) {
        const errorMessage = `Cannot execute tool "${toolName}" because context initialization is required first.

REQUIRED ACTION: You must call the "context_initialization" tool before using any other tools.

The context_initialization tool:
- Discovers all available content types and their field schemas
- Loads current site information
- Provides essential context for content operations

Please run the context_initialization tool first, then retry your request.

Current initialization status: ${contextStore.getStatus().isInitialized ? 'Initialized' : 'Not initialized'}
Timestamp: ${contextStore.getStatus().timestamp || 'Never'}`;

        logger.warn(`Context initialization required for tool: ${toolName}`);
        throw new Error(errorMessage);
    }

    logger.debug(`Context check passed for tool: ${toolName}`);
}

/**
 * Creates a context-checking server that enforces initialization requirements
 * @param server - The original MCP server instance
 * @returns A proxied server that checks context initialization before tool execution
 */
export function createContextCheckingServer(server: McpServer): McpServer {
    const originalRegisterTool = server.registerTool;

    logger.log('Creating context-checking server proxy');

    return new Proxy(server, {
        get(target, prop) {
            if (prop === 'registerTool') {
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                return function (this: McpServer, name: string, config: any, callback: any) {
                    logger.debug(`Registering tool with context checking: ${name}`);

                    const wrappedCallback = async (
                        args: unknown,
                        extra: RequestHandlerExtra<ServerRequest, ServerNotification>
                    ) => {
                        enforceInitialContextMiddleware(name);

                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        return callback(args as { [x: string]: any }, extra);
                    };

                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                    return (originalRegisterTool as any).call(this, name, config, wrappedCallback);
                };
            }

            // Return the original property for all other properties
            return Reflect.get(target, prop);
        }
    });
}
