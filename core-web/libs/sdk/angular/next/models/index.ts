/* eslint-disable @typescript-eslint/no-explicit-any */

import { Type } from '@angular/core';

import { DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/uve/types';

/**
 * Represents a dynamic component entity.
 * @typedef {Promise<Type<any>>} DynamicComponentEntity
 * @memberof @dotcms/angular
 */
export type DynamicComponentEntity = Promise<Type<any>>;

/**
 * Represents the context of a DotCMS page.
 */
export interface DotCMSPageContext {
    /**
     * Represents the DotCMS page asset.
     * @type {DotCMSPageAsset}
     * @memberof DotCMSPageContext
     */
    page: DotCMSPageAsset;

    /**
     * Represents the dynamic components of the page for each Content Type.
     * @type {DotCMSPageComponent}
     * @memberof DotCMSPageContext
     */
    components: DotCMSPageComponent;

    /**
     * Indicates the renderer mode.
     * @type {DotCMSPageRendererMode}
     * @memberof DotCMSPageContext
     */
    mode: DotCMSPageRendererMode;
}

/**
 * Represents a DotCMS page component.
 * Used to store the dynamic components of a DotCMS page.
 * @typedef {Record<string, DynamicComponentEntity>} DotCMSPageComponent
 * @memberof @dotcms/angular
 */
export type DotCMSPageComponent = Record<string, DynamicComponentEntity>;
