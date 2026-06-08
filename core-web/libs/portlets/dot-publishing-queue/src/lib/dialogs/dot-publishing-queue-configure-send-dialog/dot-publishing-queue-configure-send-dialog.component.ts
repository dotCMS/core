import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';

import {
    DotMessageService,
    DotPushPublishFiltersService,
    PushOperation
} from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPublishingQueueStore } from '../../dot-publishing-queue-page/store/dot-publishing-queue.store';

type DesignOperation = 'push' | 'remove' | 'pushremove';
type ScheduleMode = 'now' | 'schedule';

const OPERATION_MAP: Record<DesignOperation, PushOperation> = {
    push: 'publish',
    remove: 'expire',
    pushremove: 'publishexpire'
};

@Component({
    selector: 'dot-publishing-queue-configure-send-dialog',
    standalone: true,
    imports: [
        FormsModule,
        ButtonModule,
        DatePickerModule,
        MessageModule,
        SelectModule,
        DotMessagePipe
    ],
    templateUrl: './dot-publishing-queue-configure-send-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPublishingQueueConfigureSendDialogComponent {
    readonly store = inject(DotPublishingQueueStore);
    readonly dialogRef = inject(DynamicDialogRef);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly filtersService = inject(DotPushPublishFiltersService);

    readonly operation = signal<DesignOperation>('push');
    readonly scheduleMode = signal<ScheduleMode>('now');
    readonly publishDate = signal<Date | null>(null);
    readonly expireDate = signal<Date | null>(null);
    readonly setExpire = signal(false);
    readonly selectedEnvironments = signal<string[]>([]);
    readonly selectedFilterKey = signal<string | null>(null);

    readonly filters = toSignal(this.filtersService.get(), { initialValue: [] });

    readonly bundle = computed(() => this.store.pushBundleTarget());
    readonly envOptions = computed(() => this.store.environments());
    readonly envOptionLabel = 'name';
    readonly envOptionValue = 'id';

    readonly showFilter = computed(() => this.operation() !== 'remove');
    readonly showExpireDate = computed(
        () => this.operation() === 'pushremove' || (this.operation() === 'push' && this.setExpire())
    );
    readonly showPublishDate = computed(
        () => this.operation() !== 'remove' && this.scheduleMode() === 'schedule'
    );
    readonly showExpireDateScheduled = computed(
        () => this.operation() === 'remove' && this.scheduleMode() === 'schedule'
    );

    readonly canSubmit = computed(() => {
        if (this.selectedEnvironments().length === 0) {
            return false;
        }
        if (this.showFilter() && !this.selectedFilterKey()) {
            return false;
        }
        return true;
    });

    onSubmit(): void {
        const bundle = this.bundle();
        if (!bundle || !this.canSubmit()) {
            return;
        }

        const apiOperation = OPERATION_MAP[this.operation()];
        const payload = {
            operation: apiOperation,
            environments: this.selectedEnvironments(),
            filterKey: this.selectedFilterKey() ?? 'ForcePush.yml',
            publishDate: this.publishDateIso(),
            expireDate: this.expireDateIso()
        };

        this.store.submitPush(bundle.bundleId, payload, () => this.dialogRef.close());
    }

    onCancel(): void {
        this.dialogRef.close();
    }

    /** Click-handler for the action cards; signature kept terse for template inlining. */
    setOperation(op: DesignOperation): void {
        this.operation.set(op);
        if (op === 'remove') {
            this.selectedFilterKey.set(null);
        }
    }

    setSchedule(mode: ScheduleMode): void {
        this.scheduleMode.set(mode);
    }

    toggleExpire(value: boolean): void {
        this.setExpire.set(value);
    }

    private publishDateIso(): string | undefined {
        if (this.scheduleMode() !== 'schedule' || this.operation() === 'remove') {
            return undefined;
        }
        const d = this.publishDate();
        return d ? this.toOffsetIso(d) : undefined;
    }

    private expireDateIso(): string | undefined {
        const isExpireRequired = this.operation() === 'pushremove' || this.operation() === 'remove';
        const wantsExpire = isExpireRequired || (this.operation() === 'push' && this.setExpire());
        if (!wantsExpire) {
            return undefined;
        }
        const d = this.expireDate();
        return d ? this.toOffsetIso(d) : undefined;
    }

    /** ISO 8601 with timezone offset — matches what `PushBundleForm.validateDateFormat` expects. */
    private toOffsetIso(d: Date): string {
        const pad = (n: number) => n.toString().padStart(2, '0');
        const tzOffsetMin = -d.getTimezoneOffset();
        const sign = tzOffsetMin >= 0 ? '+' : '-';
        const offHrs = pad(Math.floor(Math.abs(tzOffsetMin) / 60));
        const offMin = pad(Math.abs(tzOffsetMin) % 60);
        return (
            `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
            `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}` +
            `${sign}${offHrs}:${offMin}`
        );
    }

    timezoneLabel(): string {
        return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    }

    msg(key: string, ...args: string[]): string {
        return this.dotMessageService.get(key, ...args);
    }
}
