import { DotContainer } from '@dotcms/dotcms-models';

/**
 * It is a Container linked into a DotLayoutGridBox
 */
export interface DotContainerColumnBox {
    container: DotContainer;
    uuid?: string;
}
