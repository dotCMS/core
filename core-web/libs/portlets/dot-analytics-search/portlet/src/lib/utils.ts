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
    } catch (e) {
        return false;
    }
};

export const AnalyticsQueryExamples: AnalyticsQueryExample[] = [
    {
        title: 'analytics.search.query.by.hits',
        query: `
{
   "measures":[
      "request.totalRequest"
   ],
   "dimensions":[
      "request.identifier",
      "request.title",
      "request.baseType",
      "request.url"
   ],
   "order":{
      "request.totalRequest":"desc"
   }
}`
    },
    {
        title: 'analytics.search.query.by.time.frame',
        query: `
{
   "measures":[
      "request.totalRequest"
   ],
   "dimensions":[
      "request.identifier",
      "request.title",
      "request.baseType",
      "request.url"
   ],
   "order":{
      "request.totalRequest":"desc"
   },
   "timeDimensions":[
      {
         "dimension":"request.createdAt",
         "dateRange":"last 7 days",
         "granularity":"day"
      }
   ]
}
`
    },
    {
        title: 'analytics.search.query.by.object.type',
        query: `
{
   "measures":[
      "request.totalRequest"
   ],
   "dimensions":[
      "request.baseType"
   ],
   "order":{
      "request.totalRequest":"desc"
   }
}
`
    },
    {
        title: 'analytics.search.query.by.average.request',
        query: `
{
   "dimensions":[
      "request.createdAt"
   ],
   "measures":[
      "request.fileRequestAverage",
      "request.pageRequestAverage",
      "request.otherRequestAverage"
   ],
   "order":{
      "request.createdAt":"desc"
   },
   "timeDimensions":[
      {
         "dimension":"request.createdAt",
         "granularity":"day"
      }
   ]
}
`
    },
    {
        title: 'analytics.search.query.by.total.sessions',
        query: `
{
   "dimensions":[
      "request.conHost",
      "request.conHostName"
   ],
   "measures":[
      "request.totalSessions",
      "request.totalRequest"
   ],
   "order":{
      "request.conHostName":"desc"
   }
}
`
    },
    {
        title: 'analytics.search.query.by.unique.request',
        query: `
{
   "dimensions":[
      "request.identifier",
      "request.title"
   ],
   "measures":[
      "request.totalSessions"
   ],
   "filters":[
      {
         "member":"request.baseType",
         "operator":"equals",
         "values":[
            "HTMLPAGE"
         ]
      }
   ],
   "order":{
      "request.totalSessions":"desc"
   }
}
`
    },
    {
        title: 'analytics.search.query.by.filter.event',
        query: `
{
   "dimensions":[
      "request.identifier",
      "request.title",
      "request.url"
   ],
   "measures":[
      "request.totalRequest"
   ],
   "filters":[
      {
         "member":"request.baseType",
         "operator":"equals",
         "values":[
            "HTMLPAGE"
         ]
      }
   ],
   "order":{
      "request.totalRequest":"desc"
   },
   "timeDimensions":[
      {
         "dimension":"request.createdAt",
         "granularity":"day",
         "dateRange":"This week"
      }
   ]
}
`
    },
    {
        title: 'analytics.search.query.by.blogs.hits',
        query: `
{
   "dimensions":[
      "request.identifier",
      "request.title"
   ],
   "measures":[
      "request.totalRequest"
   ],
   "filters":[
      {
         "member":"request.contentTypeName",
         "operator":"equals",
         "values":[
            "Blog"
         ]
      }
   ],
   "order":{
      "request.totalRequest":"desc"
   }
}
`
    }
];
