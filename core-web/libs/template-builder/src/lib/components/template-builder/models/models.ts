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
    layoutProperties: DotTemplateLayoutProperties;
}

export type WidgetType = 'col' | 'row';

export enum TemplateBuilderBoxSize {
    large = 'large',
    medium = 'medium',
    small = 'small'
}

/**
 * @description This interface is used to define the properties of the template layout
 *
 * @export
 * @interface DotTemplateLayoutProperties
 */
export interface DotTemplateLayoutProperties {
    header: boolean;
    footer: boolean;
    sidebar: {
        location?: string;
        containers?: DotTemplateBuilderContainer[];
        width?: string;
    };
}
