// This is the payload we recieve from the iframe for add action
export interface AddContentletPayload {
    container?: Container;
    pageContainers: Container[];
    pageID: string;
}

// This is the payload we recieve from the iframe for delete action
export interface DeleteContentletPayload {
    container: Container;
    pageContainers: Container[];
    pageID: string;
    contentletId: string;
}

export interface Container {
    acceptTypes?: string;
    identifier: string;
    uuid: string;
    contentletsId?: string[];
}

export interface SavePagePayload extends AddContentletPayload {
    whenSaved?: () => void;
}
