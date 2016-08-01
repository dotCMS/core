import { DotcmsConfig } from './dotcms-config';
import { IframeLegacyComponent } from '../../../view/components/common/iframe-legacy/IframeLegacyComponent';
import { Observable } from 'rxjs/Rx';
import { Routes } from '@ngrx/router';
import { RuleEngineContainer } from '../../../view/components/rule-engine/rule-engine.container';

export class AppConfigurationService {

    private menus: Array<any>;

    private mapComponents;

    /**
     * Default constructor of the service.
     */
    constructor() {
        this.mapComponents = {
            'RULES_ENGINE_PORTLET': RuleEngineContainer,
        };
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
                // TODO: do this more elegant
                // TODO: this is bad, we shouldn't be create the route here, a service should only return the data.
                let routes: Routes = [];
                let mapPaths = {};
                let dotcmsConfig = new DotcmsConfig(configurationItems.entity);
                if (configurationItems.errors.length > 0) {
                    console.log(configurationItems.errors[0].message);
                } else {
                    configurationItems.entity.menu.forEach((item) => {
                        item.menuItems.forEach(subMenuItem => {
                            if (subMenuItem.angular) {
                                routes.push({
                                    component: this.mapComponents[subMenuItem.id],
                                    path: subMenuItem.url,
                                });
                            } else {
                                mapPaths[subMenuItem.id] = subMenuItem.url + '&in_frame=true&frame=detailFrame';
                            }
                        });
                    });
                }
                routes.push({
                    component: IframeLegacyComponent,
                    path: '/portlet/:id',
                });
                observer.next({
                    dotcmsConfig: dotcmsConfig,
                    menuItems: {
                        mapPaths: mapPaths,
                        navigationItems: dotcmsConfig.getNavigationMenu(),
                    },
                    routes: routes,
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
