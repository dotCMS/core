import { DotContainer } from '@models/container/dot-container.model';

/**
 * It is a Container linked into a DotLayoutGridBox
 */
export interface DotContainerColumnBox {
    container: DotContainer;
    uuid?: string;
}
