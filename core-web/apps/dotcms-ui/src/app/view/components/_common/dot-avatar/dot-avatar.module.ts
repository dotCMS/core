import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotAvatarComponent } from './dot-avatar.component';

@NgModule({
    imports: [CommonModule],
    declarations: [DotAvatarComponent],
    exports: [DotAvatarComponent]
})
export class DotAvatarModule {}
