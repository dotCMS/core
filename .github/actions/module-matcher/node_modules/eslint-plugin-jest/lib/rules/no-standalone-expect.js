"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _utils = require("@typescript-eslint/utils");

var _utils2 = require("./utils");

const getBlockType = (statement, scope) => {
  const func = statement.parent;
  /* istanbul ignore if */

  if (!func) {
    throw new Error(`Unexpected BlockStatement. No parent defined. - please file a github issue at https://github.com/jest-community/eslint-plugin-jest`);
  } // functionDeclaration: function func() {}


  if (func.type === _utils.AST_NODE_TYPES.FunctionDeclaration) {
    return 'function';
  }

  if ((0, _utils2.isFunction)(func) && func.parent) {
    const expr = func.parent; // arrow function or function expr

    if (expr.type === _utils.AST_NODE_TYPES.VariableDeclarator) {
      return 'function';
    } // if it's not a variable, it will be callExpr, we only care about describe


    if (expr.type === _utils.AST_NODE_TYPES.CallExpression && (0, _utils2.isDescribeCall)(expr, scope)) {
      return 'describe';
    }
  }

  return null;
};

var _default = (0, _utils2.createRule)({
  name: __filename,
  meta: {
    docs: {
      category: 'Best Practices',
      description: 'Disallow using `expect` outside of `it` or `test` blocks',
      recommended: 'error'
    },
    messages: {
      unexpectedExpect: 'Expect must be inside of a test block.'
    },
    type: 'suggestion',
    schema: [{
      properties: {
        additionalTestBlockFunctions: {
          type: 'array',
          items: {
            type: 'string'
          }
        }
      },
      additionalProperties: false
    }]
  },
  defaultOptions: [{
    additionalTestBlockFunctions: []
  }],

  create(context, [{
    additionalTestBlockFunctions = []
  }]) {
    const callStack = [];

    const isCustomTestBlockFunction = node => additionalTestBlockFunctions.includes((0, _utils2.getNodeName)(node) || '');

    const isTestBlock = node => (0, _utils2.isTestCaseCall)(node, context.getScope()) || isCustomTestBlockFunction(node);

    return {
      CallExpression(node) {
        if ((0, _utils2.isExpectCall)(node)) {
          const parent = callStack[callStack.length - 1];

          if (!parent || parent === _utils2.DescribeAlias.describe) {
            context.report({
              node,
              messageId: 'unexpectedExpect'
            });
          }

          return;
        }

        if (isTestBlock(node)) {
          callStack.push('test');
        }

        if (node.callee.type === _utils.AST_NODE_TYPES.TaggedTemplateExpression) {
          callStack.push('template');
        }
      },

      'CallExpression:exit'(node) {
        const top = callStack[callStack.length - 1];

        if (top === 'test' && isTestBlock(node) && node.callee.type !== _utils.AST_NODE_TYPES.MemberExpression || top === 'template' && node.callee.type === _utils.AST_NODE_TYPES.TaggedTemplateExpression) {
          callStack.pop();
        }
      },

      BlockStatement(statement) {
        const blockType = getBlockType(statement, context.getScope());

        if (blockType) {
          callStack.push(blockType);
        }
      },

      'BlockStatement:exit'(statement) {
        if (callStack[callStack.length - 1] === getBlockType(statement, context.getScope())) {
          callStack.pop();
        }
      },

      ArrowFunctionExpression(node) {
        var _node$parent;

        if (((_node$parent = node.parent) === null || _node$parent === void 0 ? void 0 : _node$parent.type) !== _utils.AST_NODE_TYPES.CallExpression) {
          callStack.push('arrow');
        }
      },

      'ArrowFunctionExpression:exit'() {
        if (callStack[callStack.length - 1] === 'arrow') {
          callStack.pop();
        }
      }

    };
  }

});

exports.default = _default;