import { JsonObject } from '@angular-devkit/core';
import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';

import { AnalyticsQueryExample } from './store/dot-analytics-search.store';

const BASE_MONACO_EDITOR_OPTIONS: MonacoEditorConstructionOptions = {
    theme: 'vs',
    minimap: {
        enabled: false
    },
    cursorBlinking: 'solid',
    overviewRulerBorder: false,
    mouseWheelZoom: false,
    lineNumbers: 'on',
    roundedSelection: false,
    automaticLayout: true,
    fixedOverflowWidgets: true,
    language: 'json',
    fontSize: 12
};

export const ANALYTICS_MONACO_EDITOR_OPTIONS: MonacoEditorConstructionOptions = {
    ...BASE_MONACO_EDITOR_OPTIONS
};

export const ANALYTICS_RESULTS_MONACO_EDITOR_OPTIONS: MonacoEditorConstructionOptions = {
    ...BASE_MONACO_EDITOR_OPTIONS,
    readOnly: true
};

export const isValidJson = (jsonString: string): boolean | JsonObject => {
    try {
        return JSON.parse(jsonString);
    } catch {
        return false;
    }
};

export const AnalyticsQueryExamples: AnalyticsQueryExample[] = [
    {
        title: 'analytics.search.query.by.hits',
        query: `
{
   "measures": [
      "request.totalRequest"
   ],
   "dimensions": [
      "request.pageTitle",
      "request.url"
   ],
   "order": {
      "request.totalRequest": "desc"
   }
}`
    },
    {
        title: 'analytics.search.query.popular.pages',
        query: `
{
   "measures": [
      "request.totalRequest",
      "request.totalSessions"
   ],
   "dimensions": [
      "request.pageTitle",
      "request.url"
   ],
   "order": {
      "request.totalRequest": "desc"
   },
   "limit": 10
}`
    },
    {
        title: 'analytics.search.query.by.total.sessions',
        query: `
{
   "measures": [
      "request.totalRequest",
      "request.totalUsers"
   ],
   "dimensions": [
      "request.domain"
   ],
   "order": {
      "request.totalRequest": "desc"
   }
}`
    },
    {
        title: 'analytics.search.query.user.agents',
        query: `
{
   "measures": [
      "request.totalUsers",
      "request.totalSessions"
   ],
   "dimensions": [
      "request.userAgent"
   ],
   "order": {
      "request.totalUsers": "desc"
   },
   "limit": 20
}`
    },
    {
        title: 'analytics.search.query.by.time.frame',
        query: `
{
   "measures": [
      "request.totalRequest",
      "request.totalSessions"
   ],
   "timeDimensions": [
      {
         "dimension": "request.createdAt",
         "dateRange": "last 7 days",
         "granularity": "day"
      }
   ],
   "order": {
      "request.createdAt": "asc"
   }
}`
    },
    {
        title: 'analytics.search.query.utm.campaigns',
        query: `
{
   "measures": [
      "request.totalRequest",
      "request.totalUsers"
   ],
   "dimensions": [
      "request.utmCampaign",
      "request.utmSource",
      "request.utmMedium"
   ],
   "filters": [
      {
         "member": "request.utmCampaign",
         "operator": "set"
      }
   ],
   "order": {
      "request.totalRequest": "desc"
   }
}`
    },
    {
        title: 'analytics.search.query.referrer.analysis',
        query: `
{
   "measures": [
      "request.totalRequest",
      "request.totalSessions"
   ],
   "dimensions": [
      "request.referer"
   ],
   "filters": [
      {
         "member": "request.referer",
         "operator": "set"
      }
   ],
   "order": {
      "request.totalRequest": "desc"
   },
   "limit": 15
}`
    }
];
