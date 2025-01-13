function fn() {
    let env = karate.env; // get system property 'karate.env'
    karate.log('karate.env system property was:', env);
    if (!env) {
        env = 'dev';
    }
    let baseUrl = karate.properties['karate.base.url'] || 'http://localhost:8080';
    let authString = 'admin@dotcms.com:admin';
    let encodedAuth = function(s) {
        return java.util.Base64.getEncoder().encodeToString(s.getBytes('UTF-8'));
    };
    let authHeader = 'Basic ' + encodedAuth(authString);
    let config = {
        env: env,
        baseUrl: baseUrl,
        commonHeaders : {
            'Content-Type': 'application/json',
            'Authorization': authHeader
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