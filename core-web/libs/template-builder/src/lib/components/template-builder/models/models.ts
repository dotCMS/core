import { GridStackOptions, GridStackWidget } from 'gridstack';

export interface DotGridStackOptions extends GridStackOptions {
    children: DotGridStackWidget[];
}

export interface DotContainers {
    identifier: string;
    uuid: string;
}

export interface DotGridStackWidget extends GridStackWidget {
    containers?: DotContainers[];
    styleClass?: string[]; // We can join the classes in the parser, might be easier to work with
    subGridOpts?: DotGridStackOptions;
    parentId?: string;
}

export interface DotTemplateBuilderState {
    items: DotGridStackWidget[];
}
