import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotGravatarService } from '@dotcms/app/api/services/dot-gravatar-service';

import { DotGravatarComponent } from './dot-gravatar.component';

@NgModule({
    imports: [CommonModule, DotAvatarModule],
    declarations: [DotGravatarComponent],
    exports: [DotGravatarComponent],
    providers: [DotGravatarService]
})
export class DotGravatarModule {}
