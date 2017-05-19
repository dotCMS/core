import { ConnectionBackend, RequestOptions, BaseRequestOptions, Http } from '@angular/http';
import { CoreWebService } from '../../services/core-web-service';
import { Logger } from 'angular2-logger/core';
import { LoggerService } from '../../services/logger.service';
import { BrowserUtil } from '../browser-util';
import { ApiRoot } from '../../persistence/ApiRoot';
import { Config } from '../config';
import { UserModel } from '../../auth/UserModel';
import { StringUtils } from '../string.utils';
import { DotcmsConfig } from '../../services/system/dotcms-config';
import { MockBackend } from '@angular/http/testing';
import { TestBed, TestModuleMetadata, ComponentFixture } from '@angular/core/testing';
import { Type, Provider, Injector, ReflectiveInjector } from '@angular/core';
import { DotcmsEventsService } from '../../services/dotcms-events-service';

export class DOTTestBed {

    private static DEFAULT_CONFIG = {
        providers: [
            {provide: ConnectionBackend, useClass: MockBackend},
            {provide: RequestOptions, useClass: BaseRequestOptions},
            ApiRoot,
            BrowserUtil,
            Config,
            CoreWebService,
            DotcmsConfig,
            DotcmsEventsService,
            Http,
            Logger,
            LoggerService,
            StringUtils,
            UserModel
        ]
    };

    public static configureTestingModule(config: TestModuleMetadata): void {

        // tslint:disable-next-line:forin
        for (let property in DOTTestBed.DEFAULT_CONFIG) {
            if (config[property]) {
                DOTTestBed.DEFAULT_CONFIG[property]
                    .filter( provider => !config[property].includes(provider))
                    .forEach( item => config[property].unshift(item));
            } else {
                config[property] = DOTTestBed.DEFAULT_CONFIG[property];
            }
        }

        TestBed.configureTestingModule(config);
    }

    public static createComponent<T>(component: Type<T>): ComponentFixture<T> {
        return TestBed.createComponent(component);
    }

    public static resolveAndCreate(providers: Provider[], parent?: Injector): ReflectiveInjector {
        let finalProviders = [];

        DOTTestBed.DEFAULT_CONFIG.providers
            .forEach( provider => finalProviders.push(provider));

        providers.forEach( provider => finalProviders.push(provider));

        return ReflectiveInjector.resolveAndCreate(finalProviders, parent);
    }
}