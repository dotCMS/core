'use strict';


var del = require('del');
var exec = require('child_process').exec;
var flatten = require('gulp-flatten');
var fs = require('fs');
var glob = require('glob');
var gulp = require('gulp');
var karmaServer = require('karma').Server;
var minifyCss = require('gulp-minify-css');
var minimist = require('minimist');
var rename = require('gulp-rename');
var replace = require('gulp-replace');
var rev = require('gulp-rev');
var sass = require('gulp-sass')
var ts = require('gulp-typescript');
var uglify = require('gulp-uglify');
var usemin = require('gulp-usemin');
var flatten = require('gulp-flatten');
var replace = require('gulp-replace-task');

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

var typescriptProject = ts.createProject('./tsconfig.json');

var project = {
  configFile: 'app.constant.dev.ts',
  server: null,

  clean: function(cb) {
    console.log('Starting Clean')
    del.sync([config.distDir, config.buildDir, './gh_pages'])
    console.log('Finished Clean')
    cb()
  },

  compileJavascript: function(cb) {
    var rename = require("gulp-rename");

    gulp.src(['./src/view/' + this.configFile])
        .pipe(ts())
        .pipe(rename('constants.js'))
        .pipe(gulp.dest('build/view')).on('finish', cb);
  },

  copyModules: function(cb) {
    gulp.src([
        '@angular/**/*.js',
        'lodash/lodash.js',
        'primeng/**/*.js',
        'rxjs/**',
        'angular2-logger/**',
        'moment/moment.js'
        ], {cwd: 'node_modules/**'}) /* Glob required here. */
      .pipe(gulp.dest('build/thirdparty')).on('finish', cb);

  },

  copyFontFiles: function(cb) {
    gulp.src([
          'semantic-ui/dist/themes/default/assets/fonts/**'
        ], {cwd: 'node_modules/**'}) /* Glob required here. */
        .pipe(flatten())
        .pipe(gulp.dest('build/view/components/rule-engine/fonts')).on('finish', cb);
  },

  deployResources: function(cb) {

    gulp.src('./src/**/*.scss')
        .pipe(sass({outputStyle: 'expanded'}))
        .pipe(gulp.dest(function(file) {return file.base})).on('finish', function() {
          gulp.src('./src/index.html')
              .pipe(usemin({
                css: [ rev() ],
                js: [ uglify(), rev() ],
                ng: [ uglify(), rev() ],
                inlinejs: [ uglify() ],
                inlinecss: [ minifyCss(), 'concat' ]
              }))
              .pipe(replace({
                patterns: [
                  {
                    match: /<base href=\"\/build\/\">/g,
                    replacement: '<base href=\"/dotAdmin/\">'
                  }
                ]
              }))
              .pipe(gulp.dest('build/')).on('finish', cb);
    });

  },

  /**
   *
   */
  compileTypescript: function(cb) {
    exec('npm run tsc-no', function(err, stdout, stderr) {
      // Ignoring non-zero exit code errors, as tsc will provide non-zero exit codes on warnings.
      console.log(stdout);
      cb();
    })
  },

  copyCssThemesResources: function(cb) {
    gulp.src([
      'primeng/resources/themes/omega/fonts/*.*',

    ], {cwd: 'node_modules/**'}) /* Glob required here. */
        .pipe(flatten())
        .pipe(gulp.dest('build/scss/fonts')).on('finish', function() {
          gulp.src([
            'primeng/resources/themes/omega/images/*.*',
          ], {cwd: 'node_modules/**'})
            .pipe(flatten())
            .pipe(gulp.dest('build/scss/images')).on('finish', cb)
    });
  },

  copyCssThemes: function(cb) {
    gulp.src([
      'primeng/resources/primeng.min.css'
    ], {cwd: 'node_modules/**'}) /* Glob required here. */
        .pipe(flatten())
        .pipe(gulp.dest('build/scss')).on('finish', function() {
          project.copyCssThemesResources(cb);
        });
  },

  compileStyles: function(cb) {
    project.copyCssThemes(function () {
      var sourcemaps = require('gulp-sourcemaps');
          gulp.src('./src/**/*.scss')
              .pipe(sourcemaps.init())
              .pipe(sass({outputStyle: config.buildTarget === 'dev' ? 'expanded' : 'compressed'}))
              .on('error', sass.logError)
              .pipe(sourcemaps.write())
              .pipe(gulp.dest(config.buildDir)).on('finish', cb);
    });
  },

  callbackOnCount: function(count, cb) {
    return function() {
      if (--count === 0) {
        cb()
      }
    }
  },

  compileStatic: function(cb) {

    var done = project.callbackOnCount(2, cb)
    var gitRev = require('git-rev')
    gulp.src('./src/**/*.{js,css,eot,svg,ttf,woff,woff2,png}').pipe(gulp.dest(config.buildDir)).on('finish', done);
    gitRev.short(function(rev) {
      gulp.src([config.srcDir + '/**/*.html'])
          .pipe(replace(/\$\{build.revision\}/, rev))
          .pipe(replace(/\$\{build.date\}/, new Date().toISOString()))
          .pipe(gulp.dest(config.buildDir)).on('finish', done);
    })
  },

  compile: function(cb) {
    var done = project.callbackOnCount(7, cb, 'compile')
    project.compileJavascript(done)
    project.compileTypescript(done)
    project.compileStyles(done)
    project.compileStatic(done)
    project.deployResources(done)
    project.copyModules(done)
    project.copyFontFiles(done)
  },

  packageRelease: function(done) {
    project.clean(function() {
      project.compile(function() {
        var outPath = config.distDir + '/core-web.zip'
        if (!fs.existsSync(config.distDir)) {
          fs.mkdirSync(config.distDir)
        }
        var output = fs.createWriteStream(outPath)
        var archiver = require('archiver')
        var archive = archiver.create('zip', {})

        output.on('close', function() {
          console.log('Archive Created: ' + outPath + '. Size: ' + archive.pointer() / 1000000 + 'MB')
          done()
        });

        archive.on('error', function(err) {
          done(err)
        });

        archive.pipe(output);

        archive.directory('./build', '.').finalize()

      })
    })
  },
  catchError: function(msg){
    return function(e){
      console.log(msg || 'Error: ', e)
    }
  },
  watch: function() {
    project.watchTs()
    gulp.watch('./src/**/*.html', ['compile-templates']).on('error', project.catchError('Error watching HTML files'))
    gulp.watch('./src/**/*.js', ['compile-templates']).on('error', project.catchError('Error watching JS files'))
    return gulp.watch('./src/**/*.scss', ['compile-styles']).on('error', project.catchError('Error watching SCSS files'))
  },

  watchTs: function(){
    var spawn = require('cross-spawn')
    var tsc = spawn('npm', ['run', 'tsc']).on('error', project.catchError('Error running typescript compiler.'))
    tsc.stdout.on('data', function(data) {
      console.log('tsc: ' + data)
    })

    tsc.stderr.on('data', function(data) {
      console.log('tsc: ' + data)
    })

    tsc.on('close', function(code) {
      console.log('tsc process exited with code ' + code)
    })
  },

  /**
   * Configure the proxy and start the webserver.
   */
  startServer: function() {

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

    app.use(function (req, res, next) {
      if (req.url.startsWith('/build/c') || req.url.startsWith('/build/public') ||
          req.url.startsWith('/build/fromCore')) {

        req.url = '/build/index.html';
      }

      next();
    });

    // proxy API requests to the node server
    proxyBasePaths.forEach(function(pathSegment) {
      var target = config.proxyHost + '/' + pathSegment;
      var proxyOptions = url.parse(target)
      proxyOptions.route = '/' + pathSegment
      proxyOptions.preserveHost = true
      app.use(function(req, res, next) {

        if (req.url.indexOf('/' + pathSegment + '/') === 0) {
          console.log('Forwarding request: ', req.url)
          proxy(proxyOptions)(req, res, next)
        } else {
          next()
        }
      })
    })
    app.use(serveStatic('./'))
    app.use(serveIndex('./'))

    project.server = http.createServer(app);
    project.server.on('error', project.catchError('Error connecting to httpServer'));
    project.server.on('listening', function() {
      console.log('Started connect web server on ' + config.appHost)
      if (config.args.open) {
        var openTo = config.args.open === true ? '/index-dev.html' : config.args.open
        console.log('Opening default browser to ' + openTo)
        open(config.appHost + openTo)
      }
      else {
        console.log('add the "-o" flag to automatically open the default browser')
      }
    });
    project.server.listen(config.appPort)
  },

  stopServer: function(callback) {
    project.server.close(callback)
  },

  publish: function(deployConfig){
    var artifactoryUpload = require('gulp-artifactory-upload');
    var getRev = require('git-rev')

    var mvn = {
      group: 'com.dotcms',
      artifactId: 'core-web',
      version: require('./package.json').version
    }

    getRev.short(function(rev) {
      var versionStr = mvn.version + '-SNAPSHOT';
      var baseDeployName = 'core-web-' + versionStr
      var deployName = baseDeployName + '.zip'
      var artifactoryUrl = deployConfig.url + '/' + mvn.group.replace('.', '/') + '/' + mvn.artifactId + '/' + versionStr

      console.log('Deploying artifact: PUT ', artifactoryUrl + '/' + deployName)

      var pomPath;

      generatePom(baseDeployName, mvn.group, mvn.artifactId, versionStr, 'zip', function(path, err) {
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
              rename: function(filename) {
                return filename.replace(mvn.artifactId, baseDeployName)
              }
            }))
            .on('error', function(err) {
              throw err
            }).on('end', function() {
          project.clean(function() {
            console.log('All done.');
          })
        })
      })
    })
  }
}



