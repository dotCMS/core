import { AppComponent } from './app.component';
import { DotAlertConfirmComponent } from './view/components/_common/dot-alert-confirm/dot-alert-confirm';
import { DotThemeSelectorDropdownComponent } from './view/components/dot-theme-selector-dropdown/dot-theme-selector-dropdown.component';
import { GlobalSearchComponent } from './view/components/global-search/global-search';
import { DotLogOutContainerComponent } from './view/components/login/dot-logout-container-component/dot-log-out-container';
import { DotLoginPageComponent } from './view/components/login/main/dot-login-page.component';
import { MainCoreLegacyComponent } from './view/components/main-core-legacy/main-core-legacy-component';
import { MainComponentLegacyComponent } from './view/components/main-legacy/main-legacy.component';

// Non-standalone components (traditional NgModule components)
export const COMPONENTS = [
    MainCoreLegacyComponent,
    DotLoginPageComponent,
    DotLogOutContainerComponent,
    GlobalSearchComponent
];

// Standalone components (migrated to standalone)
export const STANDALONE_COMPONENTS = [
    AppComponent,
    MainComponentLegacyComponent,
    DotAlertConfirmComponent,
    DotThemeSelectorDropdownComponent
];
