"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _utils = require("@typescript-eslint/utils");

var _utils2 = require("./utils");

const isBooleanLiteral = node => node.type === _utils.AST_NODE_TYPES.Literal && typeof node.value === 'boolean';

/**
 * Checks if the given `ParsedExpectMatcher` is a call to one of the equality matchers,
 * with a boolean literal as the sole argument.
 *
 * @example javascript
 * toBe(true);
 * toEqual(false);
 *
 * @param {ParsedExpectMatcher} matcher
 *
 * @return {matcher is ParsedBooleanEqualityMatcher}
 */
const isBooleanEqualityMatcher = matcher => (0, _utils2.isParsedEqualityMatcherCall)(matcher) && isBooleanLiteral((0, _utils2.followTypeAssertionChain)(matcher.arguments[0]));

var _default = (0, _utils2.createRule)({
  name: __filename,
  meta: {
    docs: {
      category: 'Best Practices',
      description: 'Suggest using the built-in equality matchers',
      recommended: false,
      suggestion: true
    },
    messages: {
      useEqualityMatcher: 'Prefer using one of the equality matchers instead',
      suggestEqualityMatcher: 'Use `{{ equalityMatcher }}`'
    },
    hasSuggestions: true,
    type: 'suggestion',
    schema: []
  },
  defaultOptions: [],

  create(context) {
    return {
      CallExpression(node) {
        if (!(0, _utils2.isExpectCall)(node)) {
          return;
        }

        const {
          expect: {
            arguments: [comparison],
            range: [, expectCallEnd]
          },
          matcher,
          modifier
        } = (0, _utils2.parseExpectCall)(node);

        if (!matcher || (comparison === null || comparison === void 0 ? void 0 : comparison.type) !== _utils.AST_NODE_TYPES.BinaryExpression || comparison.operator !== '===' && comparison.operator !== '!==' || !isBooleanEqualityMatcher(matcher)) {
          return;
        }

        const matcherValue = (0, _utils2.followTypeAssertionChain)(matcher.arguments[0]).value; // we need to negate the expectation if the current expected
        // value is itself negated by the "not" modifier

        const addNotModifier = (comparison.operator === '!==' ? !matcherValue : matcherValue) === !!modifier;

        const buildFixer = equalityMatcher => fixer => {
          const sourceCode = context.getSourceCode();
          return [// replace the comparison argument with the left-hand side of the comparison
          fixer.replaceText(comparison, sourceCode.getText(comparison.left)), // replace the current matcher & modifier with the preferred matcher
          fixer.replaceTextRange([expectCallEnd, matcher.node.range[1]], addNotModifier ? `.${_utils2.ModifierName.not}.${equalityMatcher}` : `.${equalityMatcher}`), // replace the matcher argument with the right-hand side of the comparison
          fixer.replaceText(matcher.arguments[0], sourceCode.getText(comparison.right))];
        };

        context.report({
          messageId: 'useEqualityMatcher',
          suggest: ['toBe', 'toEqual', 'toStrictEqual'].map(equalityMatcher => ({
            messageId: 'suggestEqualityMatcher',
            data: {
              equalityMatcher
            },
            fix: buildFixer(equalityMatcher)
          })),
          node: (modifier || matcher).node.property
        });
      }

    };
  }

});

exports.default = _default;