import {Component, OnInit, ViewEncapsulation} from '@angular/core';
import { LoginService, LoggerService } from 'dotcms-js/dotcms-js';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-login-page-component',
    styleUrls: ['./login-page.scss'],
    templateUrl: 'login-page-component.html'
})
/**
 * The login component allows the user to fill all
 * the info required to log in the dotCMS angular backend
 */
export class LoginPageComponent implements OnInit {

    constructor(private loginService: LoginService, private loggerService: LoggerService) {}

    ngOnInit(): void {
        this.loginService.getLoginFormInfo('', []).subscribe(
            data => {
                // Translate labels and messages
                const entity = data.entity;

                // Set background color and image with the values provided by the service
                if (entity.backgroundColor !== 'undefined' && entity.backgroundColor !== '') {
                    document.body.style.backgroundColor = entity.backgroundColor;
                }
                if (entity.backgroundPicture !== 'undefined' && entity.backgroundPicture !== '') {
                    document.body.style.backgroundImage = 'url(' + entity.backgroundPicture + ')';
                }
            },
            error => {
                this.loggerService.debug(error);
            }
        );
    }
}
