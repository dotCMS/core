import { Component } from '@angular/core';

import { NxWelcomeComponent } from './nx-welcome.component';

@Component({
    standalone: true,
    imports: [NxWelcomeComponent],
    selector: 'dotcms-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent {
    title = 'angular-elements';
}
