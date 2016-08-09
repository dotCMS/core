import {Component, ViewEncapsulation, Input, Output, EventEmitter} from '@angular/core';
import {NgForm} from '@angular/forms';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';

import {LoginService} from '../../../../../api/services/login-service';

// angular material imports
import {MdButton} from '@angular2-material/button';
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {DotCMSHttpService} from '../../../../../api/services/http/dotcms-http-service';
import {ChangePasswordData} from './reset-password-container';

@Component({
    directives: [MdButton, MD_INPUT_DIRECTIVES],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [],
    selector: 'dot-reset-password-component',
    styleUrls: [],
    templateUrl: ['reset-password-component.html'],
})

export class ResetPasswordComponent {

    @Input() login:string = '';
    @Input() private token:string = '';
    @Input()  message: string = '';

    @Output() changePassword  = new EventEmitter<ChangePasswordData>();

    form: FormGroup;

    private language: string = '';

    // labels
    private resetPasswordLabel: string = '';
    private enterPasswordLabel: string = '';
    private confirmPasswordLabel: string = '';
    private changePasswordButton: string = '';

    //Message
    private resetPasswordSuccessMessage:string = '';
    private resetPasswordConfirmationDoNotMessage:string = '';
    private mandatoryFieldError: string = '';
    private passwordMandatoryFieldError: string = '';
    private confirmPasswordMandatoryFieldError: string = '';

    private password:string = '';
    private confirmPassword:string = '';

    private i18nMessages: Array<string> = [ 'error.form.mandatory', 'reset-password', 'enter-password', 're-enter-password', 'change-password', 'reset-password-success', 'reset-password-confirmation-do-not-match'];

    constructor( private loginService: LoginService, fbld: FormBuilder) {
        this.form = fbld .group({
            password: ['', Validators.required],
            confirmPassword: ['', Validators.required]
        });
    }

    ngOnInit(){
        this.loginService.getLoginFormInfo(this.language, this.i18nMessages).subscribe((data) => {
            let dataI18n = data.i18nMessagesMap;
            let entity = data.entity;

            this.resetPasswordLabel = dataI18n['reset-password'];
            this.enterPasswordLabel = dataI18n['enter-password'];
            this.confirmPasswordLabel = dataI18n['re-enter-password'];
            this.changePasswordButton = dataI18n['change-password'];
            this.mandatoryFieldError = dataI18n['error.form.mandatory'];
            this.passwordMandatoryFieldError = (this.mandatoryFieldError).replace('{0}', this.enterPasswordLabel);
            this.confirmPasswordMandatoryFieldError = (this.mandatoryFieldError).replace('{0}', this.confirmPasswordLabel);
            this.resetPasswordConfirmationDoNotMessage = dataI18n['reset-password-confirmation-do-not-match'];
            this.resetPasswordSuccessMessage = dataI18n['reset-password-success'];
        }, (error) => {
                console.log(error);
        });
    }

    public ok():void{
        if (this.password == this.confirmPassword) {
            this.changePassword.emit({
                login: this.login,
                password: this.password,
                token: this.token
            });
        }else{
            this.message = this.resetPasswordConfirmationDoNotMessage;
        }
    }

    private cleanConfirmPassword(){
        this.clean();
        this.confirmPassword = '';
    }
    private clean(){
        this.message = '';
    }
}
