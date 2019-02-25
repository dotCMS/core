import { Injectable } from '@angular/core';
import { DotContainer } from '@models/container/dot-container.model';

/**
 * Save into cache the containers used by the current template
 */
@Injectable()
export class TemplateContainersCacheService {
    private containers: { [key: string]: { container: DotContainer } };

    set(containers: { [key: string]: { container: DotContainer } }): void {
        const c: { [key: string]: { container: DotContainer } } = {};
        Object.keys(containers).forEach(function(item: string) {
            if (containers[item].container.source === 'FILE') {
                c[containers[item].container.path] = containers[item];
            } else {
                c[item] = containers[item];
            }
        });
        this.containers = c;
    }

    get(containerId: string): DotContainer {
        return this.containers[containerId] ? this.containers[containerId].container : null;
    }
}
