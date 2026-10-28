function fn() {
    let env = karate.env; // get system property 'karate.env'
    karate.log('karate.env system property was:', env);
    if (!env) {
        env = 'dev';
    }
    let baseUrl = karate.properties['karate.base.url'] || 'http://localhost:8080';
    // Basic username/password authentication is disabled by default, so bootstrap a real API JWT
    // through the supported JSON session login then API-token issuance. karate.callSingle runs the
    // bootstrap once per JVM and keeps its session cookie out of ordinary collection requests.
    let auth = karate.callSingle('classpath:authentication.feature');
    let config = {
        env: env,
        baseUrl: baseUrl,
        authHeader: auth.authHeader,
        commonHeaders : {
            'Content-Type': 'application/json',
            'Authorization': auth.authHeader
        }
    }
    if (env === 'dev') {
        // customize
        // e.g. config.foo = 'bar';
    } else if (env === 'e2e') {
        // customize
    }
    karate.log('Base URL set to:', config.baseUrl);

    return config;
}
