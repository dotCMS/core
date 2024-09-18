/**
 * Represents a listener for DotcmsClientListener.
 *
 * @typedef {Object} DotcmsClientListener
 * @property {string} action - The action that triggers the event.
 * @property {string} event - The name of the event.
 * @property {function(...args: any[]): void} callback - The callback function to handle the event.
 */
export type DotcmsClientListener = {
    action: string;
    event: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    callback: (...args: any[]) => void;
};
