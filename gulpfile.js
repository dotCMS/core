"use strict";


var glob = require('glob')
var fs = require('fs')
var gulp = require('gulp')
var rename = require('gulp-rename')
var del = require('del')
var minimist = require('minimist')
var replace = require('gulp-replace')
var ts = require('gulp-typescript');
var karmaServer = require('karma').Server
var exec = require('child_process').exec

var config = {
  appProtocol: 'http',
  appHostname: 'localhost',
  appPort: 9000,
  proxyProtocol: 'http',
  proxyHostname: 'localhost',
  proxyPort: 8080,
  buildDir: './build',
  distDir: __dirname + '/dist',
  srcDir: './src',
  buildTarget: 'dev'

}
config.appHost = config.appProtocol + '://' + config.appHostname + ':' + config.appPort
config.proxyHost = config.proxyProtocol + '://' + config.proxyHostname + ':' + config.proxyPort


var minimistCliOpts = {
  string: ['open', 'env'],
  alias: {
    'open': ['o']
  },
  default: {
    open: false,
    env: process.env.NODE_ENV || 'dev'
  }
};
config.args = minimist(process.argv.slice(2), minimistCliOpts)

if(config.args.env){
  if(config.args.env.startsWith('prod')) {
    config.buildTarget = 'prod'
  } else {
    config.buildTarget = 'dev'
  }
}

var typescriptProject = ts.createProject(config.srcDir + '/tsconfig.json');

