import { GridStackNode, GridStackOptions, GridStackWidget } from 'gridstack';

export interface DotGridStackOptions extends GridStackOptions {
    children: DotGridStackWidget[];
}

export interface DotContainer {
    identifier: string;
    uuid: string;
}

export interface DotGridStackWidget extends GridStackWidget {
    containers?: DotContainer[];
    styleClass?: string[]; // We can join the classes in the parser, might be easier to work with
    subGridOpts?: DotGridStackOptions;
    parentId?: string;
}

export interface DotGridStackNode extends GridStackNode {
    containers?: DotContainer[];
    styleClass?: string[]; // We can join the classes in the parser, might be easier to work with
    subGridOpts?: DotGridStackOptions;
    parentId?: string;
}

export interface DotTemplateBuilderState {
    items: DotGridStackWidget[];
}

export type WidgetType = 'col' | 'row';
