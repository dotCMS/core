import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';

const DEFAULT_TITLE_PLATFORM = 'dotCMS Content Management Platform';

/**
 * DotCMS Title Strategy
 * Add the string path to useTitleStrategy to use TitleStrategy
 * to add title, in the Router add the title attribute.
 */
@Injectable()
export class DotTitleStrategy extends TitleStrategy {
    private readonly title = inject(Title);
    private readonly dotMessageService = inject(DotMessageService);

    constructor() {
        super();
    }

    override updateTitle(routerState: RouterStateSnapshot) {
        const currentRouteTitle = this.buildTitle(routerState);

        // TODO: after add the title to all the paths, delete the wrapper if
        if (this.useTitleStrategy(routerState.url)) {
            const metaTitle = currentRouteTitle
                ? `${this.translateTitle(currentRouteTitle)} - ${DEFAULT_TITLE_PLATFORM}`
                : `${DEFAULT_TITLE_PLATFORM}`;
            this.title.setTitle(metaTitle);
        }
    }

    /**
     * Add to the array allowedPaths all the path do you
     * want to use the global TitleStrategy
     * @param {string} currentUrl
     * @private
     */
    private useTitleStrategy(currentUrl: string) {
        const allowedPaths = ['experiments'];

        return !!allowedPaths.find((url) => currentUrl.includes(url));
    }

    /**
     * Translate the title sent in the Route
     *
     * @example
     * ```
     * {
     *   path: 'reports',
     *   component: ComponentToShow
     *   title: 'title.to.translate',
     * }
     * ```
     *
     * @param {string} title
     * @private
     */
    private translateTitle(title: string) {
        return this.dotMessageService.get(title);
    }
}
