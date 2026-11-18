import { DotMessageService } from '@dotcms/data-access';
import { PrincipalConfiguration } from '@dotcms/ui';

/**
 * A `dot-empty-container` configuration, with the portlet's conventions filled in.
 *
 * Ten of these are built across the five tabs, every one of them repeating the same icon set
 * and the same `messageService.get` per string. Sibling portlets inline the literal because
 * they have one or two; at ten, the convention needs somewhere to live so that changing it
 * does not mean editing five components.
 *
 * Takes message keys and resolves them here, so the keys stay visible at the call site.
 */
export function toEmptyStateConfig(
    messageService: DotMessageService,
    keys: { title: string; subtitle?: string; icon: string }
): PrincipalConfiguration {
    return {
        title: messageService.get(keys.title),
        ...(keys.subtitle ? { subtitle: messageService.get(keys.subtitle) } : {}),
        icon: keys.icon,
        iconStyle: 'material-symbols-rounded'
    };
}
