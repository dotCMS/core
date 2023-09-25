import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { APP_INITIALIZER, CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

// App is our top level component
import { DotPropertiesService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { COMPONENTS } from './components';
import { CUSTOM_MODULES, NGFACES_MODULES } from './modules';
import { ENV_PROVIDERS } from './providers';
import { DotDirectivesModule } from './shared/dot-directives.module';
import { SharedModule } from './shared/shared.module';

const featureFactory = (dotProperties: DotPropertiesService) => () => {
    dotProperties.loadConfig();
};

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
        MarkdownModule.forRoot(),
        DotMessagePipe
    ],
    providers: [
        ENV_PROVIDERS,
        {
            provide: APP_INITIALIZER,
            useFactory: featureFactory,
            deps: [DotPropertiesService],
            multi: true
        }
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule {}
