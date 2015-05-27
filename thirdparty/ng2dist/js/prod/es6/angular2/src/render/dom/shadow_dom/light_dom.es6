import {DOM} from 'angular2/src/dom/dom_adapter';
import {List,
  ListWrapper} from 'angular2/src/facade/collection';
import {isBlank,
  isPresent} from 'angular2/src/facade/lang';
import * as viewModule from '../view/view';
import {Content} from './content_tag';
export class DestinationLightDom {}
class _Root {
  constructor(node, boundElementIndex) {
    this.node = node;
    this.boundElementIndex = boundElementIndex;
  }
}
export class LightDom {
  constructor(lightDomView, shadowDomView, element) {
    this.lightDomView = lightDomView;
    this.shadowDomView = shadowDomView;
    this.nodes = DOM.childNodesAsList(element);
    this.roots = null;
  }
  redistribute() {
    redistributeNodes(this.contentTags(), this.expandedDomNodes());
  }
  contentTags() {
    return this._collectAllContentTags(this.shadowDomView, []);
  }
  _collectAllContentTags(view, acc) {
    var contentTags = view.contentTags;
    var vcs = view.viewContainers;
    for (var i = 0; i < vcs.length; i++) {
      var vc = vcs[i];
      var contentTag = contentTags[i];
      if (isPresent(contentTag)) {
        ListWrapper.push(acc, contentTag);
      }
      if (isPresent(vc)) {
        ListWrapper.forEach(vc.contentTagContainers(), (view) => {
          this._collectAllContentTags(view, acc);
        });
      }
    }
    return acc;
  }
  expandedDomNodes() {
    var res = [];
    var roots = this._roots();
    for (var i = 0; i < roots.length; ++i) {
      var root = roots[i];
      if (isPresent(root.boundElementIndex)) {
        var vc = this.lightDomView.viewContainers[root.boundElementIndex];
        var content = this.lightDomView.contentTags[root.boundElementIndex];
        if (isPresent(vc)) {
          res = ListWrapper.concat(res, vc.nodes());
        } else if (isPresent(content)) {
          res = ListWrapper.concat(res, content.nodes());
        } else {
          ListWrapper.push(res, root.node);
        }
      } else {
        ListWrapper.push(res, root.node);
      }
    }
    return res;
  }
  _roots() {
    if (isPresent(this.roots))
      return this.roots;
    var boundElements = this.lightDomView.boundElements;
    this.roots = ListWrapper.map(this.nodes, (n) => {
      var boundElementIndex = null;
      for (var i = 0; i < boundElements.length; i++) {
        var boundEl = boundElements[i];
        if (isPresent(boundEl) && boundEl === n) {
          boundElementIndex = i;
          break;
        }
      }
      return new _Root(n, boundElementIndex);
    });
    return this.roots;
  }
}
Object.defineProperty(LightDom, "parameters", {get: function() {
    return [[viewModule.RenderView], [viewModule.RenderView], []];
  }});
Object.defineProperty(LightDom.prototype._collectAllContentTags, "parameters", {get: function() {
    return [[viewModule.RenderView], [assert.genericType(List, Content)]];
  }});
function redistributeNodes(contents, nodes) {
  for (var i = 0; i < contents.length; ++i) {
    var content = contents[i];
    var select = content.select;
    if (select.length === 0) {
      content.insert(ListWrapper.clone(nodes));
      ListWrapper.clear(nodes);
    } else {
      var matchSelector = (n) => DOM.elementMatches(n, select);
      var matchingNodes = ListWrapper.filter(nodes, matchSelector);
      content.insert(matchingNodes);
      ListWrapper.removeAll(nodes, matchingNodes);
    }
  }
  for (var i = 0; i < nodes.length; i++) {
    var node = nodes[i];
    if (isPresent(node.parentNode)) {
      DOM.remove(nodes[i]);
    }
  }
}
Object.defineProperty(redistributeNodes, "parameters", {get: function() {
    return [[assert.genericType(List, Content)], [List]];
  }});
//# sourceMappingURL=light_dom.js.map

//# sourceMappingURL=./light_dom.map