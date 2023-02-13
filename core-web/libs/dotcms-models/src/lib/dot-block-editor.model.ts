export type EditorAssetTypes = 'image' | 'video';

export type Actions = {
    command: string;
    menuLabel: string;
    icon: string;
    name: string;
};

export type Block = {
    url: string;
    actions: Array<Actions>;
};

export type CustomBlock = {
    extensions: Block[];
};
