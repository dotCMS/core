import { CommonModule } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';

// App is our top level component
import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';

/*
 * Custom Components
 */
import { COMPONENTS } from './components';
import { ENV_PROVIDERS } from './providers';
import { CUSTOM_MODULES, NGFACES_MODULES } from './modules';
import { SharedModule } from './shared/shared.module';

import { DotDirectivesModule } from './shared/dot-directives.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { MonacoEditorModule } from 'ngx-monaco-editor';
import { MarkdownModule } from 'ngx-markdown';

/**
 * `AppModule` is the main entry point into Angular2's bootstraping process
 */
@NgModule({
    bootstrap: [AppComponent],
    declarations: [AppComponent, ...COMPONENTS],
    imports: [
        ...CUSTOM_MODULES,
        ...NGFACES_MODULES,
        CommonModule,
        BrowserAnimationsModule,
        BrowserModule,
        FormsModule,
        HttpClientModule,
        ReactiveFormsModule,
        AppRoutingModule,
        DotDirectivesModule,
        DotPipesModule,
        SharedModule.forRoot(),
        MonacoEditorModule.forRoot(),
        MarkdownModule.forRoot()
    ],
    providers: [ENV_PROVIDERS],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule {}



