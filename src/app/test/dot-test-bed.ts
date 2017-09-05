import { ConnectionBackend, RequestOptions, BaseRequestOptions, Http } from '@angular/http';
import { Logger } from 'angular2-logger/core';
import { MockBackend } from '@angular/http/testing';
import { TestBed, TestModuleMetadata, ComponentFixture } from '@angular/core/testing';
import { Type, Provider, Injector, ReflectiveInjector, Component, LOCALE_ID } from '@angular/core';
import {
    ApiRoot,
    BrowserUtil,
    Config,
    CoreWebService,
    DotcmsConfig,
    DotcmsEventsService,
    LoggerService,
    StringUtils,
    UserModel
} from 'dotcms-js/dotcms-js';
import { ConfirmDialogModule } from 'primeng/components/confirmdialog/confirmdialog';
import { ConfirmationService } from 'primeng/primeng';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NGFACES_MODULES } from '../modules';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'p-confirmDialog',
    template: ''
})
class FakeConfirmDialogComponent {}

export class DOTTestBed {
    private static DEFAULT_CONFIG = {
        imports: [
            ...NGFACES_MODULES,
            CommonModule,
            FormsModule,
            ReactiveFormsModule
        ],
        providers: [
            { provide: ConnectionBackend, useClass: MockBackend },
            { provide: RequestOptions, useClass: BaseRequestOptions },
            { provide: LOCALE_ID, useValue: {} },
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
        for (const property in DOTTestBed.DEFAULT_CONFIG) {
            if (config[property]) {
                DOTTestBed.DEFAULT_CONFIG[property]
                    .filter(provider => !config[property].includes(provider))
                    .forEach(item => config[property].unshift(item));
            } else {
                config[property] = DOTTestBed.DEFAULT_CONFIG[property];
            }
        }

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
        const finalProviders = [];

        DOTTestBed.DEFAULT_CONFIG.providers.forEach(provider => finalProviders.push(provider));

        providers.forEach(provider => finalProviders.push(provider));

        return ReflectiveInjector.resolveAndCreate(finalProviders, parent);
    }
}
