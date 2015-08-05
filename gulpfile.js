var glob = require('glob')
var fs = require('fs')
var gulp = require('gulp')
var del = require('del')
var minimist = require('minimist')
var pConfig = require('./package.json')
var replace = require('gulp-replace')
var ts = require('gulp-typescript');
var merge = require('merge2');
var imports = {
  exec: require('child_process').exec,
  karma: require('karma').server,
  webServer: require('gulp-webserver')
}

var config = {
  appProtocol: 'http',
  appHostname: 'localhost',
  appPort: 9000,
  proxyProtocol: 'http',
  proxyHostname: 'localhost',
  proxyPort: 8080,
  buildDir: './build',
  distDir: './dist',
  srcDir: './src',
  buildTarget: 'dev',
  nonStandardBundles: {
    'angular2/angular2': 'angular2',
    'rtts_assert/rtts_assert': 'rtts_assert'
  },
  noBundle: ['css', 'text'],
  /**
   *  WARNING! These directories are deleted by the 'reset-workspace' task.
   *   Do not add any directory that is present after a fresh 'clone' operation.
   */
  transientDirectories: [
    './node_modules',
    './jspm_packages',
    './build',
    './dist'
  ]
}
config.appHost = config.appProtocol + '://' + config.appHostname + ':' + config.appPort
config.proxyHost = config.proxyProtocol + '://' + config.proxyHostname + ':' + config.proxyPort


var minimistCliOpts = {
  boolean: ['open'],
  alias: {
    'open': ['o']
  },
  default: {
    open: false,
    env: process.env.NODE_ENV || 'production'
  }
};
config.args = minimist(process.argv.slice(2), minimistCliOpts)


gulp.task('karma', [], function (done) {
  imports.karma.start({
    configFile: __dirname + '/karma.conf.js',
    singleRun: false,
    usePolling: false
  }, done);
})

gulp.task('i-test', function (done) {

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
        'build/**/*.js',
        'thirdparty/**/*.{es6,ts}']
    }
  }, done)


})


gulp.task('bundle-deps', ['unbundle'], function (done) {
  var deps = pConfig.jspm.dependencies || {}
  var promises = []
  var jspm = require('jspm')

  for (var nm in deps) {
    if (config.noBundle.indexOf(nm) < 0) {
      var bundlePath = config.buildDir + '/' + nm + '.bundle.js'
      promises.push(jspm.bundle(nm,
          bundlePath,
          {
            inject: true,
            minify: true,
            sourceMaps: false
          }))
    }
  }
  deps = config.nonStandardBundles || {}
  for (nm in deps) {
    var nonStdBundlePath =  config.buildDir + '/' + deps[nm] + '.bundle.js'
    promises.push(jspm.bundle(nm,
        nonStdBundlePath,
        {
          inject: true,
          minify: true,
          sourceMaps: false
        }))
  }
  Promise.all(promises).then(function (results) {
    done()
  }).catch(function(e){
    console.log("Error while bundling dependencies: ", e)
    throw e
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
          open(config.appHost + '/index-dev.html')
        }
        else {
          console.log("add the '-o' flag to automatically open the default browser")
        }
      })

  done()
})



/**
 *  Deploy Tasks
 */
gulp.task('packageRelease', ['copy-dist-all'], function (done) {
  var outDir = __dirname + '/dist'
  var outPath = outDir + '/core-web.zip'
  if (!fs.existsSync(outDir)) {
    fs.mkdirSync(outDir)
  }
  var output = fs.createWriteStream(outPath)
  var archiver = require('archiver')
  var archive = archiver.create('zip', {})

  output.on('close', function () {
    console.log("Archive Created: " + outPath + ". Size: " + archive.pointer() / 1000000 + 'MB')
    done()
  });

  archive.on('error', function (err) {
    throw err;
  });

  archive.pipe(output);

  archive.append(fs.createReadStream('./dist/core-web.min.js'), {name: 'core-web.min.js'})
      .append(fs.createReadStream('./dist/core-web.js'), {name: 'core-web.js'})
      .append(fs.createReadStream('./dist/core-web.js.map'), {name: 'core-web.js.map'})
      .append(fs.createReadStream('./dist/index.html'), {name: 'index.html'})
      .finalize()

});

