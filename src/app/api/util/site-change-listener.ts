import { SiteService, Site } from 'dotcms-js/dotcms-js';
import { BaseComponent } from '../../view/components/_common/_base/base-component';
import { MessageService } from '../services/messages-service';

export abstract class SiteChangeListener extends BaseComponent {
    constructor(
        private siteService: SiteService,
        i18nKeys: string[],
        messageService: MessageService
    ) {
        super(i18nKeys, messageService);

        siteService.switchSite$.subscribe(site => this.changeSiteReload(site));
    }

    abstract changeSiteReload(site: Site): void;
}
