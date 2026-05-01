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

    readonly fieldChange = output<ProvisioningChange>();

    readonly roleBehaviorOptions: RoleBehaviorOption[] = [
        {
            label: 'dotauth.roleBehavior.replace',
            value: 'replace',
            description: 'dotauth.roleBehavior.replace.description'
        },
        {
            label: 'dotauth.roleBehavior.merge',
            value: 'merge',
            description: 'dotauth.roleBehavior.merge.description'
        },
        {
            label: 'dotauth.roleBehavior.addOnly',
            value: 'add-only',
            description: 'dotauth.roleBehavior.addOnly.description'
        },
        {
            label: 'dotauth.roleBehavior.firstOnly',
            value: 'first-only',
            description: 'dotauth.roleBehavior.firstOnly.description'
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
