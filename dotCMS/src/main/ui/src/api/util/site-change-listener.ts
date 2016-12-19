import {SiteService} from '../services/site-service';
import {Site} from '../services/site-service';
import {BaseComponent} from '../../view/components/common/_base/base-component';
import {MessageService} from '../services/messages-service';

export abstract class SiteChangeListener extends BaseComponent {

    constructor(private siteService: SiteService, i18nKeys: string[], private messageService: MessageService) {
        super(i18nKeys, messageService);

        siteService.switchSite$.subscribe(
            site => this.changeSiteReload( site )
        );
    }

    abstract changeSiteReload(site: Site): void;
}
