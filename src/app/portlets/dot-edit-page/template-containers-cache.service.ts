import { Injectable } from '@angular/core';
import {
    CONTAINER_SOURCE,
    DotContainerMap,
    DotContainer
} from '@models/container/dot-container.model';

/**
 * Save into cache the containers used by the current template
 */
@Injectable()
export class TemplateContainersCacheService {
    private containers: DotContainerMap;

    set(containers: DotContainerMap): void {
        const mappedContainers: DotContainerMap = {};

        Object.keys(containers).forEach(function(item: string) {
            mappedContainers[this.getContainerReference(containers[item].container)] =
                containers[item];
        }, this);
        this.containers = mappedContainers;
    }

    get(containerId: string): DotContainer {
        return this.containers[containerId] ? this.containers[containerId].container : null;
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
