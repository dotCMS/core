/**
 * The primary theme model to be used in components and stores.
 *
 * This interface defines the minimal, normalized theme entity used across all
 * UI components, data-access services, and global store state.
 */
export interface DotTheme {
    identifier: string;
    inode: string;
    path: string;
    title: string;
    themeThumbnail: string | null;
    name: string;
    hostId: string;
}
