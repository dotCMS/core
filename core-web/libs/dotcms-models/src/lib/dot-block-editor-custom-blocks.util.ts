import { RemoteCustomExtensions } from './dot-block-editor.model';

export const REMOTE_BLOCK_NAME_REQUIRED_WARNING =
    '[remote-extension] customBlocks action.name is required for remote blocks';

/**
 * `action.name` cannot be derived from `command` or `menuLabel` — it must match the node
 * name the remote bundle registers via `Node.create({ name })`. These warnings are the
 * only signal an admin gets, so each one names the offending action and the fix.
 */
function warnMissingRemoteBlockName(extensionUrl: string | undefined, action: unknown): void {
    const identifier =
        (action as { menuLabel?: string; command?: string })?.menuLabel ||
        (action as { command?: string })?.command;
    const describedAction = identifier ? `action "${identifier}"` : 'an unnamed action';
    const source = extensionUrl ? ` in "${extensionUrl}"` : '';

    console.warn(
        `${REMOTE_BLOCK_NAME_REQUIRED_WARNING}. Skipping ${describedAction}${source}: ` +
            `it will not be selectable in Allowed Blocks, and existing content using it ` +
            `renders as an "Unsupported block" placeholder. Add "name" set to the TipTap ` +
            `node name the bundle registers (for example "customGallery").`
    );
}

export function getDeclaredRemoteBlockNames(customBlocks: RemoteCustomExtensions): string[] {
    return customBlocks.extensions.flatMap((extension) =>
        (extension.actions || [])
            .filter((action) => {
                const name = action?.name;
                const isValidName = typeof name === 'string' && name.trim().length > 0;

                if (!isValidName) {
                    warnMissingRemoteBlockName(extension?.url, action);
                }

                return isValidName;
            })
            .map((action) => action.name)
    );
}

export function warnOnUnmatchedRemoteBlockNames(
    customBlocks: RemoteCustomExtensions,
    registeredNodeNames: Iterable<string>
): void {
    const registered = new Set(registeredNodeNames);
    const registeredList = [...registered];

    getDeclaredRemoteBlockNames(customBlocks).forEach((name) => {
        if (!registered.has(name)) {
            const loaded = registeredList.length
                ? `Nodes registered by the loaded bundles: ${registeredList.join(', ')}.`
                : 'No remote nodes were registered — check that the bundle URL loads and exports a TipTap node.';

            console.warn(
                `[remote-extension] declared action.name "${name}" did not match any loaded node. ` +
                    `Content using it renders as an "Unsupported block" placeholder. ${loaded}`
            );
        }
    });
}
