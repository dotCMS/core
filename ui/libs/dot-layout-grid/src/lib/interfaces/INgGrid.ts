export type NgConfigFixDirection = 'vertical' | 'horizontal' | 'cascade';

export interface NgGridConfig {
    margins?: number[];
    draggable?: boolean;
    resizable?: boolean;
    max_cols?: number;
    max_rows?: number;
    visible_cols?: number;
    visible_rows?: number;
    min_cols?: number;
    min_rows?: number;
    col_width?: number;
    row_height?: number;
    cascade?: string;
    min_width?: number;
    min_height?: number;
    fix_to_grid?: boolean;
    auto_style?: boolean;
    auto_resize?: boolean;
    maintain_ratio?: boolean;
    prefer_new?: boolean;
    zoom_on_drag?: boolean;
    limit_to_screen?: boolean;
    center_to_screen?: boolean;
    resize_directions?: string[];
    element_based_row_height?: boolean;
    fix_item_position_direction?: NgConfigFixDirection;
    fix_collision_position_direction?: NgConfigFixDirection;
    allow_overlap?: boolean;
}

export interface NgGridItemConfig {
    uid?: string;
    payload?: any;
    col?: number;
    row?: number;
    sizex?: number;
    sizey?: number;
    dragHandle?: string;
    resizeHandle?: ResizeHandle;
    fixed?: boolean;
    draggable?: boolean;
    resizable?: boolean;
    borderSize?: number;
    maxCols?: number;
    minCols?: number;
    maxRows?: number;
    minRows?: number;
    minWidth?: number;
    minHeight?: number;
    resizeDirections?: string[];
}

export interface NgGridItemEvent {
    uid: string;
    payload: any;
    col: number;
    row: number;
    sizex: number;
    sizey: number;
    width: number;
    height: number;
    left: number;
    top: number;
}

export interface NgGridItemSize {
    x: number;
    y: number;
}

export interface NgGridItemPosition {
    col: number;
    row: number;
}

export interface NgGridRawPosition {
    left: number;
    top: number;
}

export interface NgGridItemDimensions {
    width: number;
    height: number;
}

export type ResizeHandle =
    | string
    | {
          bottomright?: string;
          bottomleft?: string;
          topright?: string;
          topleft?: string;
          right?: string;
          left?: string;
          bottom?: string;
          top?: string;
      };
