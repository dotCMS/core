import {DOM} from 'angular2/src/dom/dom_adapter';
import {List,
  ListWrapper} from 'angular2/src/facade/collection';
import {StringWrapper,
  RegExp,
  RegExpWrapper,
  RegExpMatcherWrapper,
  isPresent,
  isBlank,
  BaseException,
  int} from 'angular2/src/facade/lang';
export class ShadowCss {
  constructor() {
    this.strictStyling = true;
  }
  shimStyle(style, selector, hostSelector = '') {
    var cssText = DOM.getText(style);
    return this.shimCssText(cssText, selector, hostSelector);
  }
  shimCssText(cssText, selector, hostSelector = '') {
    cssText = this._insertDirectives(cssText);
    return this._scopeCssText(cssText, selector, hostSelector);
  }
  _insertDirectives(cssText) {
    cssText = this._insertPolyfillDirectivesInCssText(cssText);
    return this._insertPolyfillRulesInCssText(cssText);
  }
  _insertPolyfillDirectivesInCssText(cssText) {
    return StringWrapper.replaceAllMapped(cssText, _cssContentNextSelectorRe, function(m) {
      return m[1] + '{';
    });
  }
  _insertPolyfillRulesInCssText(cssText) {
    return StringWrapper.replaceAllMapped(cssText, _cssContentRuleRe, function(m) {
      var rule = m[0];
      rule = StringWrapper.replace(rule, m[1], '');
      rule = StringWrapper.replace(rule, m[2], '');
      return m[3] + rule;
    });
  }
  _scopeCssText(cssText, scopeSelector, hostSelector) {
    var unscoped = this._extractUnscopedRulesFromCssText(cssText);
    cssText = this._insertPolyfillHostInCssText(cssText);
    cssText = this._convertColonHost(cssText);
    cssText = this._convertColonHostContext(cssText);
    cssText = this._convertShadowDOMSelectors(cssText);
    if (isPresent(scopeSelector)) {
      _withCssRules(cssText, (rules) => {
        cssText = this._scopeRules(rules, scopeSelector, hostSelector);
      });
    }
    cssText = cssText + '\n' + unscoped;
    return cssText.trim();
  }
  _extractUnscopedRulesFromCssText(cssText) {
    var r = '',
        m;
    var matcher = RegExpWrapper.matcher(_cssContentUnscopedRuleRe, cssText);
    while (isPresent(m = RegExpMatcherWrapper.next(matcher))) {
      var rule = m[0];
      rule = StringWrapper.replace(rule, m[2], '');
      rule = StringWrapper.replace(rule, m[1], m[3]);
      r = rule + '\n\n';
    }
    return r;
  }
  _convertColonHost(cssText) {
    return this._convertColonRule(cssText, _cssColonHostRe, this._colonHostPartReplacer);
  }
  _convertColonHostContext(cssText) {
    return this._convertColonRule(cssText, _cssColonHostContextRe, this._colonHostContextPartReplacer);
  }
  _convertColonRule(cssText, regExp, partReplacer) {
    return StringWrapper.replaceAllMapped(cssText, regExp, function(m) {
      if (isPresent(m[2])) {
        var parts = m[2].split(','),
            r = [];
        for (var i = 0; i < parts.length; i++) {
          var p = parts[i];
          if (isBlank(p))
            break;
          p = p.trim();
          ListWrapper.push(r, partReplacer(_polyfillHostNoCombinator, p, m[3]));
        }
        return r.join(',');
      } else {
        return _polyfillHostNoCombinator + m[3];
      }
    });
  }
  _colonHostContextPartReplacer(host, part, suffix) {
    if (StringWrapper.contains(part, _polyfillHost)) {
      return this._colonHostPartReplacer(host, part, suffix);
    } else {
      return host + part + suffix + ', ' + part + ' ' + host + suffix;
    }
  }
  _colonHostPartReplacer(host, part, suffix) {
    return host + StringWrapper.replace(part, _polyfillHost, '') + suffix;
  }
  _convertShadowDOMSelectors(cssText) {
    for (var i = 0; i < _shadowDOMSelectorsRe.length; i++) {
      cssText = StringWrapper.replaceAll(cssText, _shadowDOMSelectorsRe[i], ' ');
    }
    return cssText;
  }
  _scopeRules(cssRules, scopeSelector, hostSelector) {
    var cssText = '';
    if (isPresent(cssRules)) {
      for (var i = 0; i < cssRules.length; i++) {
        var rule = cssRules[i];
        if (DOM.isStyleRule(rule) || DOM.isPageRule(rule)) {
          cssText += this._scopeSelector(rule.selectorText, scopeSelector, hostSelector, this.strictStyling) + ' {\n';
          cssText += this._propertiesFromRule(rule) + '\n}\n\n';
        } else if (DOM.isMediaRule(rule)) {
          cssText += '@media ' + rule.media.mediaText + ' {\n';
          cssText += this._scopeRules(rule.cssRules, scopeSelector, hostSelector);
          cssText += '\n}\n\n';
        } else {
          try {
            if (isPresent(rule.cssText)) {
              cssText += rule.cssText + '\n\n';
            }
          } catch (x) {
            if (DOM.isKeyframesRule(rule) && isPresent(rule.cssRules)) {
              cssText += this._ieSafeCssTextFromKeyFrameRule(rule);
            }
          }
        }
      }
    }
    return cssText;
  }
  _ieSafeCssTextFromKeyFrameRule(rule) {
    var cssText = '@keyframes ' + rule.name + ' {';
    for (var i = 0; i < rule.cssRules.length; i++) {
      var r = rule.cssRules[i];
      cssText += ' ' + r.keyText + ' {' + r.style.cssText + '}';
    }
    cssText += ' }';
    return cssText;
  }
  _scopeSelector(selector, scopeSelector, hostSelector, strict) {
    var r = [],
        parts = selector.split(',');
    for (var i = 0; i < parts.length; i++) {
      var p = parts[i];
      p = p.trim();
      if (this._selectorNeedsScoping(p, scopeSelector)) {
        p = strict && !StringWrapper.contains(p, _polyfillHostNoCombinator) ? this._applyStrictSelectorScope(p, scopeSelector) : this._applySelectorScope(p, scopeSelector, hostSelector);
      }
      ListWrapper.push(r, p);
    }
    return r.join(', ');
  }
  _selectorNeedsScoping(selector, scopeSelector) {
    var re = this._makeScopeMatcher(scopeSelector);
    return !isPresent(RegExpWrapper.firstMatch(re, selector));
  }
  _makeScopeMatcher(scopeSelector) {
    var lre = RegExpWrapper.create('\\[');
    var rre = RegExpWrapper.create('\\]');
    scopeSelector = StringWrapper.replaceAll(scopeSelector, lre, '\\[');
    scopeSelector = StringWrapper.replaceAll(scopeSelector, rre, '\\]');
    return RegExpWrapper.create('^(' + scopeSelector + ')' + _selectorReSuffix, 'm');
  }
  _applySelectorScope(selector, scopeSelector, hostSelector) {
    return this._applySimpleSelectorScope(selector, scopeSelector, hostSelector);
  }
  _applySimpleSelectorScope(selector, scopeSelector, hostSelector) {
    if (isPresent(RegExpWrapper.firstMatch(_polyfillHostRe, selector))) {
      var replaceBy = this.strictStyling ? `[${hostSelector}]` : scopeSelector;
      selector = StringWrapper.replace(selector, _polyfillHostNoCombinator, replaceBy);
      return StringWrapper.replaceAll(selector, _polyfillHostRe, replaceBy + ' ');
    } else {
      return scopeSelector + ' ' + selector;
    }
  }
  _applyStrictSelectorScope(selector, scopeSelector) {
    var isRe = RegExpWrapper.create('\\[is=([^\\]]*)\\]');
    scopeSelector = StringWrapper.replaceAllMapped(scopeSelector, isRe, (m) => m[1]);
    var splits = [' ', '>', '+', '~'],
        scoped = selector,
        attrName = '[' + scopeSelector + ']';
    for (var i = 0; i < splits.length; i++) {
      var sep = splits[i];
      var parts = scoped.split(sep);
      scoped = ListWrapper.map(parts, function(p) {
        var t = StringWrapper.replaceAll(p.trim(), _polyfillHostRe, '');
        if (t.length > 0 && !ListWrapper.contains(splits, t) && !StringWrapper.contains(t, attrName)) {
          var re = RegExpWrapper.create('([^:]*)(:*)(.*)');
          var m = RegExpWrapper.firstMatch(re, t);
          if (isPresent(m)) {
            p = m[1] + attrName + m[2] + m[3];
          }
        }
        return p;
      }).join(sep);
    }
    return scoped;
  }
  _insertPolyfillHostInCssText(selector) {
    selector = StringWrapper.replaceAll(selector, _colonHostContextRe, _polyfillHostContext);
    selector = StringWrapper.replaceAll(selector, _colonHostRe, _polyfillHost);
    return selector;
  }
  _propertiesFromRule(rule) {
    var cssText = rule.style.cssText;
    var attrRe = RegExpWrapper.create('[\'"]+|attr');
    if (rule.style.content.length > 0 && !isPresent(RegExpWrapper.firstMatch(attrRe, rule.style.content))) {
      var contentRe = RegExpWrapper.create('content:[^;]*;');
      cssText = StringWrapper.replaceAll(cssText, contentRe, 'content: \'' + rule.style.content + '\';');
    }
    return cssText;
  }
}
Object.defineProperty(ShadowCss.prototype.shimStyle, "parameters", {get: function() {
    return [[], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype.shimCssText, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._insertDirectives, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._insertPolyfillDirectivesInCssText, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._insertPolyfillRulesInCssText, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._scopeCssText, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._extractUnscopedRulesFromCssText, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._convertColonHost, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._convertColonHostContext, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._convertColonRule, "parameters", {get: function() {
    return [[assert.type.string], [RegExp], [Function]];
  }});
Object.defineProperty(ShadowCss.prototype._colonHostContextPartReplacer, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._colonHostPartReplacer, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._convertShadowDOMSelectors, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._scopeRules, "parameters", {get: function() {
    return [[], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._scopeSelector, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], [assert.type.string], [assert.type.boolean]];
  }});
Object.defineProperty(ShadowCss.prototype._selectorNeedsScoping, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._makeScopeMatcher, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._applySelectorScope, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._applySimpleSelectorScope, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._applyStrictSelectorScope, "parameters", {get: function() {
    return [[assert.type.string], [assert.type.string]];
  }});
Object.defineProperty(ShadowCss.prototype._insertPolyfillHostInCssText, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
var _cssContentNextSelectorRe = RegExpWrapper.create('polyfill-next-selector[^}]*content:[\\s]*?[\'"](.*?)[\'"][;\\s]*}([^{]*?){', 'im');
var _cssContentRuleRe = RegExpWrapper.create('(polyfill-rule)[^}]*(content:[\\s]*[\'"](.*?)[\'"])[;\\s]*[^}]*}', 'im');
var _cssContentUnscopedRuleRe = RegExpWrapper.create('(polyfill-unscoped-rule)[^}]*(content:[\\s]*[\'"](.*?)[\'"])[;\\s]*[^}]*}', 'im');
var _polyfillHost = '-shadowcsshost';
var _polyfillHostContext = '-shadowcsscontext';
var _parenSuffix = ')(?:\\((' + '(?:\\([^)(]*\\)|[^)(]*)+?' + ')\\))?([^,{]*)';
var _cssColonHostRe = RegExpWrapper.create('(' + _polyfillHost + _parenSuffix, 'im');
var _cssColonHostContextRe = RegExpWrapper.create('(' + _polyfillHostContext + _parenSuffix, 'im');
var _polyfillHostNoCombinator = _polyfillHost + '-no-combinator';
var _shadowDOMSelectorsRe = [RegExpWrapper.create('>>>'), RegExpWrapper.create('::shadow'), RegExpWrapper.create('::content'), RegExpWrapper.create('/deep/'), RegExpWrapper.create('/shadow-deep/'), RegExpWrapper.create('/shadow/')];
var _selectorReSuffix = '([>\\s~+\[.,{:][\\s\\S]*)?$';
var _polyfillHostRe = RegExpWrapper.create(_polyfillHost, 'im');
var _colonHostRe = RegExpWrapper.create(':host', 'im');
var _colonHostContextRe = RegExpWrapper.create(':host-context', 'im');
function _cssToRules(cssText) {
  return DOM.cssToRules(cssText);
}
Object.defineProperty(_cssToRules, "parameters", {get: function() {
    return [[assert.type.string]];
  }});
function _withCssRules(cssText, callback) {
  if (isBlank(callback))
    return ;
  var rules = _cssToRules(cssText);
  callback(rules);
}
Object.defineProperty(_withCssRules, "parameters", {get: function() {
    return [[assert.type.string], [Function]];
  }});
//# sourceMappingURL=shadow_css.js.map

//# sourceMappingURL=./shadow_css.map