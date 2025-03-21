/* eslint-disable @typescript-eslint/no-explicit-any */
export * from './dotcms.model';

import { Type } from '@angular/core';

import { DotCMSPageAsset } from './dotcms.model';

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
    pageAsset: DotCMSPageAsset;

    /**
     * Represents the dynamic components of the page for each Content Type.
     * @type {DotCMSPageComponent}
     * @memberof DotCMSPageContext
     */
    components: DotCMSPageComponent;

    /**
     * Indicates whether the page is being viewed inside the editor.
     * @type {boolean}
     * @memberof DotCMSPageContext
     */
    isInsideEditor: boolean;
}

/**
 * Represents a DotCMS page component.
 * Used to store the dynamic components of a DotCMS page.
 * @typedef {Record<string, DynamicComponentEntity>} DotCMSPageComponent
 * @memberof @dotcms/angular
 */
export type DotCMSPageComponent = Record<string, DynamicComponentEntity>;
