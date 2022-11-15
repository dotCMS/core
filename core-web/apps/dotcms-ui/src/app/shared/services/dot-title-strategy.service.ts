import { Injectable } from '@angular/core';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { Title } from '@angular/platform-browser';

/**
 * Page Title Strategy
 * Add the string path to useTitleStrategy to use TitleStrategy
 * to add title, in the Router add the title atribute.
 */
@Injectable()
export class DotTemplatePageTitleStrategy extends TitleStrategy {
    constructor(private readonly title: Title) {
        super();
    }

    override updateTitle(routerState: RouterStateSnapshot) {
        const dotCMSTitle = 'dotCMS Content Management Platform';
        const title = this.buildTitle(routerState);

        // TODO: after add the title to all the paths, delete the wrapper if
        if (this.useTitleStrategy(routerState.url)) {
            const metaTitle = title ? `${title} - ${dotCMSTitle}` : `${dotCMSTitle}`;
            this.title.setTitle(metaTitle);
        }
    }

    /**
     * Add to the array allowedPaths all the path do you
     * want use the global TitleStrategy
     * @param currentUrl
     * @private
     */
    private useTitleStrategy(currentUrl: string) {
        const allowedPaths = ['experiments'];
        const search = allowedPaths.find((url) => currentUrl.includes(url));

        return !!search;
    }
}
