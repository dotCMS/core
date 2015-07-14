System.config({
  "baseURL": "/",
  "transpiler": "traceur",
  "traceurOptions": {
    "annotations": true,
    "memberVariables": true,
    "typeAssertions": true,
    "typeAssertionModule": "rtts_assert/rtts_assert",
    "types": true
  },
  "paths": {
    "src/*": "src/*",
    "*.css": "*.css",
    "*.html": "*.html",
    "github:*": "jspm_packages/github/*.js",
    "npm:*": "jspm_packages/npm/*.js",
    "angular2/*": "thirdparty/ng2dist/js/prod/es6/angular2/*.es6",
    "rtts_assert/*": "thirdparty/ng2dist/js/prod/es6/rtts_assert/*.es6",
    "*": "*.js"
  },
  "bundles": {
    "build/reflect-metadata.bundle": [
      "npm:reflect-metadata@0.1.0/Reflect",
      "npm:reflect-metadata@0.1.0"
    ],
    "build/zone.js.bundle": [
      "npm:zone.js@0.4.4/zone",
      "npm:zone.js@0.4.4"
    ],
    "build/firebase.bundle": [
      "github:firebase/firebase-bower@2.2.7/firebase",
      "github:firebase/firebase-bower@2.2.7"
    ],
    "build/flux.bundle": [
      "npm:flux@2.0.3/lib/invariant",
      "npm:flux@2.0.3/lib/Dispatcher",
      "npm:flux@2.0.3/index",
      "npm:flux@2.0.3"
    ],
    "build/events.bundle": [
      "npm:events@1.0.2/events",
      "npm:events@1.0.2",
      "github:jspm/nodelibs-events@0.1.1/index",
      "github:jspm/nodelibs-events@0.1.1"
    ],
    "build/rtts_assert.bundle": [
      "rtts_assert/src/rtts_assert",
      "rtts_assert/rtts_assert"
    ],
    "build/bootstrap.bundle": [
      "github:components/jquery@2.1.4/jquery",
      "github:components/jquery@2.1.4",
      "github:twbs/bootstrap@3.3.5/js/bootstrap",
      "github:twbs/bootstrap@3.3.5"
    ],
    "build/debug.bundle": [
      "npm:ms@0.7.1/index",
      "npm:ms@0.7.1",
      "npm:debug@2.2.0/debug",
      "npm:debug@2.2.0/browser",
      "npm:debug@2.2.0"
    ],
    "build/jsonp.bundle": [
      "npm:ms@0.7.0/index",
      "npm:ms@0.7.0",
      "npm:debug@2.1.3/debug",
      "npm:debug@2.1.3/browser",
      "npm:debug@2.1.3",
      "npm:jsonp@0.2.0/index",
      "npm:jsonp@0.2.0"
    ],
    "build/rx.bundle": [
      "npm:process@0.10.1/browser",
      "npm:rx@2.5.3/dist/rx.aggregates",
      "npm:rx@2.5.3/dist/rx.async",
      "npm:rx@2.5.3/dist/rx.backpressure",
      "npm:rx@2.5.3/dist/rx.binding",
      "npm:rx@2.5.3/dist/rx.coincidence",
      "npm:rx@2.5.3/dist/rx.experimental",
      "npm:rx@2.5.3/dist/rx.joinpatterns",
      "npm:rx@2.5.3/dist/rx.sorting",
      "npm:rx@2.5.3/dist/rx.virtualtime",
      "npm:rx@2.5.3/dist/rx.testing",
      "npm:rx@2.5.3/dist/rx.time",
      "npm:process@0.10.1",
      "github:jspm/nodelibs-process@0.1.1/index",
      "github:jspm/nodelibs-process@0.1.1",
      "npm:rx@2.5.3/dist/rx",
      "npm:rx@2.5.3/index",
      "npm:rx@2.5.3"
    ],
    "build/rest.bundle": [
      "github:cujojs/rest@1.3.1/client",
      "npm:process@0.10.1/browser",
      "npm:when@3.7.3/lib/TimeoutError",
      "npm:when@3.7.3/lib/state",
      "npm:when@3.7.3/lib/apply",
      "npm:when@3.7.3/lib/decorators/flow",
      "npm:when@3.7.3/lib/decorators/fold",
      "npm:when@3.7.3/lib/decorators/inspect",
      "npm:when@3.7.3/lib/decorators/iterate",
      "npm:when@3.7.3/lib/decorators/progress",
      "npm:when@3.7.3/lib/decorators/with",
      "npm:when@3.7.3/lib/format",
      "npm:when@3.7.3/lib/makePromise",
      "npm:when@3.7.3/lib/Scheduler",
      "github:cujojs/rest@1.3.1/util/mixin",
      "github:cujojs/rest@1.3.1/util/normalizeHeaderName",
      "github:cujojs/rest@1.3.1/util/responsePromise",
      "github:cujojs/rest@1.3.1/client/default",
      "npm:process@0.10.1",
      "npm:when@3.7.3/lib/decorators/array",
      "npm:when@3.7.3/lib/decorators/unhandledRejection",
      "npm:when@3.7.3/lib/Promise",
      "github:cujojs/rest@1.3.1/UrlBuilder",
      "github:jspm/nodelibs-process@0.1.1/index",
      "github:jspm/nodelibs-process@0.1.1",
      "npm:when@3.7.3/lib/env",
      "npm:when@3.7.3/lib/decorators/timed",
      "npm:when@3.7.3/when",
      "npm:when@3.7.3",
      "github:cujojs/rest@1.3.1/client/xhr",
      "github:cujojs/rest@1.3.1/browser",
      "github:cujojs/rest@1.3.1"
    ],
    "build/angular2.bundle": [
      "angular2/src/facade/lang",
      "angular2/src/facade/collection",
      "angular2/src/di/annotations",
      "angular2/src/reflection/types",
      "angular2/src/reflection/reflection_capabilities",
      "angular2/src/di/key",
      "angular2/src/di/exceptions",
      "npm:process@0.10.1/browser",
      "npm:rx@2.5.3/dist/rx.aggregates",
      "npm:rx@2.5.3/dist/rx.async",
      "npm:rx@2.5.3/dist/rx.backpressure",
      "npm:rx@2.5.3/dist/rx.binding",
      "npm:rx@2.5.3/dist/rx.coincidence",
      "npm:rx@2.5.3/dist/rx.experimental",
      "npm:rx@2.5.3/dist/rx.joinpatterns",
      "npm:rx@2.5.3/dist/rx.sorting",
      "npm:rx@2.5.3/dist/rx.virtualtime",
      "npm:rx@2.5.3/dist/rx.testing",
      "npm:rx@2.5.3/dist/rx.time",
      "angular2/src/di/opaque_token",
      "angular2/src/change_detection/parser/parser",
      "angular2/src/change_detection/parser/locals",
      "angular2/src/change_detection/constants",
      "angular2/src/change_detection/interfaces",
      "angular2/src/change_detection/pipes/pipe",
      "angular2/src/change_detection/change_detector_ref",
      "angular2/src/change_detection/pipes/pipe_registry",
      "angular2/src/change_detection/change_detection_jit_generator",
      "angular2/src/change_detection/coalesce",
      "angular2/src/change_detection/pipes/iterable_changes",
      "angular2/src/change_detection/pipes/keyvalue_changes",
      "angular2/src/change_detection/pipes/async_pipe",
      "angular2/src/change_detection/pipes/null_pipe",
      "angular2/src/core/annotations_impl/visibility",
      "angular2/src/core/compiler/interfaces",
      "angular2/src/core/annotations_impl/view",
      "angular2/src/dom/dom_adapter",
      "angular2/src/dom/generic_browser_adapter",
      "angular2/src/core/annotations_impl/annotations",
      "angular2/src/core/compiler/directive_metadata",
      "angular2/src/facade/math",
      "angular2/src/core/annotations_impl/di",
      "angular2/src/render/api",
      "angular2/src/render/dom/view/view_container",
      "angular2/src/render/dom/view/element_binder",
      "angular2/src/render/dom/util",
      "angular2/src/render/dom/shadow_dom/content_tag",
      "angular2/src/render/dom/shadow_dom/shadow_dom_strategy",
      "angular2/src/core/zone/vm_turn_zone",
      "angular2/src/render/dom/view/view_hydrator",
      "angular2/src/render/dom/view/property_setter_factory",
      "angular2/src/render/dom/compiler/compile_step",
      "angular2/src/services/xhr",
      "angular2/src/services/url_resolver",
      "angular2/src/render/dom/compiler/property_binding_parser",
      "angular2/src/render/dom/compiler/text_interpolation_parser",
      "angular2/src/render/dom/compiler/selector",
      "angular2/src/render/dom/compiler/view_splitter",
      "angular2/src/render/dom/shadow_dom/shadow_dom_compile_step",
      "angular2/src/core/compiler/view_container_ref",
      "angular2/src/core/compiler/view_manager_utils",
      "angular2/src/core/compiler/view_pool",
      "angular2/src/core/compiler/base_query_list",
      "angular2/src/core/compiler/element_binder",
      "angular2/src/core/compiler/template_resolver",
      "angular2/src/core/compiler/component_url_mapper",
      "angular2/src/core/compiler/proto_view_factory",
      "angular2/src/core/exception_handler",
      "angular2/src/core/life_cycle/life_cycle",
      "angular2/src/render/dom/shadow_dom/style_url_resolver",
      "angular2/src/render/dom/shadow_dom/shadow_css",
      "angular2/src/services/xhr_impl",
      "angular2/src/render/dom/events/key_events",
      "angular2/src/render/dom/events/hammer_common",
      "angular2/src/render/dom/shadow_dom/style_inliner",
      "angular2/src/core/compiler/dynamic_component_loader",
      "angular2/src/core/testability/get_testability",
      "angular2/src/core/application_tokens",
      "angular2/src/core/annotations/di",
      "angular2/src/render/dom/shadow_dom/native_shadow_dom_strategy",
      "angular2/src/render/dom/shadow_dom/emulated_scoped_shadow_dom_strategy",
      "angular2/src/core/annotations/annotations",
      "angular2/src/core/decorators/decorators",
      "angular2/src/directives/class",
      "angular2/src/directives/for",
      "angular2/src/directives/if",
      "angular2/src/directives/non_bindable",
      "angular2/src/directives/switch",
      "angular2/src/forms/validators",
      "angular2/src/forms/directives",
      "angular2/src/forms/validator_directives",
      "angular2/src/forms/form_builder",
      "angular2/src/change_detection/parser/ast",
      "angular2/src/reflection/reflector",
      "npm:process@0.10.1",
      "angular2/src/change_detection/directive_record",
      "angular2/src/change_detection/change_detection_util",
      "angular2/src/change_detection/abstract_change_detector",
      "angular2/src/change_detection/change_detection",
      "angular2/src/core/annotations/visibility",
      "angular2/src/core/annotations/view",
      "angular2/src/dom/browser_adapter",
      "angular2/src/core/compiler/directive_metadata_reader",
      "angular2/src/core/compiler/view_ref",
      "angular2/src/render/dom/view/proto_view",
      "angular2/src/render/dom/shadow_dom/light_dom",
      "angular2/src/render/dom/events/event_manager",
      "angular2/src/render/dom/view/proto_view_builder",
      "angular2/src/render/dom/compiler/compile_control",
      "angular2/src/render/dom/compiler/template_loader",
      "angular2/src/render/dom/compiler/directive_parser",
      "angular2/src/core/compiler/query_list",
      "angular2/src/render/dom/shadow_dom/util",
      "angular2/src/render/dom/events/hammer_gestures",
      "angular2/src/core/testability/testability",
      "angular2/annotations",
      "angular2/directives",
      "angular2/src/forms/model",
      "angular2/src/reflection/reflection",
      "github:jspm/nodelibs-process@0.1.1/index",
      "angular2/src/change_detection/binding_record",
      "angular2/src/change_detection/dynamic_change_detector",
      "angular2/src/render/dom/view/view",
      "angular2/src/render/dom/view/view_factory",
      "angular2/src/render/dom/compiler/compile_element",
      "angular2/src/render/dom/compiler/compile_step_factory",
      "angular2/src/render/dom/shadow_dom/emulated_unscoped_shadow_dom_strategy",
      "angular2/forms",
      "angular2/src/di/binding",
      "github:jspm/nodelibs-process@0.1.1",
      "angular2/src/change_detection/proto_record",
      "angular2/src/change_detection/proto_change_detector",
      "angular2/src/render/dom/compiler/compile_pipeline",
      "npm:rx@2.5.3/dist/rx",
      "angular2/src/change_detection/exceptions",
      "angular2/src/render/dom/compiler/compiler",
      "npm:rx@2.5.3/index",
      "angular2/src/render/dom/direct_dom_renderer",
      "npm:rx@2.5.3",
      "angular2/src/core/compiler/element_ref",
      "angular2/src/facade/async",
      "angular2/src/core/compiler/view_manager",
      "angular2/src/di/injector",
      "angular2/src/core/compiler/element_injector",
      "angular2/di",
      "angular2/src/core/compiler/view",
      "angular2/src/change_detection/parser/lexer",
      "angular2/src/core/compiler/compiler",
      "angular2/change_detection",
      "angular2/src/core/application",
      "angular2/core",
      "angular2/angular2"
    ]
  },
  "defaultJSExtensions": true
});

