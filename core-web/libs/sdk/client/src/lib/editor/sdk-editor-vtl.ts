import { dotPageEditor } from './sdk-editor';

/**
 * This is the main entry point for the SDK VTL. It initializes the client and returns it.
 * This is added to VTL Script in the EditPage
 *
 * @type {*}
 */
const client = dotPageEditor.createClient();
client.init();