gulp.task('publishSnapshot', ['packageRelease'], function (done) {
  var getRev = require('git-rev')
  var config = require('./deploy-config.js').artifactory.snapshot
  var version = require('./package.json').version
  var artifactoryUpload = require('gulp-artifactory-upload');

  getRev.short(function (rev) {
    var dateStr = new Date().toISOString().replace(/T.+/, '').replace(/-/g, '')
    var versionStr = dateStr + 'R' + rev + '-SNAPSHOT';
    var deployName = 'core-web-' + versionStr + ".zip"
    config.url = config.url + '/com/dotcms/core-web/' + versionStr

    console.log("Deploying artifact: PUT ", config.url + '/' + deployName)
    console.log("Name: ", deployName)
    console.log("Date: ", dateStr)
    console.log("Rev: ", rev)
    console.log("Version: ", versionStr)

    return gulp.src('./dist/core-web.zip')
        .pipe(artifactoryUpload({
          url: config.url,
          username: config.username,
          password: config.password,
          rename: function (filename) {
            return deployName
          }
        }))
        .on('error', function (err) {
          throw err
        })
  })
});

gulp.task('ghPages-clone', ['packageRelease'], function (done) {
  var exec = require('child_process').exec;

  var options = {
    cwd: __dirname,
    timeout: 60000
  }
  if (fs.existsSync(__dirname + '/gh_pages')) {
    del.sync('./gh_pages')
  }
  exec('git clone -b gh-pages git@github.com:dotCMS/core-web.git gh_pages', options, function (err, stdout, stderr) {
    console.log(stdout);
    if (err) {
      console.log(stderr);
      throw err;
    }
    del.sync(['./gh_pages/*.js', './gh_pages/*.map', './gh_pages/*.html', './gh_pages/*.zip'])
    gulp.src('./dist/**/*').pipe(gulp.dest('./gh_pages')).on('finish', function () {
      done()
    })
  })


})

gulp.task('publishGithubPages', ['ghPages-clone'], function (done) {
  var exec = require('child_process').exec;

  var options = {
    cwd: __dirname + '/gh_pages',
    timeout: 60000
  }

  var gitAdd = function (opts) {
    console.log('adding files to git.')
    exec('git add .', opts, function (err, stdout, stderr) {
      console.log(stdout);
      if (err) {
        console.log(stderr);
        done(err);
      } else {
        gitCommit(opts)
      }

    })
  }

  var gitCommit = function (opts) {
    exec('git commit -m "Autobuild gh-pages..."', opts, function (err, stdout, stderr) {
      console.log(stdout);
      if (err) {
        console.log(stderr);
        done(err);
      } else {
        gitPush(opts)
      }
    })
  }

  var gitPush = function (opts) {
    exec('git push -u  origin gh-pages', opts, function (err, stdout, stderr) {
      if (err) {
        console.log(stderr);
        done(err);
      } else {
        done()
      }
    })
  }
  gitAdd(options)
})

gulp.task('copy-dist-bootstrap', function () {
  return gulp.src(['./jspm_packages/github/twbs/**/fonts/*']).pipe(gulp.dest('./dist/jspm_packages/github/twbs/'))
})


gulp.task('copy-dist-main', ['bundle-dist', 'bundle-minified', 'bundle-dev'], function () {
  return gulp.src(['./*.html', 'index.js']).pipe(replace("./dist/core-web.sfx.js", './core-web.sfx.js')).pipe(gulp.dest('./dist/'))
})


gulp.task('copy-dist-all', ['copy-dist-main', 'copy-dist-bootstrap'], function () {
  return gulp.src(['./build/*.js', './build/*.map']).pipe(replace("./dist/core-web.sfx.js", './core-web.sfx.js')).pipe(gulp.dest('./dist/'))
})

var typescriptProject = ts.createProject({
  outDir: config.buildDir,
  module: 'commonjs',
  target: 'es5',
  emitDecoratorMetadata: true,
  typescript: require('typescript')
});

gulp.task('compile-ts', function (done) {
  var tsResult = gulp.src('./src/**/*.ts')
      .pipe(ts(typescriptProject));

  merge([ // Merge the two output streams, so this task is finished when the IO of both operations are done.
    tsResult.dts.pipe(gulp.dest('build/definitions')),
    tsResult.js.pipe(gulp.dest('build'))
  ]).on('queueDrain', done);
});