System.config({
  "meta": {
    "npm:zone.js@0.4.1/zone": {
      "format": "global"
    }
  }
});

System.config({
  "map": {
    "bootstrap": "github:twbs/bootstrap@3.3.5",
    "css": "github:systemjs/plugin-css@0.1.12",
    "debug": "npm:debug@2.2.0",
    "events": "github:jspm/nodelibs-events@0.1.1",
    "firebase": "github:firebase/firebase-bower@2.2.7",
    "flux": "npm:flux@2.0.3",
    "jsonp": "npm:jsonp@0.2.0",
    "reflect-metadata": "npm:reflect-metadata@0.1.0",
    "rest": "github:cujojs/rest@1.3.1",
    "rx": "npm:rx@2.5.3",
    "text": "github:systemjs/plugin-text@0.0.2",
    "traceur": "github:jmcriffey/bower-traceur@0.0.88",
    "traceur-runtime": "github:jmcriffey/bower-traceur-runtime@0.0.88",
    "zone.js": "npm:zone.js@0.4.4",
    "github:cujojs/rest@1.3.1": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0",
      "when": "npm:when@3.7.3"
    },
    "github:jspm/nodelibs-assert@0.1.0": {
      "assert": "npm:assert@1.3.0"
    },
    "github:jspm/nodelibs-buffer@0.1.0": {
      "buffer": "npm:buffer@3.2.2"
    },
    "github:jspm/nodelibs-constants@0.1.0": {
      "constants-browserify": "npm:constants-browserify@0.0.1"
    },
    "github:jspm/nodelibs-crypto@0.1.0": {
      "crypto-browserify": "npm:crypto-browserify@3.9.14"
    },
    "github:jspm/nodelibs-events@0.1.1": {
      "events": "npm:events@1.0.2"
    },
    "github:jspm/nodelibs-http@1.7.1": {
      "Base64": "npm:Base64@0.2.1",
      "events": "github:jspm/nodelibs-events@0.1.1",
      "inherits": "npm:inherits@2.0.1",
      "stream": "github:jspm/nodelibs-stream@0.1.0",
      "url": "github:jspm/nodelibs-url@0.1.0",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "github:jspm/nodelibs-https@0.1.0": {
      "https-browserify": "npm:https-browserify@0.0.0"
    },
    "github:jspm/nodelibs-net@0.1.2": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0",
      "http": "github:jspm/nodelibs-http@1.7.1",
      "net": "github:jspm/nodelibs-net@0.1.2",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "stream": "github:jspm/nodelibs-stream@0.1.0",
      "timers": "github:jspm/nodelibs-timers@0.1.0",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "github:jspm/nodelibs-os@0.1.0": {
      "os-browserify": "npm:os-browserify@0.1.2"
    },
    "github:jspm/nodelibs-path@0.1.0": {
      "path-browserify": "npm:path-browserify@0.0.0"
    },
    "github:jspm/nodelibs-process@0.1.1": {
      "process": "npm:process@0.10.1"
    },
    "github:jspm/nodelibs-stream@0.1.0": {
      "stream-browserify": "npm:stream-browserify@1.0.0"
    },
    "github:jspm/nodelibs-timers@0.1.0": {
      "timers-browserify": "npm:timers-browserify@1.4.1"
    },
    "github:jspm/nodelibs-tty@0.1.0": {
      "tty-browserify": "npm:tty-browserify@0.0.0"
    },
    "github:jspm/nodelibs-url@0.1.0": {
      "url": "npm:url@0.10.3"
    },
    "github:jspm/nodelibs-util@0.1.0": {
      "util": "npm:util@0.10.3"
    },
    "github:jspm/nodelibs-vm@0.1.0": {
      "vm-browserify": "npm:vm-browserify@0.0.4"
    },
    "github:systemjs/plugin-css@0.1.12": {
      "clean-css": "npm:clean-css@3.1.9",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "path": "github:jspm/nodelibs-path@0.1.0"
    },
    "github:twbs/bootstrap@3.3.5": {
      "jquery": "github:components/jquery@2.1.4"
    },
    "npm:amdefine@0.1.1": {
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "module": "github:jspm/nodelibs-module@0.1.0",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:asn1.js@2.0.4": {
      "assert": "github:jspm/nodelibs-assert@0.1.0",
      "bn.js": "npm:bn.js@2.0.5",
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "inherits": "npm:inherits@2.0.1",
      "minimalistic-assert": "npm:minimalistic-assert@1.0.0",
      "vm": "github:jspm/nodelibs-vm@0.1.0"
    },
    "npm:assert@1.3.0": {
      "util": "npm:util@0.10.3"
    },
    "npm:browserify-aes@1.0.1": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "create-hash": "npm:create-hash@1.1.1",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "inherits": "npm:inherits@2.0.1",
      "stream": "github:jspm/nodelibs-stream@0.1.0",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0"
    },
    "npm:browserify-rsa@2.0.1": {
      "bn.js": "npm:bn.js@2.0.5",
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "constants": "github:jspm/nodelibs-constants@0.1.0",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0",
      "randombytes": "npm:randombytes@2.0.1"
    },
    "npm:browserify-sign@3.0.2": {
      "bn.js": "npm:bn.js@2.0.5",
      "browserify-rsa": "npm:browserify-rsa@2.0.1",
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "create-hash": "npm:create-hash@1.1.1",
      "create-hmac": "npm:create-hmac@1.1.3",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0",
      "elliptic": "npm:elliptic@3.1.0",
      "inherits": "npm:inherits@2.0.1",
      "parse-asn1": "npm:parse-asn1@3.0.1",
      "stream": "github:jspm/nodelibs-stream@0.1.0",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0"
    },
    "npm:buffer@3.2.2": {
      "base64-js": "npm:base64-js@0.0.8",
      "ieee754": "npm:ieee754@1.1.6",
      "is-array": "npm:is-array@1.0.1"
    },
    "npm:clean-css@3.1.9": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "commander": "npm:commander@2.6.0",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "http": "github:jspm/nodelibs-http@1.7.1",
      "https": "github:jspm/nodelibs-https@0.1.0",
      "os": "github:jspm/nodelibs-os@0.1.0",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "source-map": "npm:source-map@0.1.43",
      "url": "github:jspm/nodelibs-url@0.1.0",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:commander@2.6.0": {
      "child_process": "github:jspm/nodelibs-child_process@0.1.0",
      "events": "github:jspm/nodelibs-events@0.1.1",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:constants-browserify@0.0.1": {
      "systemjs-json": "github:systemjs/plugin-json@0.1.0"
    },
    "npm:core-util-is@1.0.1": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0"
    },
    "npm:create-ecdh@2.0.1": {
      "bn.js": "npm:bn.js@2.0.5",
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0",
      "elliptic": "npm:elliptic@3.1.0"
    },
    "npm:create-hash@1.1.1": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "inherits": "npm:inherits@2.0.1",
      "ripemd160": "npm:ripemd160@1.0.1",
      "sha.js": "npm:sha.js@2.4.2",
      "stream": "github:jspm/nodelibs-stream@0.1.0"
    },
    "npm:create-hmac@1.1.3": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "create-hash": "npm:create-hash@1.1.1",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0",
      "inherits": "npm:inherits@2.0.1",
      "stream": "github:jspm/nodelibs-stream@0.1.0"
    },
    "npm:crypto-browserify@3.9.14": {
      "browserify-aes": "npm:browserify-aes@1.0.1",
      "browserify-sign": "npm:browserify-sign@3.0.2",
      "create-ecdh": "npm:create-ecdh@2.0.1",
      "create-hash": "npm:create-hash@1.1.1",
      "create-hmac": "npm:create-hmac@1.1.3",
      "diffie-hellman": "npm:diffie-hellman@3.0.2",
      "inherits": "npm:inherits@2.0.1",
      "pbkdf2": "npm:pbkdf2@3.0.4",
      "public-encrypt": "npm:public-encrypt@2.0.1",
      "randombytes": "npm:randombytes@2.0.1"
    },
    "npm:debug@2.1.3": {
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "ms": "npm:ms@0.7.0",
      "net": "github:jspm/nodelibs-net@0.1.2",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "tty": "github:jspm/nodelibs-tty@0.1.0",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:debug@2.2.0": {
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "ms": "npm:ms@0.7.1",
      "net": "github:jspm/nodelibs-net@0.1.2",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "tty": "github:jspm/nodelibs-tty@0.1.0",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:diffie-hellman@3.0.2": {
      "bn.js": "npm:bn.js@2.0.5",
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0",
      "miller-rabin": "npm:miller-rabin@2.0.1",
      "randombytes": "npm:randombytes@2.0.1",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0"
    },
    "npm:elliptic@3.1.0": {
      "bn.js": "npm:bn.js@2.0.5",
      "brorand": "npm:brorand@1.0.5",
      "hash.js": "npm:hash.js@1.0.3",
      "inherits": "npm:inherits@2.0.1",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0"
    },
    "npm:hash.js@1.0.3": {
      "inherits": "npm:inherits@2.0.1"
    },
    "npm:https-browserify@0.0.0": {
      "http": "github:jspm/nodelibs-http@1.7.1"
    },
    "npm:inherits@2.0.1": {
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:jsonp@0.2.0": {
      "debug": "npm:debug@2.1.3"
    },
    "npm:miller-rabin@2.0.1": {
      "bn.js": "npm:bn.js@2.0.5",
      "brorand": "npm:brorand@1.0.5"
    },
    "npm:os-browserify@0.1.2": {
      "os": "github:jspm/nodelibs-os@0.1.0"
    },
    "npm:parse-asn1@3.0.1": {
      "asn1.js": "npm:asn1.js@2.0.4",
      "browserify-aes": "npm:browserify-aes@1.0.1",
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "create-hash": "npm:create-hash@1.1.1",
      "pbkdf2": "npm:pbkdf2@3.0.4",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0"
    },
    "npm:path-browserify@0.0.0": {
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:pbkdf2@3.0.4": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "child_process": "github:jspm/nodelibs-child_process@0.1.0",
      "create-hmac": "npm:create-hmac@1.1.3",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0"
    },
    "npm:process@0.11.1": {
      "assert": "github:jspm/nodelibs-assert@0.1.0"
    },
    "npm:public-encrypt@2.0.1": {
      "bn.js": "npm:bn.js@2.0.5",
      "browserify-rsa": "npm:browserify-rsa@2.0.1",
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "create-hash": "npm:create-hash@1.1.1",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0",
      "parse-asn1": "npm:parse-asn1@3.0.1",
      "randombytes": "npm:randombytes@2.0.1"
    },
    "npm:punycode@1.3.2": {
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:randombytes@2.0.1": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:readable-stream@1.1.13": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "core-util-is": "npm:core-util-is@1.0.1",
      "events": "github:jspm/nodelibs-events@0.1.1",
      "inherits": "npm:inherits@2.0.1",
      "isarray": "npm:isarray@0.0.1",
      "process": "github:jspm/nodelibs-process@0.1.1",
      "stream": "github:jspm/nodelibs-stream@0.1.0",
      "stream-browserify": "npm:stream-browserify@1.0.0",
      "string_decoder": "npm:string_decoder@0.10.31",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:reflect-metadata@0.1.0": {
      "assert": "github:jspm/nodelibs-assert@0.1.0",
      "crypto": "github:jspm/nodelibs-crypto@0.1.0"
    },
    "npm:ripemd160@1.0.1": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:rx@2.5.3": {
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:sha.js@2.4.2": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "inherits": "npm:inherits@2.0.1",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:source-map@0.1.43": {
      "amdefine": "npm:amdefine@0.1.1",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:stream-browserify@1.0.0": {
      "events": "github:jspm/nodelibs-events@0.1.1",
      "inherits": "npm:inherits@2.0.1",
      "readable-stream": "npm:readable-stream@1.1.13"
    },
    "npm:string_decoder@0.10.31": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0"
    },
    "npm:timers-browserify@1.4.1": {
      "process": "npm:process@0.11.1"
    },
    "npm:url@0.10.3": {
      "assert": "github:jspm/nodelibs-assert@0.1.0",
      "punycode": "npm:punycode@1.3.2",
      "querystring": "npm:querystring@0.2.0",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:util@0.10.3": {
      "inherits": "npm:inherits@2.0.1",
      "process": "github:jspm/nodelibs-process@0.1.1"
    },
    "npm:vm-browserify@0.0.4": {
      "indexof": "npm:indexof@0.0.1"
    },
    "npm:when@3.7.3": {
      "process": "github:jspm/nodelibs-process@0.1.1"
    }
  }
});

