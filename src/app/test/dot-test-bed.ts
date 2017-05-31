import { ConnectionBackend, RequestOptions, BaseRequestOptions, Http } from '@angular/http';
import { Logger } from 'angular2-logger/core';
import { MockBackend } from '@angular/http/testing';
import { TestBed, TestModuleMetadata, ComponentFixture } from '@angular/core/testing';
import { Type, Provider, Injector, ReflectiveInjector, Component } from '@angular/core';
import { ApiRoot } from '../api/persistence/ApiRoot';
import { BrowserUtil } from '../api/util/browser-util';
import { Config } from '../api/util/config';
import { CoreWebService } from '../api/services/core-web-service';
import { DotcmsConfig } from '../api/services/system/dotcms-config';
import { DotcmsEventsService } from '../api/services/dotcms-events-service';
import { LoggerService } from '../api/services/logger.service';
import { StringUtils } from '../api/util/string.utils';
import { UserModel } from '../api/auth/UserModel';
import { ConfirmDialogModule } from 'primeng/components/confirmdialog/confirmdialog';
import { ConfirmationService } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { NGFACES_MODULES } from '../modules';

@Component({
    selector: 'p-confirmDialog',
    template: ''
})
class FakeConfirmDialogComponent {
}

export class DOTTestBed {

    private static DEFAULT_CONFIG = {
        imports: [...NGFACES_MODULES, FormsModule],
        providers: [
            {provide: ConnectionBackend, useClass: MockBackend},
            {provide: RequestOptions, useClass: BaseRequestOptions},
            ApiRoot,
            BrowserUtil,
            Config,
            ConfirmationService,
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

        console.log('config', config);
        TestBed.configureTestingModule(config);

        TestBed.overrideModule(ConfirmDialogModule, {
            set: {
                declarations: [FakeConfirmDialogComponent],
                exports: [FakeConfirmDialogComponent]
            }
        });
        TestBed.compileComponents();
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