var project = {
  server: null,

  clean: function (cb) {
    console.log("Starting 'Clean'")
    del.sync([config.distDir, config.buildDir, './gh_pages'])
    console.log("Finished 'Clean'")
    cb()
  },

  compileJavascript: function (cb) {
    cb()
  },

  copyNodeFiles: function (cb) {

    var libs =
    {
      'angular2/bundles/': [
        { dev: 'angular2-polyfills.js', prod: 'angular2-polyfills.min.js', out: 'angular2-polyfills.js' },
        { dev: 'angular2.dev.js', prod: 'angular2.min.js', out: 'angular2.js' },
        { dev: 'http.dev.js', prod: 'http.min.js', out: 'http.js' },
        { dev: 'router.dev.js', prod: 'router.min.js', out: 'router.js' }
      ],
      'rxjs/bundles/': [
        { dev: 'Rx.js', prod: 'Rx.min.js', out: 'Rx.js' },
        { dev: 'Rx.min.js.map', prod: null, out: 'Rx.min.js.map' }
      ],
      'angular-material/': [
        { dev: 'angular-material.layouts.css', prod: 'angular-material.layouts.min.css', out: 'angular-material.layouts.css' }
      ],
      'core-js/client/': [
        { dev: 'shim.js', prod: 'shim.min.js', out: 'shim.js' }
      ],
      'es6-shim/': [
        { dev: 'es6-shim.js', prod: 'es6-shim.min.js', out: 'es6-shim.js' }
      ],
      'jquery/dist/': [
        { dev: 'jquery.js', prod: 'jquery.min.js', out: 'jquery.js' },
        { dev: 'jquery.min.map', prod: null, out: 'jquery.min.map' }
      ],
      'systemjs/dist/': [
        { dev: 'system.src.js', prod: 'system.js', out: 'system.js' },
        { dev: 'system.js.map', prod: null, out: 'system.js.map' },
        { dev: 'system-polyfills.src.js', prod: 'system-polyfills.js', out: 'system-polyfills.js' },
        { dev: 'system-polyfills.js.map', prod: null, out: 'system-polyfills.js.map' }
      ],
      'whatwg-fetch/': [
        { dev: 'fetch.js', prod: 'fetch.js', out: 'fetch.js' }
      ]
    }

    var baseOutPath =  config.buildDir + '/thirdparty/'
    var libKeys = Object.keys(libs)


    var count = 0
    libKeys.forEach(function(basePath) {
      var lib = libs[basePath]
      lib.forEach(function(libFile){
        if(libFile[config.buildTarget] != null) {
          count++
        }
      })
    })
    var done = project.callbackOnCount(count, cb)

    libKeys.forEach(function(basePath) {
      var lib = libs[basePath]
      lib.forEach(function(libFile){
        var inFile = libFile[config.buildTarget]
        if(inFile) {
          gulp.src('./node_modules/' + basePath + libFile[config.buildTarget])
              .pipe(rename(function (path) {
                path.basename = libFile.out.substring(0, libFile.out.lastIndexOf("."))
                return path
              }))
              .pipe(gulp.dest(baseOutPath + basePath)).on('finish', done);
        }
      })


    })

  },

  /**
   *
   */
  compileTypescript: function (cb) {
    exec('npm run tsc-no', function (err, stdout, stderr) {
      // Ignoring non-zero exit code errors, as tsc will provide non-zero exit codes on warnings.
      console.log(stdout);
      cb();
    })
  },

  compileStyles: function (cb) {
    var sass = require('gulp-sass')
    var sourcemaps = require('gulp-sourcemaps');
    gulp.src('./src/**/*.scss')
        .pipe(sourcemaps.init())
        .pipe(sass({outputStyle: config.buildTarget === 'dev' ? 'expanded' : 'compressed'}))
        .pipe(sourcemaps.write())
        .pipe(gulp.dest(config.buildDir)).on('finish', cb);
  },

  callbackOnCount: function (count, cb) {
    return function () {
      if (--count === 0) {
        cb()
      }
    }
  },

  compileStatic: function (cb) {

    var done = project.callbackOnCount(2, cb)
    var gitRev = require('git-rev')
    gulp.src('./src/**/*.{js,css,eot,svg,ttf,woff,woff2,png}').pipe(gulp.dest(config.buildDir)).on('finish', done);
    gitRev.short(function (rev) {
      gulp.src([config.srcDir + '/**/*.html'])
          .pipe(replace(/\$\{build.revision\}/, rev))
          .pipe(replace(/\$\{build.date\}/, new Date().toISOString()))
          .pipe(gulp.dest(config.buildDir)).on('finish', done);
    })
  },

  compile: function (cb) {
    var done = project.callbackOnCount(5, cb, 'compile')
    project.compileJavascript(done)
    project.compileTypescript(done)
    project.compileStyles(done)
    project.compileStatic(done)
    project.copyNodeFiles(done)
  },

  packageRelease: function (done) {
    project.clean(function () {
      project.compile(function () {
        var outPath = config.distDir + '/core-web.zip'
        if (!fs.existsSync(config.distDir)) {
          fs.mkdirSync(config.distDir)
        }
        var output = fs.createWriteStream(outPath)
        var archiver = require('archiver')
        var archive = archiver.create('zip', {})

        output.on('close', function () {
          console.log("Archive Created: " + outPath + ". Size: " + archive.pointer() / 1000000 + 'MB')
          done()
        });

        archive.on('error', function (err) {
          done(err)
        });

        archive.pipe(output);

        archive.directory('./build', '.').finalize()

      })
    })
  },

  watch: function () {

    project.watchTs()


    gulp.watch('./src/**/*.html', ['compile-templates'])
    gulp.watch('./src/**/*.js', ['compile-templates'])
    return gulp.watch('./src/**/*.scss', ['compile-styles'])
  },

  watchTs: function(){
    var spawn = require('child_process').spawn
    var tsc = spawn('npm', ['run', 'tsc'])
    tsc.stdout.on('data', function (data) {
      console.log('tsc: ' + data)
    })

    tsc.stderr.on('data', function (data) {
      console.log('tsc: ' + data)
    })

    tsc.on('close', function (code) {
      console.log('tsc process exited with code ' + code)
    })
  },

  /**
   * Configure the proxy and start the webserver.
   */
  startServer: function () {
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

    project.server = http.createServer(app)
        .listen(config.appPort)
        .on('listening', function () {
          console.log('Started connect web server on ' + config.appHost)
          if (config.args.open) {
            var openTo = config.args.open === true ? '/index-dev.html' : config.args.open
            console.log('Opening default browser to ' + openTo)
            open(config.appHost + openTo)
          }
          else {
            console.log("add the '-o' flag to automatically open the default browser")
          }
        })
  },

  stopServer: function (callback) {
    project.server.close(callback)
  },

  rewriteJspmConfigForUnitTests: function (cb) {
    var inFile = __dirname + '/config.js'
    var outFile = __dirname + '/config-karma.local.js'
    var fs = require('fs')
    fs.readFile(inFile, 'utf8', function (err, data) {
      if (err) {
        console.log("It broke: ", err);
        cb(err)
      }
      var result = data.replace(/baseURL/g, '// baseURL');

      fs.writeFile(outFile, result, 'utf8', function (err) {
        if (err) {
          console.log("It broke while writing: ", err);
          cb(err)
        }
        cb()
      });
    });

  },

  runTests: function (singleRun, intTests, callback) {
    var configFile = __dirname + (intTests === true ? '/karma-it.conf.js' : '/karma.conf.js')
    new karmaServer({
      configFile: configFile,
      singleRun: singleRun
    }, callback).start()
  }
}

gulp.task('test', [], function (done) {
  project.rewriteJspmConfigForUnitTests(function (err) {
    if (err) {
      done(err)
    }
    project.runTests(true, false, done)
  });
})

gulp.task('itest', function (done) {
  project.startServer()
  project.runTests(true, true, function () {
    project.stopServer(done)
  })
})


gulp.task('tdd', function () {
  project.watch(true, false)
  return project.runTests(false, false)
})

gulp.task('itdd', ['dev-watch'], function (done) {
  project.startServer()
  project.runTests(false, true, function () {
    project.stopServer(done)
  })
})

gulp.task('start-server', function (done) {
  project.startServer()
  done()
})


/**
 *  Deploy Tasks
 */
gulp.task('package', [], function (done) {
  project.packageRelease(done)
});

