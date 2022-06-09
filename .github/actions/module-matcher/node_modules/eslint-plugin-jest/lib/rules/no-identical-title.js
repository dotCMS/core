"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _utils = require("./utils");

const newDescribeContext = () => ({
  describeTitles: [],
  testTitles: []
});

var _default = (0, _utils.createRule)({
  name: __filename,
  meta: {
    docs: {
      category: 'Best Practices',
      description: 'Disallow identical titles',
      recommended: 'error'
    },
    messages: {
      multipleTestTitle: 'Test title is used multiple times in the same describe block.',
      multipleDescribeTitle: 'Describe block title is used multiple times in the same describe block.'
    },
    schema: [],
    type: 'suggestion'
  },
  defaultOptions: [],

  create(context) {
    const contexts = [newDescribeContext()];
    return {
      CallExpression(node) {
        var _getNodeName;

        const scope = context.getScope();
        const currentLayer = contexts[contexts.length - 1];

        if ((0, _utils.isDescribeCall)(node, scope)) {
          contexts.push(newDescribeContext());
        }

        if ((_getNodeName = (0, _utils.getNodeName)(node.callee)) !== null && _getNodeName !== void 0 && _getNodeName.endsWith('.each')) {
          return;
        }

        const [argument] = node.arguments;

        if (!argument || !(0, _utils.isStringNode)(argument)) {
          return;
        }

        const title = (0, _utils.getStringValue)(argument);

        if ((0, _utils.isTestCaseCall)(node, scope)) {
          if (currentLayer.testTitles.includes(title)) {
            context.report({
              messageId: 'multipleTestTitle',
              node: argument
            });
          }

          currentLayer.testTitles.push(title);
        }

        if (!(0, _utils.isDescribeCall)(node, scope)) {
          return;
        }

        if (currentLayer.describeTitles.includes(title)) {
          context.report({
            messageId: 'multipleDescribeTitle',
            node: argument
          });
        }

        currentLayer.describeTitles.push(title);
      },

      'CallExpression:exit'(node) {
        if ((0, _utils.isDescribeCall)(node, context.getScope())) {
          contexts.pop();
        }
      }

    };
  }

});

exports.default = _default;