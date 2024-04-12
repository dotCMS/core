/**
 * Bound information for a contentlet.
 *
 * @interface ContentletBound
 */
interface ContentletBound {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: string;
}
/**
 * Bound information for a container.
 *
 * @interface ContainerBound
 */
interface ContainerBound {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: string;
    contentlets: ContentletBound[];
}
/**
 * Calculates the bounding information for each page element within the given containers.
 *
 * @export
 * @param {HTMLDivElement[]} containers
 * @return {*} An array of objects containing the bounding information for each page element.
 */
export declare function getPageElementBound(containers: HTMLDivElement[]): ContainerBound[];
/**
 * An array of objects containing the bounding information for each contentlet inside a container.
 *
 * @export
 * @param {DOMRect} containerRect
 * @param {HTMLDivElement[]} contentlets
 * @return {*}
 */
export declare function getContentletsBound(containerRect: DOMRect, contentlets: HTMLDivElement[]): ContentletBound[];
/**
 * Get container data from VTLS.
 *
 * @export
 * @param {HTMLElement} container
 * @return {*}
 */
export declare function getContainerData(container: HTMLElement): {
    acceptTypes: string;
    identifier: string;
    maxContentlets: string;
    uuid: string;
};
/**
 * Get the closest container data from the contentlet.
 *
 * @export
 * @param {Element} element
 * @return {*}
 */
export declare function getClosestContainerData(element: Element): {
    acceptTypes: string;
    identifier: string;
    maxContentlets: string;
    uuid: string;
} | null;
/**
 * Find the closest contentlet element based on HTMLElement.
 *
 * @export
 * @param {(HTMLElement | null)} element
 * @return {*}
 */
export declare function findContentletElement(element: HTMLElement | null): HTMLElement | null;
export declare function findVTLElement(element: HTMLElement | null): HTMLElement | null;
export declare function findVTLData(target: HTMLElement): {
    inode: string | undefined;
    name: string | undefined;
}[] | null;
export {};
