import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import {
    DotAuthGroupMapping,
    DotAuthProvisioningConfig,
    DotAuthRoleBehavior
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

export interface ProvisioningChange {
    path: string;
    value: unknown;
}

interface RoleBehaviorOption {
    label: string;
    value: DotAuthRoleBehavior;
    description: string;
}

@Component({
    selector: 'dot-auth-provisioning',
    standalone: true,
    host: {
        class: 'dot-auth-provisioning'
    },
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        ToggleSwitchModule,
        TooltipModule,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    styleUrl: '../_dot-auth-shared.scss',
    templateUrl: './dot-auth-provisioning.component.html'
})
export class DotAuthProvisioningComponent {
    readonly config = input.required<DotAuthProvisioningConfig & Record<string, unknown>>();
    readonly syncLabel = input.required<string>();
    readonly syncKey = input<string>('syncOnLogin');
    // ponytail: OIDC has no backend key for sync-on-login yet; hide the toggle there
    readonly showSync = input<boolean>(true);

    readonly fieldChange = output<ProvisioningChange>();

    readonly roleBehaviorOptions: RoleBehaviorOption[] = [
        {
            label: 'dotauth.roleBehavior.syncAll',
            value: 'sync-all',
            description: 'dotauth.roleBehavior.syncAll.description'
        },
        {
            label: 'dotauth.roleBehavior.idpOnly',
            value: 'idp-only',
            description: 'dotauth.roleBehavior.idpOnly.description'
        },
        {
            label: 'dotauth.roleBehavior.staticOnly',
            value: 'static-only',
            description: 'dotauth.roleBehavior.staticOnly.description'
        },
        {
            label: 'dotauth.roleBehavior.additive',
            value: 'additive',
            description: 'dotauth.roleBehavior.additive.description'
        },
        {
            label: 'dotauth.roleBehavior.none',
            value: 'none',
            description: 'dotauth.roleBehavior.none.description'
        }
    ];

    defaultRolesText(): string {
        return (this.config().defaultRoles ?? []).join(', ');
    }

    onChange(field: string, value: unknown): void {
        this.fieldChange.emit({ path: field, value });
    }

    onDefaultRolesChange(value: string): void {
        this.fieldChange.emit({
            path: 'defaultRoles',
            value: value
                .split(',')
                .map((r) => r.trim())
                .filter(Boolean)
        });
    }

    onAddMapping(): void {
        const current = [...this.config().groupMappings];
        current.push({ idpGroup: '', dotcmsRole: '' });
        this.fieldChange.emit({ path: 'groupMappings', value: current });
    }

    onRemoveMapping(index: number): void {
        const current = [...this.config().groupMappings];
        current.splice(index, 1);
        this.fieldChange.emit({ path: 'groupMappings', value: current });
    }

    onMappingChange(index: number, field: keyof DotAuthGroupMapping, value: string): void {
        const current = this.config().groupMappings.map((m) => ({ ...m }));
        current[index][field] = value;
        this.fieldChange.emit({ path: 'groupMappings', value: current });
    }
}
