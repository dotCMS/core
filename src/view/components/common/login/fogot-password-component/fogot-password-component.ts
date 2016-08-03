import {Component, EventEmitter , Input, Output, ViewEncapsulation} from '@angular/core';
import {LoginService} from '../../../../../api/services/login-service';

// angular material imports
import {MdButton} from '@angular2-material/button';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {DotCMSHttpService} from "../../../../../api/services/http/dotcms-http-service";
import {DotCMSHttpResponse} from "../../../../../api/services/http/dotcms-http-response";
import { Router } from '@ngrx/router';

@Component({
    directives: [MdButton,  MD_INPUT_DIRECTIVES],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [LoginService],
    selector: 'dot-fogot-password-component',
    styleUrls: [],
    templateUrl: ['fogot-password-component.html'],
})

export class FogotPasswordComponent {

    @Input() message: string;

    @Output() cancel = new EventEmitter<>();
    @Output() recoverPassword  = new EventEmitter<string>();

    private forgotPasswordLogin: string;
    private language: string = '';

    // labels
    forgotPasswordLabel: string = '';
    forgotPasswordButton: string = '';
    forgotPasswordConfirmationMessage: string = '';
    cancelButton: string = '';
    userIdOrEmailLabel:string = ''

    private i18nMessages: Array<string> = [  'user-id', 'email-address', 'forgot-password', 'get-new-password', 'cancel', 'an-email-with-instructions-will-be-sent'];

    constructor( private loginService: LoginService, private router: Router) {

    }

    ngOnInit(){
        this.loadLabels();
    }

    /**
     * Executes the recover password service
     */
    ok(): void {
        if (confirm(this.forgotPasswordConfirmationMessage)) {
            this.recoverPassword.emit(this.forgotPasswordLogin);
        }
    }

    /**
     * Update the color and or image according to the values specified
     */
    private loadLabels(): void {

        this.loginService.getLoginFormInfo(this.language, this.i18nMessages).subscribe((data) => {

            // Translate labels and messages
            let dataI18n = data.i18nMessagesMap;
            let entity = data.entity;


            if ('emailAddress' === entity.authorizationType) {
                this.userIdOrEmailLabel = dataI18n['email-address'];
            } else {
                this.userIdOrEmailLabel = dataI18n['user-id'];
            }

            this.forgotPasswordLabel = dataI18n['forgot-password'];
            this.forgotPasswordButton = dataI18n['get-new-password'];
            this.cancelButton = dataI18n.cancel;
            this.forgotPasswordConfirmationMessage = dataI18n['an-email-with-instructions-will-be-sent'];
        }, (error) => {
             console.log(error);
        });
    }
}
