import {Component,Input, Inject, ViewEncapsulation} from "@angular/core";
import {MD_INPUT_DIRECTIVES} from '@angular2-material/input/input';
import {MdButton} from '@angular2-material/button';
import {MdToolbar} from '@angular2-material/toolbar';
import {I18nService} from "../../../../api/system/locale/I18n";
import {MdCheckbox} from '@angular2-material/checkbox/checkbox';
import {MD_CARD_DIRECTIVES} from '@angular2-material/card';
import {LoginService} from '../../../../api/services/login-service';
import {Observable} from 'rxjs/Rx';

@Component({
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'dot-login-component',
    template: ` 
        <div class="login-component-container">
            <md-card id="loginBox" class="loginBox">
                <!-- md-toolbar color="primary">{{LoginLabel}}</md-toolbar -->
                <md-card-title>{{LoginLabel}}</md-card-title>
                <div class="error-message" id="dotLoginMessagesDiv">{{message}}</div>
                <md-content>                
                        <table>
                            <tr>
                                <td class="serverBox">
                                    <div>
                                    <p>Company Logo</p>
                                    <p>Server Id</p>
                                    <p>licence</p>
	                                <p>Release version</p>
	                                <p>Release build date</p>
                                    </div>
                                </td>
                                <td><div class="middleBox"></div></td>
                                <td>
                                   <table >
                                        <tr>
                                            <td class="td30">
                                                <label>{{emailAddressLabel}}:</label>
                                            </td>
                                            <td>
                                                <md-input placeholder="{{emailAddressLabel}}" [(ngModel)]="my_account_login" id="my_account_login" floatingPlaceholder="false"></md-input>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="td30">
                                                <label>{{passwordLabel}}:</label>
                                            </td>
                                            <td>
                                                <md-input placeholder="{{passwordLabel}}" [(ngModel)]="password" id="password" type="password" floatingPlaceholder="false"></md-input>
                                            </td>
                                        </tr>
                                        <tr>    
                                            <td colspan="2">
                                                <md-checkbox [(ngModel)]="my_account_r_m" id="my_account_r_m">{{rememberMeLabel}}</md-checkbox>
                                            </td>
                                            <td>
                                                <select id="language" [(ngModel)]="language" (change)="changeLanguage($event.target.value)">
                                                 <template ngFor let-lan [ngForOf]="languages">
                                                 <option  value="{{lan}}">
                                                    <img title="" id="myLanguageImage" alt="" src="/html/images/languages/{{lan}}.gif" align="left" style="padding:1px;border:1px solid #ffffff">
                                                  </option>
                                                  </template>
                                                </select>
                                            </td>
                                        </tr>
                                        <tr>
                                          <td colspan="2">
                                                <md-card-actions>
                                                    <section>
                                                        <md-button md-raised-button color="warn" (click)="logInUser()">{{loginButton}}</md-button>
                                                        <md-button md-raised-button (click)="showForgotPassword()">{{forgotPasswordButton}}</md-button>
                                                    </section>
                                                </md-card-actions>
                                            </td>
                                        </tr>
                                    </table>
                                 </td>   
                            </tr>
                        </table>                
                </md-content>
            </md-card>
            <md-card id="forgotPassword" class="forgotPasswordBox hideBox">
                <md-card-title>{{forgotPasswordLabel}}</md-card-title>
                <!-- md-toolbar color="primary">Forgot Password</md-toolbar -->
                <md-card-content>                
                    <md-input placeholder="{{emailAddressLabel}}" [(ngModel)]="forgotPasswordEmail" id="forgotPasswordEmail" floatingPlaceholder="false"></md-input>
                </md-card-content>
                <md-card-actions>
                    <section>
                        <md-button md-raised-button color="warn" (click)="recoverPassword()">{{forgotPasswordButton}}</md-button> 
                        <md-button md-raised-button color="warn" (click)="cancelRecoverPassword()">{{cancelButton}}</md-button>
                    </section>                    
                </md-card-actions>
            </md-card>
        </div>
    `,
    providers: [LoginService],
    directives:[MdToolbar,MD_INPUT_DIRECTIVES,MdButton,MdCheckbox,MD_CARD_DIRECTIVES],
    styleUrls: ['login-component.css'],
    encapsulation: ViewEncapsulation.Emulated

})

//export interface Locale {
//    language: string,
//    country: string
//}
/**
 * The login component allows the user to fill all
 * the info required to log in the dotCMS angular backend
 */
