import { DotCMSContentlet } from './dot-contentlet.model';

// This will extend the DotCMSContentlet with more properties,
// but for now we will just use the DotCMSContentlet until we have folders on the request response
export type DotContentDriveItem = DotCMSContentlet;

/**
 * Interface representing data needed for context menu interactions
 * @interface ContextMenuData
 * @property {Event} event - The DOM event that triggered the context menu
 * @property {DotContentDriveItem} contentlet - The content item associated with the context menu
 */
export interface ContextMenuData {
    event: Event;
    contentlet: DotContentDriveItem;
}
