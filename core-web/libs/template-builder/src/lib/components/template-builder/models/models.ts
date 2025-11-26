import { GridStackNode, GridStackOptions, GridStackWidget } from 'gridstack';

import { DotContainer, DotContainerMap, DotLayoutSideBar } from '@dotcms/dotcms-models';

export const SYSTEM_CONTAINER_IDENTIFIER = 'SYSTEM_CONTAINER';

export const BOX_WIDTH = 1; // THE Box initial width is 3 columns

/**
 * @description This is the model for using custom data on the GridStackOptions
 *
 * @export
 * @interface DotGridStackOptions
 * @extends {GridStackOptions}
 */
export interface DotGridStackOptions extends GridStackOptions {
    children: DotGridStackWidget[];
}

/**
 * @description This is the model for the Containers of Boxes
 *
 * @export
 * @interface DotTemplateBuilderContainer
 */
export interface DotTemplateBuilderContainer {
    identifier: string;
    uuid?: string;
}

/**
 * @description This is the model for using custom data on the GridStackWidget
 *
 * @export
 * @interface DotGridStackWidget
 * @extends {GridStackWidget}
 */
export interface DotGridStackWidget extends GridStackWidget {
    containers?: DotTemplateBuilderContainer[]; // Although we are using this for Rows and Boxes, be aware that Rows does not have containers
    styleClass?: string[]; // We can join the classes in the parser, might be easier to work with
    subGridOpts?: DotGridStackOptions;
    parentId?: string;
    willBoxFit?: boolean;
}

/**
 * @description This is the model for using custom data on the GridStackNode
 *
 * @export
 * @interface DotGridStackNode
 * @extends {GridStackNode}
 */
export interface DotGridStackNode extends GridStackNode {
    containers?: DotTemplateBuilderContainer[]; // Although we are using this for Rows and Boxes, be aware that Rows does not have containers
    styleClass?: string[]; // We can join the classes in the parser, might be easier to work with
    subGridOpts?: DotGridStackOptions;
    parentId?: string;
    willBoxFit?: boolean;
}

/**
 * @description This is the Store Model for TemplateBuilder
 *
 * @export
 * @interface DotTemplateBuilderState
 */
export interface DotTemplateBuilderState {
    rows: DotGridStackWidget[];
    containerMap: DotContainerMap;
    layoutProperties: DotTemplateLayoutProperties;
    resizingRowID: string;
    themeId: string;
    shouldEmit: boolean;
    templateIdentifier: string;
    defaultContainer?: DotContainer;
}

export type WidgetType = 'col' | 'row';

/**
 * @description This it the enum for TemplateBuilderBox Variants
 *
 * @export
 * @enum {string}
 */
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
    sidebar: DotLayoutSideBar;
}

/**
 * @description This it the model for the Scroll Direction of the GridStack
 *
 * @export
 * @enum {number}
 */
export enum SCROLL_DIRECTION {
    UP = 'UP',
    DOWN = 'DOWN',
    NONE = 'NONE'
}
