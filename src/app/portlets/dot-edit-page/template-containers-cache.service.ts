import { Injectable } from '@angular/core';
import {
    CONTAINER_SOURCE,
    DotContainer,
    DotContainerMap
} from '@models/container/dot-container.model';

/**
 * Save into cache the containers used by the current template
 */
@Injectable()
export class TemplateContainersCacheService {
    private containers: DotContainerMap;

    set(containers: DotContainerMap): void {
        const mappedContainers: DotContainerMap = {};
        this.containers = containers;

        Object.keys(containers).forEach(function(item: string) {
            if (containers[item].container.source === CONTAINER_SOURCE.FILE) {
                mappedContainers[containers[item].container.path] =
                    containers[item].container.identifier;
            }
        });

        this.containers = { ...this.containers, ...mappedContainers };
        console.log('containers', this.containers);
    }

    get(containerId: string): DotContainer {
        // return this.containers[containerId] ? this.containers[containerId].container : null;
        return this.containers[containerId]
            ? typeof this.containers[containerId].container === 'string'
              ? this.containers[<string>(<unknown>this.containers[containerId])]
              : this.containers[containerId].container
            : null;
    }

    /**
     * Based on the container source, it returns the identifier that should be used as reference.
     *
     * @param dotContainer
     * @returns string
     * @memberof TemplateContainersCacheService
     */
    getContainerReference(dotContainer: DotContainer): string {
        return dotContainer.source === CONTAINER_SOURCE.FILE
            ? dotContainer.path
            : dotContainer.identifier;
    }
}
