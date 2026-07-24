import { RemoteCustomExtensions } from './dot-block-editor.model';

export const REMOTE_BLOCK_NAME_REQUIRED_WARNING =
    '[remote-extension] customBlocks action.name is required for remote blocks';

export function getDeclaredRemoteBlockNames(customBlocks: RemoteCustomExtensions): string[] {
    return customBlocks.extensions.flatMap((extension) =>
        (extension.actions || [])
            .map((action) => action?.name)
            .filter((name): name is string => {
                const isValidName = typeof name === 'string' && name.trim().length > 0;

                if (!isValidName) {
                    console.warn(REMOTE_BLOCK_NAME_REQUIRED_WARNING);
                }

                return isValidName;
            })
    );
}

export function warnOnUnmatchedRemoteBlockNames(
    customBlocks: RemoteCustomExtensions,
    registeredNodeNames: Iterable<string>
): void {
    const registered = new Set(registeredNodeNames);

    getDeclaredRemoteBlockNames(customBlocks).forEach((name) => {
        if (!registered.has(name)) {
            console.warn(
                `[remote-extension] declared action.name "${name}" did not match any loaded node`
            );
        }
    });
}
