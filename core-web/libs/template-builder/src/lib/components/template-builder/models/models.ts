import { GridStackNode, GridStackOptions, GridStackWidget } from 'gridstack';

import { DotContainerMap, DotLayoutSideBar } from '@dotcms/dotcms-models';

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
}

/**
 * @description This is the model for the DotAddStyleClassesDialogStore
 *
 * @export
 * @interface DotAddStyleClassesDialogState
 */
export interface DotAddStyleClassesDialogState {
    styleClasses: StyleClassModel[];
    selectedClasses: StyleClassModel[];
    filteredClasses: StyleClassModel[];
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
 * @description This it the model for Autocomplete StyleClasses
 *
 * @export
 * @interface StyleClassModel
 */
export interface StyleClassModel {
    cssClass: string;
}
