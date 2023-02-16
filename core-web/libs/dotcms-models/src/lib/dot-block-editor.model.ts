export type EditorAssetTypes = 'image' | 'video';

export type Action = {
    command: string;
    menuLabel: string;
    icon: string;
    name: string;
};

export type Block = {
    url: string;
    actions: Array<Action>;
};

export type CustomBlock = {
    extensions: Block[];
};
