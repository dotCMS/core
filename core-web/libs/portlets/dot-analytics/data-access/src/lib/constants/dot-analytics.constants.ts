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
