/**
 * Interface for custom events.
 *
 * @interface
 */
export interface DotEvent<T> {
    name: string;
    data?: T;
}
