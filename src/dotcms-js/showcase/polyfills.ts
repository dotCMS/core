import 'core-js/es6';
import 'core-js/es7/reflect';
import 'zone.js/dist/zone';
import { environment } from '../../environments/environment';


if (environment.production) {
    // Production
} else {
    // Development
    Error['stackTraceLimit'] = Infinity;
    // import 'zone.js/dist/long-stack-trace-zone';
}
