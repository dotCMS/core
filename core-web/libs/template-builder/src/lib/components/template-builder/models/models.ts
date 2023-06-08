import { GridStackNode, GridStackOptions, GridStackWidget } from 'gridstack';

export interface DotGridStackOptions extends GridStackOptions {
    children: DotGridStackWidget[];
}

export interface DotTemplateBuilderContainer {
    identifier: string;
    uuid: string;
}

export interface DotGridStackWidget extends GridStackWidget {
    containers?: DotTemplateBuilderContainer[];
    styleClass?: string[]; // We can join the classes in the parser, might be easier to work with
    subGridOpts?: DotGridStackOptions;
    parentId?: string;
}

export interface DotGridStackNode extends GridStackNode {
    containers?: DotTemplateBuilderContainer[];
    styleClass?: string[]; // We can join the classes in the parser, might be easier to work with
    subGridOpts?: DotGridStackOptions;
    parentId?: string;
}

export interface DotTemplateBuilderState {
    items: DotGridStackWidget[];
}

export type WidgetType = 'col' | 'row';

export enum TemplateBuilderBoxSize {
    large = 'large',
    medium = 'medium',
    small = 'small'
}

export interface StyleClassModel {
    klass: string;
}
