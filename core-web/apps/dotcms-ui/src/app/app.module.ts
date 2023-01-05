import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

// App is our top level component
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

// Custom Components
import { COMPONENTS } from './components';
import { CUSTOM_MODULES, NGFACES_MODULES } from './modules';
import { ENV_PROVIDERS } from './providers';
import { SharedModule } from './shared/shared.module';

import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { MarkdownModule } from 'ngx-markdown';
import { DotDirectivesModule } from './shared/dot-directives.module';

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
        MonacoEditorModule,
        MarkdownModule.forRoot()
    ],
    providers: [ENV_PROVIDERS],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule {}
