import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';

import { DotAuthGroupMapping, DotAuthProvisioningConfig, DotAuthRoleBehavior } from '@dotcms/dotcms-models';
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
    template: `
        <div class="field-grid">
            <div class="field">
                <label>
                    {{ 'dotauth.field.autoProvision' | dm }}
                    <i
                        class="label-tip pi pi-question-circle"
                        pTooltip="Create a dotCMS user on first successful sign-in."
                        tooltipPosition="top"></i>
                </label>
                <div class="flex items-center gap-2.5" style="height: 36px">
                    <p-toggleswitch
                        [ngModel]="config().autoProvision"
                        (ngModelChange)="onChange('autoProvision', $event)" />
                </div>
            </div>
            <div class="field">
                <label>
                    {{ syncLabel() }}
                    <i
                        class="label-tip pi pi-question-circle"
                        pTooltip="Refresh name, email, and group memberships on each sign-in or exchange."
                        tooltipPosition="top"></i>
                </label>
                <div class="flex items-center gap-2.5" style="height: 36px">
                    <p-toggleswitch
                        [ngModel]="config()[syncKey()]"
                        (ngModelChange)="onChange(syncKey(), $event)" />
                </div>
            </div>
            <div class="field full">
                <label>
                    {{ 'dotauth.field.defaultRoles' | dm }}
                    <i
                        class="label-tip pi pi-question-circle"
                        pTooltip="Assigned to every provisioned user. Group mappings below add additional roles on top of these."
                        tooltipPosition="top"></i>
                </label>
                <input
                    pInputText
                    [ngModel]="defaultRolesText()"
                    (ngModelChange)="onDefaultRolesChange($event)" />
            </div>
        </div>

        <!-- Role behavior -->
        <div>
            <div class="text-sm font-medium mb-1">{{ 'dotauth.field.roleBehavior' | dm }}</div>
            <div class="text-[0.8125rem] text-[var(--text-color-secondary)] mb-2.5">
                {{ 'dotauth.field.roleBehavior.detail' | dm }}
            </div>
            <div class="role-behavior-grid">
                @for (opt of roleBehaviorOptions; track opt.value) {
                    <label
                        class="role-behavior-card"
                        [class.selected]="config().roleBehavior === opt.value">
                        <input
                            type="radio"
                            [checked]="config().roleBehavior === opt.value"
                            (change)="onChange('roleBehavior', opt.value)" />
                        <div class="flex flex-col gap-0.5">
                            <span class="text-[0.8125rem] font-semibold">{{ opt.label }}</span>
                            <span class="text-xs text-[var(--text-color-secondary)] leading-snug">
                                {{ opt.description }}
                            </span>
                        </div>
                    </label>
                }
            </div>
        </div>

        <!-- Group mappings -->
        <div>
            <div class="flex items-center justify-between">
                <div>
                    <div class="text-sm font-medium">{{ 'dotauth.field.groupMappings' | dm }}</div>
                    <div class="text-[0.8125rem] text-[var(--text-color-secondary)]">
                        {{ 'dotauth.field.groupMappings.detail' | dm }}
                    </div>
                </div>
                <p-button
                    size="small"
                    [outlined]="true"
                    icon="pi pi-plus"
                    [label]="'dotauth.action.add-mapping' | dm"
                    (onClick)="onAddMapping()" />
            </div>
            @if (config().groupMappings.length === 0) {
                <div class="empty-placeholder mt-2">{{ 'dotauth.empty.mappings' | dm }}</div>
            } @else {
                <div class="mt-2 rounded-md border border-[var(--surface-border)]">
                    <table class="w-full text-sm">
                        <thead>
                            <tr class="border-b border-[var(--surface-border)]">
                                <th
                                    class="p-2.5 text-left text-xs font-semibold text-[var(--text-color-secondary)]">
                                    {{ 'dotauth.field.idpGroup' | dm }}
                                </th>
                                <th
                                    class="p-2.5 text-left text-xs font-semibold text-[var(--text-color-secondary)]">
                                    {{ 'dotauth.field.dotcmsRole' | dm }}
                                </th>
                                <th class="w-px"></th>
                            </tr>
                        </thead>
                        <tbody>
                            @for (mapping of config().groupMappings; track $index; let i = $index) {
                                <tr class="border-b border-[var(--surface-border)] last:border-b-0">
                                    <td class="p-2">
                                        <input
                                            pInputText
                                            class="w-full"
                                            [ngModel]="mapping.idpGroup"
                                            (ngModelChange)="onMappingChange(i, 'idpGroup', $event)"
                                            placeholder="dotcms-admins" />
                                    </td>
                                    <td class="p-2">
                                        <input
                                            pInputText
                                            class="w-full"
                                            [ngModel]="mapping.dotcmsRole"
                                            (ngModelChange)="onMappingChange(i, 'dotcmsRole', $event)"
                                            placeholder="CMS Administrator" />
                                    </td>
                                    <td class="p-2">
                                        <p-button
                                            size="small"
                                            [text]="true"
                                            severity="danger"
                                            icon="pi pi-trash"
                                            (onClick)="onRemoveMapping(i)" />
                                    </td>
                                </tr>
                            }
                        </tbody>
                    </table>
                </div>
            }
        </div>
    `
})
export class DotAuthProvisioningComponent {
    readonly config = input.required<DotAuthProvisioningConfig & Record<string, unknown>>();
    readonly syncLabel = input.required<string>();
    readonly syncKey = input<string>('syncOnLogin');

    readonly change = output<ProvisioningChange>();

    readonly roleBehaviorOptions: RoleBehaviorOption[] = [
        {
            label: 'Replace',
            value: 'replace',
            description: "Set the user's roles to exactly the mapped roles + defaults each login."
        },
        {
            label: 'Merge with defaults',
            value: 'merge',
            description:
                'Add mapped roles + defaults; remove other IdP-derived roles no longer present.'
        },
        {
            label: 'Add only',
            value: 'add-only',
            description: 'Add mapped roles to the user; never remove roles.'
        },
        {
            label: 'On first provision only',
            value: 'first-only',
            description:
                'Apply mapped roles when the user is first created; never modify roles on subsequent logins.'
        }
    ];

    defaultRolesText(): string {
        return (this.config().defaultRoles ?? []).join(', ');
    }

    onChange(field: string, value: unknown): void {
        this.change.emit({ path: field, value });
    }

    onDefaultRolesChange(value: string): void {
        this.change.emit({
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
        this.change.emit({ path: 'groupMappings', value: current });
    }

    onRemoveMapping(index: number): void {
        const current = [...this.config().groupMappings];
        current.splice(index, 1);
        this.change.emit({ path: 'groupMappings', value: current });
    }

    onMappingChange(index: number, field: keyof DotAuthGroupMapping, value: string): void {
        const current = this.config().groupMappings.map((m) => ({ ...m }));
        current[index][field] = value;
        this.change.emit({ path: 'groupMappings', value: current });
    }
}
