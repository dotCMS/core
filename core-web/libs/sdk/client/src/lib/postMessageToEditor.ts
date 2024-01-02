import { CUSTOMER_ACTIONS } from './enums';

/**
 * Post message props
 *
 * @export
 * @template T
 * @interface PostMessageProps
 */
type PostMessageProps<T> = {
    action: CUSTOMER_ACTIONS;
    payload?: T;
};

/**
 * Post message to dotcms page editor
 *
 * @export
 * @template T
 * @param {PostMessageProps<T>} message
 */
export function postMessageToEditor<T = unknown>(message: PostMessageProps<T>) {
    window.parent.postMessage(message, '*');
}
