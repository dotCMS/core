// This is the payload we recieve from the iframe for add action
export interface AddContentletPayload {
    container?: Container;
    pageContainers: Container[];
    pageID: string;
    personaTag?: string;
}

// This is the payload we recieve from the iframe for delete action
export interface DeleteContentletPayload {
    container: Container;
    pageContainers: Container[];
    pageID: string;
    contentletId: string;
    personaTag?: string;
}

export interface SetUrlPayload {
    url: string;
}

export interface Container {
    acceptTypes?: string;
    identifier: string;
    uuid: string;
    contentletsId?: string[];
    personaTag?: string;
}

export interface SavePagePayload extends AddContentletPayload {
    whenSaved?: () => void;
}

export interface ContainerActionPayload {
    pageContainers: Container[];
    container: Container;
    contentletID: string;
    personaTag?: string;
}
