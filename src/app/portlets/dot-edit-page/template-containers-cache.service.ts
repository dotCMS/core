import { Injectable } from '@angular/core';
import { DotContainer } from '../../shared/models/container/dot-container.model';

/**
 * Save into cache the containers used by the current template
 */
@Injectable()
export class TemplateContainersCacheService {
    private containers: { [key: string]: {container: DotContainer}};

    set(containers: { [key: string]: {container: DotContainer}}): void {
        this.containers = containers;
    }

    get(containerId: string): DotContainer {
        return this.containers[containerId] ? this.containers[containerId].container :  null;
    }
}
