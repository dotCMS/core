import { Injectable } from '@angular/core';
import {
    CONTAINER_SOURCE,
    DotContainerMap,
    DotContainer
} from '@models/container/dot-container.model';

/**
 * Save into cache the containers used by the current template
 */
@Injectable({
    providedIn: 'root'
})
export class DotTemplateContainersCacheService {
    private containers: DotContainerMap;

    set(containers: DotContainerMap): void {
        this.containers = containers;
    }

    get(containerId: string): DotContainer {
        return this.containers[containerId];
    }

    /**
     * Based on the container source, it returns the identifier that should be used as reference.
     *
     * @param dotContainer
     * @returns string
     * @memberof DotTemplateContainersCacheService
     */
    getContainerReference(dotContainer: DotContainer): string {
        return dotContainer.source === CONTAINER_SOURCE.FILE
            ? dotContainer.path
            : dotContainer.identifier;
    }
}
