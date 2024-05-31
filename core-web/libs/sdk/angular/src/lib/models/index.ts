/* eslint-disable @typescript-eslint/no-explicit-any */
export * from './dotcms.model';

import { Type } from '@angular/core';

import { DotCMSPageAsset } from './dotcms.model';
export type DynamicComponentEntity = Promise<Type<any>>;

export interface DotCMSPageContext {
    pageAsset: DotCMSPageAsset;
    components: DotCMSPageComponent;
    isInsideEditor: boolean;
}

export type DotCMSPageComponent = Record<string, DynamicComponentEntity>;
