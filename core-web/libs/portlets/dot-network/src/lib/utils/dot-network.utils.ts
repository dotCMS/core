import { DotClusterHealth, DotClusterNodeStatus } from '@dotcms/dotcms-models';

export type DotNetworkTagSeverity = 'success' | 'warn' | 'danger' | 'secondary';

export interface DotNetworkTag {
    readonly severity: DotNetworkTagSeverity;
    readonly labelKey: string;
}

export const NODE_STATUS_TAGS: Record<DotClusterNodeStatus, DotNetworkTag> = {
    UP: { severity: 'success', labelKey: 'network.status.up' },
    LAGGING: { severity: 'warn', labelKey: 'network.status.lagging' },
    DOWN: { severity: 'danger', labelKey: 'network.status.down' }
};

export const HEALTH_TAGS: Record<DotClusterHealth, DotNetworkTag> = {
    GREEN: { severity: 'success', labelKey: 'network.health.green' },
    YELLOW: { severity: 'warn', labelKey: 'network.health.yellow' },
    RED: { severity: 'danger', labelKey: 'network.health.red' },
    UNKNOWN: { severity: 'secondary', labelKey: 'network.health.unknown' }
};

/** A message key and its argument, resolved by `DotMessageService`. */
export interface DotNetworkMessage {
    readonly key: string;
    readonly args: string[];
}

/** Relative time for "Contacted N ago", from the seconds since the last heartbeat. */
export function toElapsedMessage(seconds: number | null): DotNetworkMessage {
    if (seconds === null) {
        return { key: 'network.node.contacted.never', args: [] };
    }

    const value = Math.max(0, Math.floor(seconds));

    if (value < 60) {
        return value === 1
            ? { key: 'network.time.second-ago', args: [] }
            : { key: 'network.time.seconds-ago', args: [String(value)] };
    }

    const minutes = Math.floor(value / 60);
    if (minutes < 60) {
        return minutes === 1
            ? { key: 'network.time.minute-ago', args: [] }
            : { key: 'network.time.minutes-ago', args: [String(minutes)] };
    }

    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
        return hours === 1
            ? { key: 'network.time.hour-ago', args: [] }
            : { key: 'network.time.hours-ago', args: [String(hours)] };
    }

    const days = Math.floor(hours / 24);

    return days === 1
        ? { key: 'network.time.day-ago', args: [] }
        : { key: 'network.time.days-ago', args: [String(days)] };
}