gulp.task('start-server', function(done) {
  project.startServer()
  done()
})


/**
 *  Deploy Tasks
 */

gulp.task('package', [], function(done) {
  project.packageRelease(done)
});

var generatePom = function(baseDeployName, groupId, artifactId, version, packaging, callback) {
  var pom = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"',
    'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"> <modelVersion>4.0.0</modelVersion>"',
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

  fs.writeFile(outPath, pom.join('\n'), function(err) {
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
  project.configFile = 'app.constant.prod.ts';
  done()
})

gulp.task('publish-snapshot', ['set-build-target-to-prod', 'package'], function(done) {
  project.publish(require('./deploy-config.js').artifactory.snapshot);
  done();
});

gulp.task('ghPages-clone', ['package'], function(done) {
  var exec = require('child_process').exec;

  var options = {
    cwd: __dirname,
    timeout: 300000
  }
  if (fs.existsSync(__dirname + '/gh_pages')) {
    del.sync('./gh_pages')
  }
  exec('git clone -b gh-pages git@github.com:dotCMS/core-web.git gh_pages', options, function(err, stdout, stderr) {
    console.log(stdout);
    if (err) {
      done(err)
      return;
    }
    del.sync(['./gh_pages/**/*', '!./gh_pages/.git'])
    gulp.src('./dist/**/*').pipe(gulp.dest('./gh_pages')).on('finish', function() {
      done()
    })
  })
})

gulp.task('publish-github-pages', ['ghPages-clone'], function(done) {
  var exec = require('child_process').exec;

  var options = {
    cwd: __dirname + '/gh_pages',
    timeout: 300000
  }

  var gitAdd = function(opts) {
    console.log('adding files to git.')
    exec('git add .', opts, function(err, stdout, stderr) {
      console.log(stdout);
      if (err) {
        console.log(stderr);
        done(err);
      } else {
        gitCommit(opts)
      }

    })
  }

  var gitCommit = function(opts) {
    exec('git commit -m "Autobuild gh-pages..."', opts, function(err, stdout, stderr) {
      console.log(stdout);
      if (err) {
        console.log(stderr);
        done(err);
      } else {
        gitPush(opts)
      }
    })
  }

  var gitPush = function(opts) {
    exec('git push -u  origin gh-pages', opts, function(err, stdout, stderr) {
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


gulp.task('copy-dist-main', [], function(done) {
  var gitRev = require('git-rev')

  gitRev.short(function(rev) {
    console.log('Revision is: ', rev)
    var result = gulp.src([config.buildDir + '/index.html'])
        .pipe(replace(/\$\{build.revision\}/, rev))
        .pipe(replace(/\$\{build.date\}/, new Date().toISOString()))
        .pipe(gulp.dest(config.distDir))
    done()
  })

})

gulp.task('copy-dist-all', ['copy-dist-main'], function() {
  return gulp.src(['./build/*.js', './build/*.map']).pipe(replace('./dist/core-web.sfx.js', './core-web.sfx.js')).pipe(gulp.dest(config.distDir))
})

gulp.task('compile-ts', function(cb) {
  project.compileTypescript(cb)
});

gulp.task('copy-node-files', function(cb) {
  project.copyModules(cb)
});

gulp.task('compile-styles', function(done) {
  project.compileStyles(done)
});

gulp.task('copy-font-icons', function(done) {
  project.copyFontFiles(done);
})

gulp.task('compile-js', function(done) {
  project.compileJavascript(done)
})

gulp.task('compile-templates', [], function(done) {
  project.compileStatic(done)
})

gulp.task('compile', [], function(done) {
  project.compile(done)
})

gulp.task('watch', ['compile-styles', 'compile-js', 'compile-templates', 'copy-node-files', 'copy-font-icons'], function() {
  return project.watch()
});

gulp.task('publish', ['publish-github-pages'], function(done) {
  project.configFile = 'app.constant.prod.ts';
  project.publish(require('./deploy-config.js').artifactory.release);
  done()
})

gulp.task('serve', ['start-server', 'watch'], function(done) {
  // if 'done' is not passed in this task will not block.
})

gulp.task('build', ['package'], function(done) {
})

gulp.task('clean', [], function(done) {
  project.clean(done)
})

gulp.task('default', function(done) {
  done()
});