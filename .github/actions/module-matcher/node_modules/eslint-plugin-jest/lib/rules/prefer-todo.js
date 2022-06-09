"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _utils = require("@typescript-eslint/utils");

var _utils2 = require("./utils");

function isEmptyFunction(node) {
  if (!(0, _utils2.isFunction)(node)) {
    return false;
  }

  return node.body.type === _utils.AST_NODE_TYPES.BlockStatement && !node.body.body.length;
}

function createTodoFixer(node, fixer) {
  const testName = (0, _utils2.getNodeName)(node).split('.').shift();
  return fixer.replaceText(node.callee, `${testName}.todo`);
}

const isTargetedTestCase = (node, scope) => (0, _utils2.isTestCaseCall)(node, scope) && [_utils2.TestCaseName.it, _utils2.TestCaseName.test, 'it.skip', 'test.skip'].includes((0, _utils2.getNodeName)(node));

var _default = (0, _utils2.createRule)({
  name: __filename,
  meta: {
    docs: {
      category: 'Best Practices',
      description: 'Suggest using `test.todo`',
      recommended: false
    },
    messages: {
      emptyTest: 'Prefer todo test case over empty test case',
      unimplementedTest: 'Prefer todo test case over unimplemented test case'
    },
    fixable: 'code',
    schema: [],
    type: 'layout'
  },
  defaultOptions: [],

  create(context) {
    return {
      CallExpression(node) {
        const [title, callback] = node.arguments;

        if (!title || !isTargetedTestCase(node, context.getScope()) || !(0, _utils2.isStringNode)(title)) {
          return;
        }

        if (callback && isEmptyFunction(callback)) {
          context.report({
            messageId: 'emptyTest',
            node,
            fix: fixer => [fixer.removeRange([title.range[1], callback.range[1]]), createTodoFixer(node, fixer)]
          });
        }

        if ((0, _utils2.hasOnlyOneArgument)(node)) {
          context.report({
            messageId: 'unimplementedTest',
            node,
            fix: fixer => [createTodoFixer(node, fixer)]
          });
        }
      }

    };
  }

});

exports.default = _default;