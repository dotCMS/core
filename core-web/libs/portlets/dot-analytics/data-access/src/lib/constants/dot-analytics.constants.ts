export const TIME_RANGE_OPTIONS = {
    today: 'today',
    yesterday: 'yesterday',
    last7days: 'last7days',
    last30days: 'last30days',
    custom: 'custom'
} as const;

/** Reverse mapping for Internal → URL-friendly */
export const TIME_RANGE_CUBEJS_MAPPING = {
    today: 'today',
    yesterday: 'yesterday',
    last7days: 'from 7 days ago to now',
    last30days: 'from 30 days ago to now'
} as const;

/** Dashboard tab identifiers */
export const DASHBOARD_TABS = {
    pageview: 'pageview',
    conversions: 'conversions'
} as const;

export type DashboardTab = (typeof DASHBOARD_TABS)[keyof typeof DASHBOARD_TABS];

/** Dashboard tab configuration */
export interface DashboardTabConfig {
    id: DashboardTab;
    label: string;
}

/** Ordered list of dashboard tabs */
export const DASHBOARD_TAB_LIST: DashboardTabConfig[] = [
    { id: DASHBOARD_TABS.pageview, label: 'analytics.dashboard.tabs.pageview' },
    { id: DASHBOARD_TABS.conversions, label: 'analytics.dashboard.tabs.conversions' }
];

/**
 * Chart color palette
 */
export const CHART_COLORS = {
    primary: '#3B82F6',
    primaryBackground: 'rgba(59, 130, 246, 0.1)',
    secondary: '#10B981',
    secondaryBackground: 'rgba(16, 185, 129, 0.1)',
    gray: '#E5E7EB'
};
