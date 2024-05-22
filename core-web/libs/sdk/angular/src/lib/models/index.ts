/* eslint-disable @typescript-eslint/no-explicit-any */
export * from './dotcms.model';

import { Type } from '@angular/core';
export type DynamicComponentEntity = Promise<Type<any>>;
