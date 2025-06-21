import { ComponentRef, TemplateRef, Type } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Configuration options for the drawer
 */
export interface DotDrawerOptions<T = any, D = any> {
    /**
     * The component to render inside the drawer
     */
    nzContent?: Type<T> | TemplateRef<T> | string;

    /**
     * Data to pass to the component
     */
    nzContentParams?: Partial<T & D>;

    /**
     * Drawer title
     */
    nzTitle?: string | TemplateRef<void>;

    /**
     * Drawer width (when position is left/right)
     */
    nzWidth?: number | string;

    /**
     * Drawer height (when position is top/bottom)
     */
    nzHeight?: number | string;

    /**
     * Position of the drawer
     */
    nzPlacement?: 'left' | 'right' | 'top' | 'bottom';

    /**
     * Whether the drawer is closable
     */
    nzClosable?: boolean;

    /**
     * Whether the drawer has a mask
     */
    nzMask?: boolean;

    /**
     * Whether clicking the mask closes the drawer
     */
    nzMaskClosable?: boolean;

    /**
     * Custom CSS class for the drawer
     */
    nzWrapClassName?: string;

    /**
     * Z-index of the drawer
     */
    nzZIndex?: number;

    /**
     * Callback when the drawer is about to close
     * Return false to prevent closing
     */
    nzOnCancel?: () => Observable<boolean> | Promise<boolean> | boolean;

    /**
     * Whether to destroy the component when closed
     */
    nzDestroyOnClose?: boolean;

    /**
     * Custom styles for the drawer body
     */
    nzBodyStyle?: Record<string, string>;

    /**
     * Custom header content
     */
    nzExtra?: string | TemplateRef<void>;
}

/**
 * Options specifically for component-based drawers
 */
export type DotDrawerOptionsOfComponent<T = any, D = any> = DotDrawerOptions<T, D> & {
    nzContent: Type<T>;
};

/**
 * Result interface for drawer operations
 */
export interface DotDrawerResult<R = any> {
    type: 'close' | 'cancel' | 'confirm';
    data?: R;
}

/**
 * Configuration for the drawer container
 */
export interface DotDrawerConfig<T = any, D = any> {
    visible: boolean;
    options: DotDrawerOptions<T, D>;
    componentRef?: ComponentRef<T>;
}

/**
 * Drawer placement type
 */
export type DotDrawerPlacement = 'left' | 'right' | 'top' | 'bottom';
