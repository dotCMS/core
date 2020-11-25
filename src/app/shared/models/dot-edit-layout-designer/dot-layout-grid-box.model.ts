import { DotContainerColumnBox } from './dot-container-column-box.model';
import { NgGridItemConfig } from 'dot-layout-grid';

/**
 * It is NgGrid box
 *
 * for more information see: https://github.com/BTMorton/angular2-grid
 */
export interface DotLayoutGridBox {
    config: NgGridItemConfig;
    containers: DotContainerColumnBox[];
}
