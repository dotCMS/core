import { Injectable } from '@angular/core';
import { CONTAINER_SOURCE, DotContainer } from '@models/container/dot-container.model';

/**
 * Save into cache the containers used by the current template
 */
@Injectable()
export class TemplateContainersCacheService {
    private containers: { [key: string]: { container: DotContainer } };

    set(containers: { [key: string]: { container: DotContainer } }): void {
        // const _this = this;
        const mappedContainers: { [key: string]: { container: DotContainer } } = {};

        // Object.keys(containers).forEach(function(item: string) {
        //     if (containers[item].container.source === 'FILE') {
        //         mappedContainers[containers[item].container.path] = containers[item];
        //     } else {
        //         mappedContainers[item] = containers[item];
        //     }
        // });


        Object.keys(containers).forEach(function(item: string) {
            mappedContainers[this.getContainerReference(containers[item].container)] =  containers[item];
        },  this);
        this.containers = mappedContainers;

        // Object.keys(containers).forEach(item => {
        //     debugger;
        //     this.containers[this.getContainerReference(containers[item].container)] =
        //         containers[item];
        // }, this);
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
