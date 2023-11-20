export interface AddContentletPayload {
    container?: AddContentletContainerPayload;
    pageContainers: AddContentletContainerPayload[];
}

export interface AddContentletContainerPayload {
    acceptTypes: string;
    identifier: string;
    uuid: string;
    contentletsId: string[];
}
