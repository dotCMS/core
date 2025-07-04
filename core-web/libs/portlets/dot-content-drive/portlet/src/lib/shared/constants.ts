import { Site } from '@dotcms/dotcms-js';

export const SYSTEM_HOST: Site = {
    identifier: 'SYSTEM_HOST',
    hostname: 'SYSTEM_HOST',
    type: 'HOST',
    archived: false,
    googleMap: ''
};

// We want to exclude forms and Hosts, and only show contentlets that are not deleted
export const BASE_QUERY = '+systemType:false -contentType:forms -contentType:Host +deleted:false';
