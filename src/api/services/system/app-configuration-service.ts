import { DotcmsConfig } from './dotcms-config';
import { IframeLegacyComponent } from '../../../view/components/common/iframe-legacy/IframeLegacyComponent';
import { Observable } from 'rxjs/Rx';
import { Routes } from '@ngrx/router';
import { RuleEngineContainer } from '../../../view/components/rule-engine/rule-engine.container';


import {RuleEngineContainer} from '../../view/components/rule-engine/rule-engine.container';
import {IframeLegacyComponent} from '../../view/components/common/iframe-legacy/IframeLegacyComponent';
import {MainComponent} from "../../../view/components/common/main-component/main-component";
import {LoginPageComponent} from "../../../view/components/common/login/login-page-component";
import {ForgotPasswordContainer} from "../../../view/components/common/login/forgot-password-component/forgot-password-container";
import {LoginContainer} from "../../../view/components/common/login/login-component/login-container";
import {ResetPasswordContainer} from "../../../view/components/common/login/reset-password-component/reset-password-container";



export class AppConfigurationService {

    /**
     * Default constructor of the service.
     */
    constructor() {

    }

    /**
     * Transforms the response sent by the App Configuration end-point into
     * a useful easier to read object that other components can inject in
     * order to access system configuration parameters.
     *
     * @returns {any} The Observable containing useful dotCMS configuration
     *          data.
     */
   public getConfigProperties(): Observable<any> {
        return Observable.create(observer => {
            this.getConfig().subscribe((configurationItems) => {

                observer.next({
                    dotcmsConfig: new DotcmsConfig(configurationItems.entity)
                });
                observer.complete();
            });
        });
   }

    /**
     * Returns the configuration parameters for this Web App through the
     * configuration end-point.
     *
     * @returns {any} A JSON response with the app configuration parameters.
     */
   private getConfig(): Observable<any> {

        return Observable.create(observer => {
            let oReq = new XMLHttpRequest();

            oReq.onreadystatechange = (() => {
                if (oReq.status === 401) {
                    // if the user is not loggedIn will be here ;
                    observer.next(JSON.parse('{"errors":[],"entity":[]}'));
                    observer.complete();
                }else if (oReq.status === 400 || oReq.status === 500) {
                    console.log('Error ' + oReq.status + ': ' + oReq.statusText);
                }else if (oReq.readyState === XMLHttpRequest.DONE) {
                    observer.next(JSON.parse(oReq.response));
                    observer.complete();
                }
            });
            oReq.open('GET', '/api/v1/appconfiguration');
            oReq.send();
        });
   }

 }
