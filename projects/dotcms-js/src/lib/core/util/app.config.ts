import { Injectable } from '@angular/core';
/**
 * Configuration class for dotcms-js. You can extend this class to overide one to many config options and inject
 * the your AppConfig in the app.module ts file of your application
 */
@Injectable()
export class AppConfig {
    public iconPath = './src/assets/images/icons';
    public dotCMSURLKey = 'siteURLJWT';
    constructor() {}
}
