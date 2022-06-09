"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _utils = require("./utils");

const snapshotMatchers = ['toMatchSnapshot', 'toThrowErrorMatchingSnapshot'];

const isSnapshotMatcher = matcher => {
  return snapshotMatchers.includes(matcher.name);
};

const isSnapshotMatcherWithoutHint = matcher => {
  if (!matcher.arguments || matcher.arguments.length === 0) {
    return true;
  } // this matcher only supports one argument which is the hint


  if (matcher.name !== 'toMatchSnapshot') {
    return matcher.arguments.length !== 1;
  } // if we're being passed two arguments,
  // the second one should be the hint


  if (matcher.arguments.length === 2) {
    return false;
  }

  const [arg] = matcher.arguments; // the first argument to `toMatchSnapshot` can be _either_ a snapshot hint or
  // an object with asymmetric matchers, so we can't just assume that the first
  // argument is a hint when it's by itself.

  return !(0, _utils.isStringNode)(arg);
};

const messages = {
  missingHint: 'You should provide a hint for this snapshot'
};

var _default = (0, _utils.createRule)({
  name: __filename,
  meta: {
    docs: {
      category: 'Best Practices',
      description: 'Prefer including a hint with external snapshots',
      recommended: false
    },
    messages,
    type: 'suggestion',
    schema: [{
      type: 'string',
      enum: ['always', 'multi']
    }]
  },
  defaultOptions: ['multi'],

  create(context, [mode]) {
    const snapshotMatchers = [];
    const depths = [];
    let expressionDepth = 0;

    const reportSnapshotMatchersWithoutHints = () => {
      for (const snapshotMatcher of snapshotMatchers) {
        if (isSnapshotMatcherWithoutHint(snapshotMatcher)) {
          context.report({
            messageId: 'missingHint',
            node: snapshotMatcher.node.property
          });
        }
      }
    };

    const enterExpression = () => {
      expressionDepth++;
    };

    const exitExpression = () => {
      expressionDepth--;

      if (mode === 'always') {
        reportSnapshotMatchersWithoutHints();
        snapshotMatchers.length = 0;
      }

      if (mode === 'multi' && expressionDepth === 0) {
        if (snapshotMatchers.length > 1) {
          reportSnapshotMatchersWithoutHints();
        }

        snapshotMatchers.length = 0;
      }
    };

    return {
      'Program:exit'() {
        enterExpression();
        exitExpression();
      },

      FunctionExpression: enterExpression,
      'FunctionExpression:exit': exitExpression,
      ArrowFunctionExpression: enterExpression,
      'ArrowFunctionExpression:exit': exitExpression,

      'CallExpression:exit'(node) {
        const scope = context.getScope();

        if ((0, _utils.isDescribeCall)(node, scope) || (0, _utils.isTestCaseCall)(node, scope)) {
          var _depths$pop;

          /* istanbul ignore next */
          expressionDepth = (_depths$pop = depths.pop()) !== null && _depths$pop !== void 0 ? _depths$pop : 0;
        }
      },

      CallExpression(node) {
        const scope = context.getScope();

        if ((0, _utils.isDescribeCall)(node, scope) || (0, _utils.isTestCaseCall)(node, scope)) {
          depths.push(expressionDepth);
          expressionDepth = 0;
        }

        if (!(0, _utils.isExpectCall)(node)) {
          return;
        }

        const {
          matcher
        } = (0, _utils.parseExpectCall)(node);

        if (!matcher || !isSnapshotMatcher(matcher)) {
          return;
        }

        snapshotMatchers.push(matcher);
      }

    };
  }

});

exports.default = _default;