var generatePom = function (baseDeployName, groupId, artifactId, version, packaging, callback) {
  var pom = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"',
    'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"> <modelVersion>4.0.0</modelVersion>',
    '<groupId>' + groupId + '</groupId>',
    '<artifactId>' + artifactId + '</artifactId>',
    '<version>' + version + '</version>',
    '<packaging>' + packaging + '</packaging>',
    '</project>']

  var outDir = config.distDir
  var outPath = outDir + '/' + artifactId + '.pom'
  if (!fs.existsSync(outDir)) {
    fs.mkdirSync(outDir)
  }

  fs.writeFile(outPath, pom.join('\n'), function (err) {
    if (err) {
      callback(null, err)
      return
    }
    console.log('Wrote pom to ', outPath);
    callback(outPath)
  });
}

gulp.task('set-build-target-to-prod', function(done){
  config.buildTarget = 'prod'
  done()
})

gulp.task('publish-snapshot', ['package'], function (done) {
  var artifactoryUpload = require('gulp-artifactory-upload');
  var getRev = require('git-rev')
  var deployConfig = require('./deploy-config.js').artifactory.snapshot

  var mvn = {
    group: 'com.dotcms',
    artifactId: 'core-web',
    version: require('./package.json').version
  }

  getRev.short(function (rev) {
    var versionStr = mvn.version + '-SNAPSHOT';
    var baseDeployName = 'core-web-' + versionStr
    var deployName = baseDeployName + ".zip"
    var artifactoryUrl = deployConfig.url + '/' + mvn.group.replace('.', '/') + '/' + mvn.artifactId + '/' + versionStr

    console.log("Deploying artifact: PUT ", artifactoryUrl + '/' + deployName)

    var pomPath;

    generatePom(baseDeployName, mvn.group, mvn.artifactId, versionStr, 'zip', function (path, err) {
      if (err) {
        done(err)
        return;
      }
      console.log('setting pomPath to: ', path)
      pomPath = path
      gulp.src(['./dist/core-web.zip', pomPath])
          .pipe(artifactoryUpload({
            url: artifactoryUrl,
            username: deployConfig.username,
            password: deployConfig.password,
            rename: function (filename) {
              return filename.replace(mvn.artifactId, baseDeployName)
            }
          }))
          .on('error', function (err) {
            throw err
          }).on('end', function () {
        console.log("All done.")
      })
    })
  })

});

gulp.task('ghPages-clone', ['package'], function (done) {
  var exec = require('child_process').exec;

  var options = {
    cwd: __dirname,
    timeout: 300000
  }
  if (fs.existsSync(__dirname + '/gh_pages')) {
    del.sync('./gh_pages')
  }
  exec('git clone -b gh-pages git@github.com:dotCMS/core-web.git gh_pages', options, function (err, stdout, stderr) {
    console.log(stdout);
    if (err) {
      done(err)
      return;
    }
    del.sync(['./gh_pages/**/*', '!./gh_pages/.git'])
    gulp.src('./dist/**/*').pipe(gulp.dest('./gh_pages')).on('finish', function () {
      done()
    })
  })
})

gulp.task('publish-github-pages', ['ghPages-clone'], function (done) {
  var exec = require('child_process').exec;

  var options = {
    cwd: __dirname + '/gh_pages',
    timeout: 300000
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


gulp.task('copy-dist-main', [], function (done) {
  var gitRev = require('git-rev')

  gitRev.short(function (rev) {
    console.log("Revision is: ", rev)
    var result = gulp.src([config.buildDir + '/index.html'])
        .pipe(replace(/\$\{build.revision\}/, rev))
        .pipe(replace(/\$\{build.date\}/, new Date().toISOString()))
        .pipe(gulp.dest(config.distDir))
    done()
  })

})


gulp.task('copy-dist-all', ['copy-dist-main'], function () {
  return gulp.src(['./build/*.js', './build/*.map']).pipe(replace("./dist/core-web.sfx.js", './core-web.sfx.js')).pipe(gulp.dest(config.distDir))
})

gulp.task('compile-ts', function (cb) {
  project.compileTypescript(cb)
});


gulp.task('copy-node-files', function (cb) {
  project.copyNodeFiles(cb)
});

gulp.task('compile-styles', function (done) {
  project.compileStyles(done)
});

gulp.task('compile-js', function (done) {
  project.compileJavascript(done)
})

gulp.task('compile-templates', [], function (done) {
  project.compileStatic(done)
})

gulp.task('compile', [], function (done) {
  project.compile(done)
})

gulp.task('watch', ['compile-styles', 'compile-js', 'compile-templates', 'copy-node-files'], function () {
  return project.watch()
});

gulp.task('publish', ['publish-github-pages', 'publish-snapshot'], function (done) {
  done()
})


gulp.task('serve', ['start-server', 'watch'], function (done) {
  // if 'done' is not passed in this task will not block.
})

gulp.task('build', ['package'], function (done) {
})

gulp.task('clean', [], function (done) {
  project.clean(done)
})

gulp.task('default', function (done) {
  done()
});