import { Injectable } from '@angular/core';
import { DotContainer } from '@models/container/dot-container.model';

/**
 * Save into cache the containers used by the current template
 */
@Injectable()
export class TemplateContainersCacheService {
    private containers: { [key: string]: { container: DotContainer } };

    set(containers: { [key: string]: { container: DotContainer } }): void {
        const mappedContainers: { [key: string]: { container: DotContainer } } = {};
        Object.keys(containers).forEach(function(item: string) {
            if (containers[item].container.source === 'FILE') {
                mappedContainers[containers[item].container.path] = containers[item];
            } else {
                mappedContainers[item] = containers[item];
            }
        });
        this.containers = mappedContainers;
    }

    get(containerId: string): DotContainer {
        return this.containers[containerId] ? this.containers[containerId].container : null;
    }
}
