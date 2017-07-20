import { BrowserModule } from '@angular/platform-browser';
import { HttpModule, JsonpModule } from '@angular/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import {
  NgModule,
  ApplicationRef
} from '@angular/core';
import {
  removeNgStyles,
  createNewHosts,
  createInputTransfer
} from '@angularclass/hmr';

/*
 * Platform and Environment providers/directives/pipes
 */
import { ENV_PROVIDERS } from './environment';
// App is our top level component
import { AppComponent } from './app-component';
import { AppRoutingModule } from './app-routing.module';
import { APP_RESOLVER_PROVIDERS } from './app.resolver';
import { AppState, InternalStateType } from './app.service';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

/*
 * Custom Components
 */
import { COMPONENTS, DIRECTIVES, PIPES } from './components';

import '../styles/main.scss';

import { NGFACES_MODULES } from './modules';
import { ActionButtonModule } from './view/components/_common/action-button/action-button.module';
import { FieldValidationMessageModule } from './view/components/_common/field-validation-message/file-validation-message.module';
import { ListingDataTableModule } from './view/components/listing-data-table/listing-data-table.module';
import { SiteSelectorModule } from './view/components/_common/site-selector/site-selector.module';

// Application wide providers
const APP_PROVIDERS = [
  ...APP_RESOLVER_PROVIDERS,
  AppState
];

type StoreType = {
  state: InternalStateType,
  restoreInputValues: () => void,
  disposeOldHosts: () => void
};

/**
 * `AppModule` is the main entry point into Angular2's bootstraping process
 */
@NgModule({
  bootstrap: [ AppComponent ],
  declarations: [
    AppComponent,
    ...PIPES,
    ...COMPONENTS,
    ...DIRECTIVES
  ],
  imports: [ // import Angular's modules
    ...NGFACES_MODULES,
    ActionButtonModule,
    BrowserAnimationsModule,
    BrowserModule,
    BrowserModule,
    FieldValidationMessageModule,
    FieldValidationMessageModule,
    FormsModule,
    FormsModule,
    HttpModule,
    HttpModule,
    JsonpModule,
    JsonpModule,
    ListingDataTableModule,
    ListingDataTableModule,
    ReactiveFormsModule,
    SiteSelectorModule,
    // AppRoutingModule should always be the last one
    AppRoutingModule
  ],
  providers: [ // expose our Services and Providers into Angular's dependency injection
    ENV_PROVIDERS,
    APP_PROVIDERS
  ]
})
export class AppModule {

  constructor(
    public appRef: ApplicationRef,
    public appState: AppState
  ) {}

  public hmrOnInit(store: StoreType): void {
    if (!store || !store.state) {
      return;
    }

    // tslint:disable-next-line:no-console
    console.log('HMR store', JSON.stringify(store, null, 2));
    // set state
    this.appState._state = store.state;
    // set input values
    if ('restoreInputValues' in store) {
      let restoreInputValues = store.restoreInputValues;
      setTimeout(restoreInputValues);
    }

    this.appRef.tick();
    delete store.state;
    delete store.restoreInputValues;
  }

  public hmrOnDestroy(store: StoreType): void {
    const cmpLocation = this.appRef.components.map((cmp) => cmp.location.nativeElement);
    // save state
    const state = this.appState._state;
    store.state = state;
    // recreate root elements
    store.disposeOldHosts = createNewHosts(cmpLocation);
    // save input values
    store.restoreInputValues  = createInputTransfer();
    // remove styles
    removeNgStyles();
  }

  public hmrAfterDestroy(store: StoreType): void {
    // display new elements
    store.disposeOldHosts();
    delete store.disposeOldHosts;
  }

}
