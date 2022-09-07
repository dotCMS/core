import { NgModule } from '@angular/core';
import { DotContainerPropertiesComponent } from '@portlets/dot-containers/container-create/dot-container-properties/dot-container-properties.component';
import { CommonModule } from '@angular/common';
import { InplaceModule } from 'primeng/inplace';
import { SharedModule } from 'primeng/api';
import { InputTextModule } from 'primeng/inputtext';

@NgModule({
    declarations: [DotContainerPropertiesComponent],
    exports: [DotContainerPropertiesComponent],
    imports: [CommonModule, InplaceModule, SharedModule, InputTextModule]
})
export class DotContainerPropertiesModule {}
