import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { definePreset } from '@primeng/themes';
import Aura from '@primeng/themes/aura';
import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

// App is our top level component
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { providePrimeNG } from 'primeng/config';

import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { COMPONENTS } from './components';
import { CUSTOM_MODULES, NGFACES_MODULES } from './modules';
import { ENV_PROVIDERS } from './providers';
import { DotDirectivesModule } from './shared/dot-directives.module';
import { SharedModule } from './shared/shared.module';

const MyPreset = definePreset(Aura, {
    semantic: {
        primary: {
            50: '{blue.50}',
            100: '{blue.100}',
            200: '{blue.200}',
            300: '{blue.300}',
            400: '{blue.400}',
            500: '{blue.500}',
            600: '{blue.600}',
            700: '{blue.700}',
            800: '{blue.800}',
            900: '{blue.900}',
            950: '{blue.950}'
        },
        colorScheme: {
            light: {
                primary: {
                    color: '{blue.900}',
                    inverseColor: '#ffffff',
                    hoverColor: '{blue.500}',
                    activeColor: '{blue.600}'
                },
                highlight: {
                    background: '{blue.50}',
                    focusBackground: '{blue.50}',
                    color: '{blue.800}',
                    focusColor: '{blue.800}'
                }
            },
        }
    }
});


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
        DotSafeHtmlPipe,
        SharedModule.forRoot(),
        MonacoEditorModule,
        MarkdownModule.forRoot(),
        DotMessagePipe
    ],
    providers: [ENV_PROVIDERS,
        provideAnimationsAsync(),
        providePrimeNG({
            theme: {
                preset: MyPreset,
                options: {
                    darkModeSelector: false
                }
            }
        })
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule {}
