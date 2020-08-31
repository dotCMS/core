import { h } from '../dotcmsfields.core.js';

import { a as Fragment } from './chunk-1d89c98b.js';
import { a as getOriginalStatus, b as checkProp, c as getClassNames, h as getHintId, i as getErrorClass, d as getTagHint, e as getTagError, f as updateStatus, m as isStringType } from './chunk-62cd3eff.js';
import { a as createCommonjsModule, b as commonjsGlobal } from './chunk-0e32e502.js';

var autoComplete = createCommonjsModule(function (module, exports) {
(function (global, factory) {
  module.exports = factory();
}(commonjsGlobal, (function () {
  function _classCallCheck(instance, Constructor) {
    if (!(instance instanceof Constructor)) {
      throw new TypeError("Cannot call a class as a function");
    }
  }

  function _defineProperties(target, props) {
    for (var i = 0; i < props.length; i++) {
      var descriptor = props[i];
      descriptor.enumerable = descriptor.enumerable || false;
      descriptor.configurable = true;
      if ("value" in descriptor) descriptor.writable = true;
      Object.defineProperty(target, descriptor.key, descriptor);
    }
  }

  function _createClass(Constructor, protoProps, staticProps) {
    if (protoProps) _defineProperties(Constructor.prototype, protoProps);
    if (staticProps) _defineProperties(Constructor, staticProps);
    return Constructor;
  }

  var dataAttribute = "data-result";
  var select = {
    resultsList: "autoComplete_results_list",
    result: "autoComplete_result",
    highlight: "autoComplete_highlighted"
  };
  var getInput = function getInput(selector) {
    return typeof selector === "string" ? document.querySelector(selector) : selector();
  };
  var createResultsList = function createResultsList(renderResults) {
    var resultsList = document.createElement("ul");
    if (renderResults.container) {
      select.resultsList = renderResults.container(resultsList) || select.resultsList;
    }
    resultsList.setAttribute("id", select.resultsList);
    renderResults.destination.insertAdjacentElement(renderResults.position, resultsList);
    return resultsList;
  };
  var highlight = function highlight(value) {
    return "<span class=".concat(select.highlight, ">").concat(value, "</span>");
  };
  var addResultsToList = function addResultsToList(resultsList, dataSrc, dataKey, callback) {
    dataSrc.forEach(function (event, record) {
      var result = document.createElement("li");
      var resultValue = dataSrc[record].value[event.key] || dataSrc[record].value;
      result.setAttribute(dataAttribute, resultValue);
      result.setAttribute("class", select.result);
      result.setAttribute("tabindex", "1");
      result.innerHTML = callback ? callback(event, result) : event.match || event;
      resultsList.appendChild(result);
    });
  };
  var navigation = function navigation(selector, resultsList) {
    var input = getInput(selector);
    var first = resultsList.firstChild;
    document.onkeydown = function (event) {
      var active = document.activeElement;
      switch (event.keyCode) {
        case 38:
          if (active !== first && active !== input) {
            active.previousSibling.focus();
          } else if (active === first) {
            input.focus();
          }
          break;
        case 40:
          if (active === input && resultsList.childNodes.length > 0) {
            first.focus();
          } else if (active !== resultsList.lastChild) {
            active.nextSibling.focus();
          }
          break;
      }
    };
  };
  var clearResults = function clearResults(resultsList) {
    return resultsList.innerHTML = "";
  };
  var getSelection = function getSelection(field, resultsList, callback, resultsValues) {
    var results = resultsList.querySelectorAll(".".concat(select.result));
    Object.keys(results).forEach(function (selection) {
      ["mousedown", "keydown"].forEach(function (eventType) {
        results[selection].addEventListener(eventType, function (event) {
          if (eventType === "mousedown" || event.keyCode === 13 || event.keyCode === 39) {
            callback({
              event: event,
              query: getInput(field).value,
              matches: resultsValues.matches,
              results: resultsValues.list.map(function (record) {
                return record.value;
              }),
              selection: resultsValues.list.find(function (value) {
                var resValue = value.value[value.key] || value.value;
                return resValue === event.target.closest(".".concat(select.result)).getAttribute(dataAttribute);
              })
            });
            clearResults(resultsList);
          }
        });
      });
    });
  };
  var autoCompleteView = {
    getInput: getInput,
    createResultsList: createResultsList,
    highlight: highlight,
    addResultsToList: addResultsToList,
    navigation: navigation,
    clearResults: clearResults,
    getSelection: getSelection
  };

  var autoComplete =
  function () {
    function autoComplete(config) {
      _classCallCheck(this, autoComplete);
      this.selector = config.selector || "#autoComplete";
      this.data = {
        src: function src() {
          return typeof config.data.src === "function" ? config.data.src() : config.data.src;
        },
        key: config.data.key
      };
      this.searchEngine = config.searchEngine === "loose" ? "loose" : "strict";
      this.threshold = config.threshold || 0;
      this.resultsList = autoCompleteView.createResultsList({
        container: config.resultsList && config.resultsList.container ? config.resultsList.container : false,
        destination: config.resultsList && config.resultsList.destination ? config.resultsList.destination : autoCompleteView.getInput(this.selector),
        position: config.resultsList && config.resultsList.position ? config.resultsList.position : "afterend"
      });
      this.sort = config.sort || false;
      this.placeHolder = config.placeHolder;
      this.maxResults = config.maxResults || 5;
      this.resultItem = config.resultItem;
      this.highlight = config.highlight || false;
      this.onSelection = config.onSelection;
      this.init();
    }
    _createClass(autoComplete, [{
      key: "search",
      value: function search(query, record) {
        var highlight = this.highlight;
        var recordLowerCase = record.toLowerCase();
        if (this.searchEngine === "loose") {
          query = query.replace(/ /g, "");
          var match = [];
          var searchPosition = 0;
          for (var number = 0; number < recordLowerCase.length; number++) {
            var recordChar = record[number];
            if (searchPosition < query.length && recordLowerCase[number] === query[searchPosition]) {
              recordChar = highlight ? autoCompleteView.highlight(recordChar) : recordChar;
              searchPosition++;
            }
            match.push(recordChar);
          }
          if (searchPosition !== query.length) {
            return false;
          }
          return match.join("");
        } else {
          if (recordLowerCase.includes(query)) {
            var pattern = new RegExp("".concat(query), "i");
            query = pattern.exec(record);
            return highlight ? record.replace(query, autoCompleteView.highlight(query)) : record;
          }
        }
      }
    }, {
      key: "listMatchedResults",
      value: function listMatchedResults(data) {
        var _this = this;
        var resList = [];
        var inputValue = autoCompleteView.getInput(this.selector).value.toLowerCase();
        data.filter(function (record, index) {
          var search = function search(key) {
            var match = _this.search(inputValue, record[key] || record);
            if (match && key) {
              resList.push({
                key: key,
                index: index,
                match: match,
                value: record
              });
            } else if (match && !key) {
              resList.push({
                index: index,
                match: match,
                value: record
              });
            }
          };
          if (_this.data.key) {
            var _iteratorNormalCompletion = true;
            var _didIteratorError = false;
            var _iteratorError = undefined;
            try {
              for (var _iterator = _this.data.key[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {
                var key = _step.value;
                search(key);
              }
            } catch (err) {
              _didIteratorError = true;
              _iteratorError = err;
            } finally {
              try {
                if (!_iteratorNormalCompletion && _iterator.return != null) {
                  _iterator.return();
                }
              } finally {
                if (_didIteratorError) {
                  throw _iteratorError;
                }
              }
            }
          } else {
            search();
          }
        });
        var list = this.sort ? resList.sort(this.sort).slice(0, this.maxResults) : resList.slice(0, this.maxResults);
        autoCompleteView.addResultsToList(this.resultsList, list, this.data.key, this.resultItem);
        autoCompleteView.navigation(this.selector, this.resultsList);
        return {
          matches: resList.length,
          list: list
        };
      }
    }, {
      key: "ignite",
      value: function ignite(data) {
        var _this2 = this;
        var selector = this.selector;
        var input = autoCompleteView.getInput(selector);
        var placeHolder = this.placeHolder;
        var onSelection = this.onSelection;
        if (placeHolder) {
          input.setAttribute("placeholder", placeHolder);
        }
        input.onkeyup = function (event) {
          var resultsList = _this2.resultsList;
          var clearResults = autoCompleteView.clearResults(resultsList);
          if (input.value.length > _this2.threshold && input.value.replace(/ /g, "").length) {
            var list = _this2.listMatchedResults(data);
            input.dispatchEvent(new CustomEvent("type", {
              bubbles: true,
              detail: {
                event: event,
                query: input.value,
                matches: list.matches,
                results: list.list
              },
              cancelable: true
            }));
            if (onSelection) {
              autoCompleteView.getSelection(selector, resultsList, onSelection, list);
            }
          }
        };
      }
    }, {
      key: "init",
      value: function init() {
        var _this3 = this;
        var dataSrc = this.data.src();
        if (dataSrc instanceof Promise) {
          dataSrc.then(function (data) {
            return _this3.ignite(data);
          });
        } else {
          this.ignite(dataSrc);
        }
      }
    }]);
    return autoComplete;
  }();

  return autoComplete;

})));
});

class DotAutocompleteComponent {
    constructor() {
        this.disabled = false;
        this.placeholder = '';
        this.threshold = 0;
        this.maxResults = 0;
        this.debounce = 300;
        this.data = null;
        this.id = `autoComplete${new Date().getTime()}`;
        this.keyEvent = {
            Enter: this.emitEnter.bind(this),
            Escape: this.clean.bind(this)
        };
    }
    componentDidLoad() {
        if (this.data) {
            this.initAutocomplete();
        }
    }
    render() {
        return (h("input", { autoComplete: "off", disabled: this.disabled || null, id: this.id, onBlur: (event) => this.handleBlur(event), onKeyDown: (event) => this.handleKeyDown(event), placeholder: this.placeholder || null }));
    }
    watchThreshold() {
        this.initAutocomplete();
    }
    watchData() {
        this.initAutocomplete();
    }
    watchMaxResults() {
        this.initAutocomplete();
    }
    handleKeyDown(event) {
        const { value } = this.getInputElement();
        if (value && this.keyEvent[event.key]) {
            event.preventDefault();
            this.keyEvent[event.key](value);
        }
    }
    handleBlur(event) {
        event.preventDefault();
        setTimeout(() => {
            if (document.activeElement.parentElement !== this.getResultList()) {
                this.clean();
                this.lostFocus.emit(event);
            }
        }, 0);
    }
    clean() {
        this.getInputElement().value = '';
        this.cleanOptions();
    }
    cleanOptions() {
        this.getResultList().innerHTML = '';
    }
    emitselect(select) {
        this.clean();
        this.selection.emit(select);
    }
    emitEnter(select) {
        if (select) {
            this.clean();
            this.enter.emit(select);
        }
    }
    getInputElement() {
        return this.el.querySelector(`#${this.id}`);
    }
    initAutocomplete() {
        this.clearList();
        new autoComplete({
            data: {
                src: async () => this.getData()
            },
            sort: (a, b) => {
                if (a.match < b.match) {
                    return -1;
                }
                if (a.match > b.match) {
                    return 1;
                }
                return 0;
            },
            placeHolder: this.placeholder,
            selector: `#${this.id}`,
            threshold: this.threshold,
            searchEngine: 'strict',
            highlight: true,
            maxResults: this.maxResults,
            debounce: this.debounce,
            resultsList: {
                container: () => this.getResultListId(),
                destination: this.getInputElement(),
                position: 'afterend'
            },
            resultItem: ({ match }) => match,
            onSelection: ({ event, selection }) => {
                event.preventDefault();
                this.focusOnInput();
                this.emitselect(selection.value);
            }
        });
    }
    clearList() {
        const list = this.getResultList();
        if (list) {
            list.remove();
        }
    }
    focusOnInput() {
        this.getInputElement().focus();
    }
    getResultList() {
        return this.el.querySelector(`#${this.getResultListId()}`);
    }
    getResultListId() {
        return `${this.id}_results_list`;
    }
    async getData() {
        const autocomplete = this.getInputElement();
        autocomplete.setAttribute('placeholder', 'Loading...');
        const data = typeof this.data === 'function' ? await this.data() : [];
        autocomplete.setAttribute('placeholder', this.placeholder || '');
        return data;
    }
    static get is() { return "dot-autocomplete"; }
    static get properties() { return {
        "data": {
            "type": "Any",
            "attr": "data",
            "watchCallbacks": ["watchData"]
        },
        "debounce": {
            "type": Number,
            "attr": "debounce",
            "reflectToAttr": true
        },
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "maxResults": {
            "type": Number,
            "attr": "max-results",
            "reflectToAttr": true,
            "watchCallbacks": ["watchMaxResults"]
        },
        "placeholder": {
            "type": String,
            "attr": "placeholder",
            "reflectToAttr": true
        },
        "threshold": {
            "type": Number,
            "attr": "threshold",
            "reflectToAttr": true,
            "watchCallbacks": ["watchThreshold"]
        }
    }; }
    static get events() { return [{
            "name": "selection",
            "method": "selection",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "enter",
            "method": "enter",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "lostFocus",
            "method": "lostFocus",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return "dot-autocomplete input{-webkit-box-sizing:border-box;box-sizing:border-box;width:200px}dot-autocomplete ul{background-color:#fff;list-style:none;margin:0;max-height:300px;overflow:auto;padding:0;position:absolute;width:200px}dot-autocomplete ul li{background-color:#fff;border:1px solid #ccc;-webkit-box-sizing:border-box;box-sizing:border-box;cursor:pointer;padding:.25rem}dot-autocomplete ul li:first-child{border-top:1px solid #ccc}dot-autocomplete ul li:focus{background-color:#ffffe0;outline:0}dot-autocomplete ul li .autoComplete_highlighted{font-weight:700}"; }
}

class DotChipComponent {
    constructor() {
        this.label = '';
        this.deleteLabel = 'Delete';
        this.disabled = false;
    }
    render() {
        const label = this.label ? `${this.deleteLabel} ${this.label}` : null;
        return (h(Fragment, null,
            h("span", null, this.label),
            h("button", { type: "button", "aria-label": label, disabled: this.disabled, onClick: () => this.remove.emit(this.label) }, this.deleteLabel)));
    }
    static get is() { return "dot-chip"; }
    static get properties() { return {
        "deleteLabel": {
            "type": String,
            "attr": "delete-label",
            "reflectToAttr": true
        },
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "label": {
            "type": String,
            "attr": "label",
            "reflectToAttr": true
        }
    }; }
    static get events() { return [{
            "name": "remove",
            "method": "remove",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return "dot-chip span{margin-right:.25rem}dot-chip button{cursor:pointer}"; }
}

class DotTagsComponent {
    constructor() {
        this.value = '';
        this.data = null;
        this.name = '';
        this.label = '';
        this.hint = '';
        this.placeholder = '';
        this.required = false;
        this.requiredMessage = 'This field is required';
        this.disabled = false;
        this.threshold = 0;
        this.debounce = 300;
    }
    reset() {
        this.value = '';
        this.status = getOriginalStatus(this.isValid());
        this.emitChanges();
    }
    valueWatch() {
        this.value = checkProp(this, 'value', 'string');
    }
    componentWillLoad() {
        this.status = getOriginalStatus(this.isValid());
        this.validateProps();
        this.emitStatusChange();
    }
    hostData() {
        return {
            class: getClassNames(this.status, this.isValid(), this.required)
        };
    }
    render() {
        return (h(Fragment, null,
            h("dot-label", { label: this.label, required: this.required, name: this.name },
                h("div", { "aria-describedby": getHintId(this.hint), tabIndex: this.hint ? 0 : null, class: "dot-tags__container" },
                    h("dot-autocomplete", { class: getErrorClass(this.status.dotValid), data: this.data, debounce: this.debounce, disabled: this.isDisabled(), onEnter: this.onEnterHandler.bind(this), onLostFocus: this.blurHandler.bind(this), onSelection: this.onSelectHandler.bind(this), placeholder: this.placeholder || null, threshold: this.threshold }),
                    h("div", { class: "dot-tags__chips" }, this.getValues().map((tagLab) => (h("dot-chip", { disabled: this.isDisabled(), label: tagLab, onRemove: this.removeTag.bind(this) })))))),
            getTagHint(this.hint),
            getTagError(this.showErrorMessage(), this.getErrorMessage())));
    }
    addTag(label) {
        const values = this.getValues();
        if (!values.includes(label)) {
            values.push(label);
            this.value = values.join(',');
            this.updateStatus();
            this.emitChanges();
        }
    }
    blurHandler() {
        if (!this.status.dotTouched) {
            this.status = updateStatus(this.status, {
                dotTouched: true
            });
            this.emitStatusChange();
        }
    }
    emitChanges() {
        this.emitStatusChange();
        this.emitValueChange();
    }
    emitStatusChange() {
        this.statusChange.emit({
            name: this.name,
            status: this.status
        });
    }
    emitValueChange() {
        this.valueChange.emit({
            name: this.name,
            value: this.value
        });
    }
    getErrorMessage() {
        return this.isValid() ? '' : this.requiredMessage;
    }
    getValues() {
        return isStringType(this.value) ? this.value.split(',') : [];
    }
    isDisabled() {
        return this.disabled || null;
    }
    isValid() {
        return !this.required || (this.required && !!this.value);
    }
    onEnterHandler({ detail = '' }) {
        detail.split(',').forEach((label) => {
            this.addTag(label.trim());
        });
    }
    onSelectHandler({ detail = '' }) {
        const value = detail.replace(',', ' ').replace(/\s+/g, ' ');
        this.addTag(value);
    }
    removeTag(event) {
        const values = this.getValues().filter((item) => item !== event.detail);
        this.value = values.join(',');
        this.updateStatus();
        this.emitChanges();
    }
    showErrorMessage() {
        return this.getErrorMessage() && !this.status.dotPristine;
    }
    updateStatus() {
        this.status = updateStatus(this.status, {
            dotTouched: true,
            dotPristine: false,
            dotValid: this.isValid()
        });
    }
    validateProps() {
        this.valueWatch();
    }
    static get is() { return "dot-tags"; }
    static get properties() { return {
        "data": {
            "type": "Any",
            "attr": "data"
        },
        "debounce": {
            "type": Number,
            "attr": "debounce",
            "reflectToAttr": true
        },
        "disabled": {
            "type": Boolean,
            "attr": "disabled",
            "reflectToAttr": true
        },
        "el": {
            "elementRef": true
        },
        "hint": {
            "type": String,
            "attr": "hint",
            "reflectToAttr": true
        },
        "label": {
            "type": String,
            "attr": "label",
            "reflectToAttr": true
        },
        "name": {
            "type": String,
            "attr": "name",
            "reflectToAttr": true
        },
        "placeholder": {
            "type": String,
            "attr": "placeholder",
            "reflectToAttr": true
        },
        "required": {
            "type": Boolean,
            "attr": "required",
            "reflectToAttr": true
        },
        "requiredMessage": {
            "type": String,
            "attr": "required-message",
            "reflectToAttr": true
        },
        "reset": {
            "method": true
        },
        "status": {
            "state": true
        },
        "threshold": {
            "type": Number,
            "attr": "threshold",
            "reflectToAttr": true
        },
        "value": {
            "type": String,
            "attr": "value",
            "reflectToAttr": true,
            "mutable": true,
            "watchCallbacks": ["valueWatch"]
        }
    }; }
    static get events() { return [{
            "name": "valueChange",
            "method": "valueChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }, {
            "name": "statusChange",
            "method": "statusChange",
            "bubbles": true,
            "cancelable": true,
            "composed": true
        }]; }
    static get style() { return "dot-tags .dot-tags__container{display:-ms-flexbox;display:flex;-ms-flex-align:start;align-items:flex-start;border:1px solid #d3d3d3}dot-tags .dot-tags__container dot-autocomplete{margin:.5rem 1rem .5rem .5rem}dot-tags .dot-tags__container .dot-tags__chips{margin:.5rem 1rem 0 0}dot-tags .dot-tags__container dot-chip{border:1px solid #ccc;display:inline-block;margin:0 .5rem .5rem 0;padding:.2rem}dot-tags button{border:0}"; }
}

export { DotAutocompleteComponent as DotAutocomplete, DotChipComponent as DotChip, DotTagsComponent as DotTags };