gulp.task('compile-styles', function (done) {
  var sass = require('gulp-sass')
  var sourcemaps = require('gulp-sourcemaps');
  gulp.src('./src/**/*.scss')
      .pipe(sourcemaps.init())
      .pipe(sass({outputStyle: config.buildTarget === 'dev' ? 'expanded' : 'compressed'}))
      .pipe(sourcemaps.write())
      .pipe(gulp.dest(config.buildDir)).on('finish',  done);
});

gulp.task('compile-js', [], function (done) {
  gulp.src('./src/**/*.js').pipe(gulp.dest(config.buildDir)).on('finish',  done);
})

gulp.task('compile-templates', [], function (done) {
  gulp.src('./src/**/*.html').pipe(gulp.dest(config.buildDir)).on('finish',  done);
})


gulp.task('bundle-dist', ['compile-all'], function (done) {
  var sfxPath = config.buildDir + '/core-web.sfx.js'
  console.info("Bundling self-executing build to " + sfxPath)
  var jspm = require('jspm')
  jspm.bundleSFX('index',
      sfxPath,
      {
        inject: false,
        minify: false,
        sourceMaps: false
      }).then(function () {
        gulp.src(sfxPath).pipe(gulp.dest('./dist/')).on('finish',  done);
      }).catch(function (e) {
        console.log("Error creating bundle with JSPM: ", e)
        throw e
      })
})

/**
 * Compile typescript and styles into build, then bundle the built files using JSPM.
 *
 */
gulp.task('bundle-minified', ['compile-all'], function (done) {
  var minifiedPath = config.buildDir + '/core-web.min.js'
  console.info("Bundling minified build to " + minifiedPath)
  var jspm = require('jspm')
  jspm.configureLoader({
    "baseURL": "./build",
    "defaultJSExtensions": true,
    "transpiler": "babel",
    "babelOptions": {
      "optional": [
        "runtime"
      ],
      "stage": 1
    }})

    jspm.bundle('./index',
        minifiedPath,
        {
          inject: false,
          minify: true,
          sourceMaps: false
        }).then(function () {
          gulp.src(minifiedPath).pipe(gulp.dest('./dist/')).on('finish',  done);

        }).catch(function (e) {
          console.log("Error creating bundle with JSPM: ", e);
          throw e;
        })
})


gulp.task('bundle-dev', ['compile-all'], function (done) {
  var devPath = config.buildDir + '/core-web.js'
  console.info("Bundling unminified build to " + devPath)
  var jspm = require('jspm')
  jspm.bundle('./index',
      devPath,
      {
        inject: false,
        minify: false,
        sourceMaps: false // Bugged :~(
      }).then(done).catch(function (e) {
        console.log("Error creating bundle with JSPM: ", e);
        throw e;
      })
})


gulp.task('bundle-all', ['bundle-dev', 'bundle-minified', 'bundle-sfx'], function (done) {
   done();
})


gulp.task('prod-watch', ['compile-ts', 'compile-styles'], function () {
  gulp.watch('./src/**/*.ts', ['compile-ts']);
  return gulp.watch('./src/**/*.scss', ['compile-styles']);
});

gulp.task('dev-watch', ['compile-styles'], function () {
  gulp.watch('./src/**/*.ts', ['compile-ts']);
  return gulp.watch('./src/**/*.scss', ['compile-styles']);
});

//noinspection JSUnusedLocalSymbols
gulp.task('play', ['start-server', 'dev-watch'], function (done) {
  // if 'done' is not passed in this task will not block.
})

gulp.task('compile-all', ['compile-js', 'compile-ts', 'compile-styles', 'compile-templates'], function (done) {
  done()
})

gulp.task('build', ['compile-all'], function (done) {
  done()
})

gulp.task('clean', ['unbundle'], function (done) {
  del.sync([config.distDir, config.buildDir, './gh_pages'])
  done()
})

gulp.task('reset-workspace', function (done) {
  del(config.transientDirectories, done)
})

gulp.task('default', function (done) {
  done()
});