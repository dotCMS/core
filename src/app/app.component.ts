import { Component } from '@angular/core';
import { NotLicensedService } from './api/services/not-licensed-service';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss']
})
export class AppComponent {
    constructor(notLicensedService: NotLicensedService) {
        document.ondragover = document.ondrop = (ev) => {
            notLicensedService.init();
            ev.preventDefault();
        };
    }
}
