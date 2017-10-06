import { Component } from '@angular/core';
import { NotLicensedService } from './api/services/not-licensed-service';
import { DotConfirmationService } from './api/services/dot-confirmation';

@Component({
    selector: 'dot-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent {
    constructor(notLicensedService: NotLicensedService, public dotConfirmationService: DotConfirmationService) {
        document.ondragover = document.ondrop = (ev) => {
            notLicensedService.init();
            ev.preventDefault();
        };
    }
}
