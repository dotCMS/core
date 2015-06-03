var glob = require('glob')
var fs = require('fs')
var gulp = require('gulp')
var del = require('del')
var minimist = require('minimist')
var jspm = require('jspm')
var pConfig = require('./package.json')
var imports = {
  exec:      require('child_process').exec,
  karma:     require('karma').server,
  webServer: require('gulp-webserver')
}

var config = {
  appProtocol:        'http',
  appHostname:        'localhost',
  appPort:            9000,
  proxyProtocol:      'http',
  proxyHostname:      'localhost',
  proxyPort:          8080,
  depBundles:         './jspm_bundles',
  nonStandardBundles: {
    'angular2/angular2':        'angular2',
    'rtts_assert/rtts_assert':  'rtts_assert'
  },
  noBundle:           ['css', 'text'],
  /**
   *  WARNING! These directories are deleted by the 'reset-workspace' task.
  *   Do not add any directory that is present after a fresh 'clone' operation.
  */
  transientDirectories: [
    './node_modules',
    './jspm_packages',
    './jspm_bundles'
  ]
}
config.appHost = config.appProtocol + '://' + config.appHostname + ':' + config.appPort
config.proxyHost = config.proxyProtocol + '://' + config.proxyHostname + ':' + config.proxyPort


var minimistCliOpts = {
  boolean: ['open'],
  alias:   {
    'open': ['o']
  },
  default: {
    open: false,
    env:  process.env.NODE_ENV || 'production'
  }
};
config.args = minimist(process.argv.slice(2), minimistCliOpts)


gulp.task('jspmInstall', function (done) {
  imports.exec('jspm install', function (error, stdout, stderr) {
    if (stdout) {
      console.log(stdout);
    }
    if (stderr) {
      console.warn(stderr)
    }
    done();
  })

})


gulp.task('bundleDeps', ['unbundle'], function (done) {
  var deps = pConfig.jspm.dependencies || {}
  var promises = []
  for (var nm in deps) {
    if (config.noBundle.indexOf(nm) < 0) {
      promises.push(jspm.bundle(nm,
        config.depBundles +
        '/' +
        nm +
        '.bundle.js',
        {inject:      true,
          minify:     true,
          sourceMaps: false
        }))
    }
  }
  deps = config.nonStandardBundles || {}
  for (nm in deps) {
    promises.push(jspm.bundle(nm,
      config.depBundles +
      '/' +
      deps[nm] +
      '.bundle.js',
      {inject:      true,
        minify:     true,
        sourceMaps: false
      }))
  }
  Promise.all(promises).then(function (results) {
    done()
  })
})

gulp.task('unbundle', ['default'], function (done) {
  imports.exec('jspm unbundle', function (error, stdout, stderr) {
    if (stdout) {
      console.log(stdout);
    }
    if (stderr) {
      console.warn(stderr)
    }
    done();
  })
})

gulp.task('karma', [], function(done){
  imports.karma.start({
    configFile: __dirname + '/karma.conf.js',
    singleRun: false,
    usePolling: false
  }, done);
})

gulp.task('i-test', function(done){

  imports.karma.start({
    configFile: __dirname + '/karma.conf.js',
    singleRun: false,
    usePolling: false,
    proxies: {
      '/api': config.proxyHost + '/api'
    },
    jspm: {
      useBundles: true,
      loadFiles: ['src/**/*.it.es6'],
      serveFiles: ['src/**/*.ts', 'src/*/{*.es6,!(it|spec)/**/*.es6}',
                   'jspm_bundles/**/*.js',
                   'thirdparty/**/*.{es6,ts}']
    }
  }, done)


})

gulp.task('start-server', function (done) {

  var http = require('http');
  var proxy = require('proxy-middleware');
  var connect = require('connect');
  var serveStatic = require('serve-static');
  var serveIndex = require('serve-index');
  var open = require('open');
  var url = require('url');

  var proxyBasePaths = [
    'admin',
    'html',
    'api',
    'c',
    'dwr',
    'DotAjaxDirector'
  ]

  var app = connect();

  // proxy API requests to the node server
  proxyBasePaths.forEach(function (pathSegment) {
    var target = config.proxyHost + '/' + pathSegment;
    var proxyOptions = url.parse(target)
    proxyOptions.route = '/' + pathSegment
    proxyOptions.preserveHost = true
    app.use(function (req, res, next) {
      if (req.url.indexOf('/' + pathSegment + '/') === 0) {
        console.log("Forwarding request: ", req.url)
        proxy(proxyOptions)(req, res, next)
      } else {
        next()
      }
    })
  })
  app.use(serveStatic('./'))
  app.use(serveIndex('./'))

  http.createServer(app)
    .listen(config.appPort)
    .on('listening', function () {
      console.log('Started connect web server on ' + config.appHost)
      if (config.args.open) {
        open(config.appHost)
      }
      else {
        console.log("add the '-o' flag to automatically open the default browser")
      }
    })

  done()
})

//noinspection JSUnusedLocalSymbols
gulp.task('play', ['start-server'], function(done){
  // if 'done' is not passed in this task will not block.
})


gulp.task('clean', ['unbundle'], function (done) {
  del(['./dist'], done)
})

gulp.task('reset-workspace', function(done){
  del(config.transientDirectories, done)
})

gulp.task('default', function () {});