export class LoginComponent {
    @Input() my_account_login:string= '@dotcms.com';
    @Input() password:string;
    @Input() my_account_r_m:boolean=false;
    @Input() forgotPasswordEmail:string;

    languages:string[];
    @Input() language:string='en_US';
    message:string='';
    serverInfo:any[];

    //labels
    LoginLabel:string="";
    emailAddressLabel:string="";
    userIdLabel:string="";
    passwordLabel:string="";
    rememberMeLabel:string="";
    forgotPasswordLabel:string="";
    loginButton:string="";
    forgotPasswordButton:string="";
    cancelButton:string="";


    _loginService : LoginService;

    constructor(@Inject('menuItems') private menuItems:any[], private _resources:I18nService, private _service:LoginService) {
        this._resources.get("Login").subscribe((label)=>{
            this.LoginLabel = label;
        });
        this._resources.get("email-address").subscribe((label)=>{
            this.emailAddressLabel = label;
        });
        this._resources.get("user-id").subscribe((label)=>{
            this.userIdLabel = label;
        });
        this._resources.get("password").subscribe((label)=>{
            this.passwordLabel = label;
        });
        this._resources.get("remember-me").subscribe((label)=>{
            this.rememberMeLabel = label;
        });
        this._resources.get("sign-in").subscribe((label)=>{
            this.loginButton = label;
        });
        this._resources.get("forgot-password").subscribe((label)=>{
            this.forgotPasswordLabel = label;
        });

        this._resources.get("get-new-password").subscribe((label)=>{
            this.forgotPasswordButton = label;
        });

        this._resources.get("cancel").subscribe((label)=>{
            this.cancelButton = label;
        });

        this.serverInfo = _service.getServerInfo();
        //to improve
        this.languages=['en_US','sp_SP','fr_FR'];

        this._loginService = _service;

        //this.updateScreenBackground();
    }

    /**
     *  Executes the log in service
     */
    logInUser(){

        this._loginService.logInUser(this.my_account_login, this.password, this.my_account_r_m, this.language).subscribe((result:any)=> {
            if (result.errors.length > 0) {
                this.message = result.errors[0].message;
            } else {
                this.message = '';
                window.location.reload();
            }
        }, (error) =>{
            if(error.response.status == 400 || error.response.status == 401){
                this.message = this.getErrorMessage(error);
            }else{
                console.log('Login ERROR: '+error);
                //this.message = getErrorMessage(error);
            }
        });

    }

    /**
     * Executes the recover password service
     */
    recoverPassword(){
        document.getElementById('forgotPassword').className='forgotPasswordBox hideBox';
        document.getElementById('loginBox').style.display='';

        this._loginService.recoverPassword(this.forgotPasswordEmail).subscribe((result:any)=> {

        },(error)=>{
            console.log('Recover password ERROR: '+error);
            this.message = this.getErrorMessage(error);
        });
    }

    /**
     * Executes the recover password service
     */
    cancelRecoverPassword(){
        document.getElementById('forgotPassword').className='forgotPasswordBox hideBox';
        document.getElementById('loginBox').style.display='';
    }

    /**
     * Display the forgot password card
     */
    showForgotPassword(){
        document.getElementById('forgotPassword').className='forgotPasswordBox';
        document.getElementById('loginBox').style.display='none';
    }

    /**
     * Execute the change language service
     */
    changeLanguage(lang : string){
        this.language=lang;
        alert("language changed to:"+this.language);
    }

    /**
     * Update the color and or image according to the values specified
     * @param backgroundcolor  page Body background color
     * @param backgroundimage  page Body background image
     */
    updateScreenBackground(){
        this._loginService.getServerInfo().subscribe((data) =>{
            if(data.backgroundcolor != 'undefined' && data.backgroundcolor != '') {
                document.getElementById('loginBox').style.backgroundColor = data.backgroundcolor;
            }
            if(data.backgroundcolor != 'undefined' && data.backgroundcolor != '') {
                document.getElementById('loginBox').style.backgroundImage = data.backgroundimage;
            }

            //TODO update server info data

        }, (error)=>{

        });
    }

    getErrorMessage(error:any):string{
        let errorObject = JSON.parse(error.response._body);
        let errorMessages:string='';
        errorObject.errors.forEach(e =>{
            errorMessages += e.message;
        })
        return errorMessages;
    }

}