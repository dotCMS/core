export interface AddContentletPayload {
    container?: Container;
    pageContainers: Container[];
    pageID: string;
}

export interface Container {
    acceptTypes: string;
    identifier: string;
    uuid: string;
    contentletsId: string[];
}

export interface SavePagePayload extends AddContentletPayload {
    contentletID: string;
    whenSaved?: () => void;
}
