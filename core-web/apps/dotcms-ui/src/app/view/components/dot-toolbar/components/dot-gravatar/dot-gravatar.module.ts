import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotGravatarComponent } from './dot-gravatar.component';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotGravatarService } from '@services/dot-gravatar-service';

@NgModule({
    imports: [CommonModule, DotAvatarModule],
    declarations: [DotGravatarComponent],
    exports: [DotGravatarComponent],
    providers: [DotGravatarService]
})
export class DotGravatarModule {}
