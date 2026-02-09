import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

// App is our top level component
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { AppRoutingModule } from './app-routing.module';
import { COMPONENTS, STANDALONE_COMPONENTS } from './components';
import { NGFACES_MODULES } from './modules';
import { ENV_PROVIDERS } from './providers';
import { DotDirectivesModule } from './shared/dot-directives.module';
import { SharedModule } from './shared/shared.module';

@NgModule({
    declarations: [...COMPONENTS],
    imports: [
        ...NGFACES_MODULES,
        ...STANDALONE_COMPONENTS,
        CommonModule,
        BrowserAnimationsModule,
        BrowserModule,
        FormsModule,
        HttpClientModule,
        ReactiveFormsModule,
        AppRoutingModule,
        DotDirectivesModule,
        DotSafeHtmlPipe,
        SharedModule.forRoot(),
        MonacoEditorModule,
        MarkdownModule.forRoot(),
        DotMessagePipe
    ],
    providers: [ENV_PROVIDERS],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule {}
