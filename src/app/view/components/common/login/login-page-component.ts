import {Component, EventEmitter, Output, ViewEncapsulation} from '@angular/core';
import {LoginService} from '../../../../api/services/login-service';
import {LoggerService} from '../../../../api/services/logger.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-login-page-component',
    styleUrls: ['login-page.css'],
    templateUrl: 'login-page-component.html',
})

/**
 * The login component allows the user to fill all
 * the info required to log in the dotCMS angular backend
 */
export class LoginPageComponent {
    @Output() recoverPassword: EventEmitter<boolean>  = new EventEmitter<boolean>(false);

    constructor(private loginService: LoginService, private loggerService: LoggerService) {
    }

    private ngOnInit(): void {

        this.loginService.getLoginFormInfo('', []).subscribe((data) => {

            // Translate labels and messages
            let entity = data.entity;

            // Set background color and image with the values provided by the service
            if (entity.backgroundColor !== 'undefined' && entity.backgroundColor !== '') {
                document.body.style.backgroundColor = entity.backgroundColor;
            }
            if (entity.backgroundPicture !== 'undefined' && entity.backgroundPicture !== '') {
                document.body.style.backgroundImage = 'url(' + entity.backgroundPicture + ')';
            }
        }, (error) => {
            this.loggerService.debug(error);
        });
    }
}
