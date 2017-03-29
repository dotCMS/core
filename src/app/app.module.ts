import { BrowserModule } from '@angular/platform-browser';
import { HttpModule } from '@angular/http';
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
import { ROUTES } from './app.routing';
// App is our top level component
import { AppComponent } from './app-component';
import { APP_RESOLVER_PROVIDERS } from './app.resolver';
import { AppState, InternalStateType } from './app.service';

import {FormsModule, ReactiveFormsModule} from '@angular/forms';

import {InputTextModule} from 'primeng/primeng';
import {PasswordModule} from 'primeng/primeng';
import {CheckboxModule} from 'primeng/primeng';
import {ButtonModule} from 'primeng/primeng';
import {DropdownModule} from 'primeng/primeng';
import {AutoCompleteModule} from 'primeng/primeng';
import {MainCoreLegacyComponent} from './view/components/main-core-legacy/main-core-legacy-component';
import {ToolbarModule} from 'primeng/primeng';
import {DialogModule} from 'primeng/primeng';
import {RadioButtonModule} from 'primeng/primeng';

const NGFACES_MODULES = [
  InputTextModule,
  PasswordModule,
  CheckboxModule,
  RadioButtonModule,
  ButtonModule,
  DropdownModule,
  AutoCompleteModule,
  ToolbarModule,
  DialogModule
];

/*
 * Custom Components
 */
import { COMPONENTS, DIRECTIVES, PIPES } from './components';

import '../styles/main.scss';

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
    ...DIRECTIVES,
  ],
  imports: [ // import Angular's modules
    BrowserModule,
    HttpModule,
    ROUTES,
    FormsModule,
    ReactiveFormsModule,
    ...NGFACES_MODULES,
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
