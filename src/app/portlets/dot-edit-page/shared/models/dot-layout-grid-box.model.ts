import { NgGridItemConfig } from 'angular2-grid';
import { DotContainer } from './dot-container.model';

export interface DotLayoutGridBox {
    config: NgGridItemConfig;
    containers: DotContainer[];
}
