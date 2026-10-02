import { Component } from '@angular/core';

/**
 * Page shell of the Configuration (Beta) portlet. Hosts the section cards and the sticky
 * action bar that saves the whole page.
 */
@Component({
    selector: 'dot-configuration-shell',
    templateUrl: './dot-configuration-shell.component.html',
    styleUrls: ['./dot-configuration-shell.component.scss'],
    host: { class: 'flex h-full min-h-0 flex-col' }
})
export class DotConfigurationShellComponent {}
