"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _utils = require("@typescript-eslint/utils");

var _utils2 = require("./utils");

var _default = (0, _utils2.createRule)({
  name: __filename,
  meta: {
    docs: {
      category: 'Best Practices',
      description: 'Prefer `await expect(...).resolves` over `expect(await ...)` syntax',
      recommended: false
    },
    fixable: 'code',
    messages: {
      expectResolves: 'Use `await expect(...).resolves instead.'
    },
    schema: [],
    type: 'suggestion'
  },
  defaultOptions: [],
  create: context => ({
    CallExpression(node) {
      const [awaitNode] = node.arguments;

      if ((0, _utils2.isExpectCall)(node) && (awaitNode === null || awaitNode === void 0 ? void 0 : awaitNode.type) === _utils.AST_NODE_TYPES.AwaitExpression) {
        context.report({
          node: node.arguments[0],
          messageId: 'expectResolves',

          fix(fixer) {
            return [fixer.insertTextBefore(node, 'await '), fixer.removeRange([awaitNode.range[0], awaitNode.argument.range[0]]), fixer.insertTextAfter(node, '.resolves')];
          }

        });
      }
    }

  })
});

exports.default = _default;