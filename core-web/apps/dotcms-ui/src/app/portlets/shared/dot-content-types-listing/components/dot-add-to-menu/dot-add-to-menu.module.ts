import { NgModule } from '@angular/core';

import { DotAddToMenuComponent } from './dot-add-to-menu.component';

import { DotAddToMenuService } from '../../../../../api/services/add-to-menu/add-to-menu.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DotNavigationService } from '../../../../../view/components/dot-navigation/services/dot-navigation.service';

@NgModule({
    imports: [DotAddToMenuComponent],
    exports: [DotAddToMenuComponent],
    providers: [DotAddToMenuService, DotMenuService, DotNavigationService]
})
export class DotAddToMenuModule {}
