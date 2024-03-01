export declare function getPageElementBound(containers: HTMLDivElement[]): {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: {
        container: {
            acceptTypes: string | undefined;
            identifier: string | undefined;
            maxContentlets: string | undefined;
            uuid: string | undefined;
        };
    };
    contentlets: {
        x: number;
        y: number;
        width: number;
        height: number;
        payload: string;
    }[];
}[];
export declare function getContentletsBound(containerRect: DOMRect, contentlets: HTMLDivElement[]): {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: string;
}[];
export declare function getContainerData(container: HTMLElement): {
    acceptTypes: string | undefined;
    identifier: string | undefined;
    maxContentlets: string | undefined;
    uuid: string | undefined;
};
export declare function getClosestContainerData(element: Element): {
    acceptTypes: string | undefined;
    identifier: string | undefined;
    maxContentlets: string | undefined;
    uuid: string | undefined;
} | null;
export declare function findContentletElement(element: HTMLElement | null): HTMLElement | null;
