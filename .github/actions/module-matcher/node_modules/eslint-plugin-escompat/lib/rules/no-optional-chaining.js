module.exports = (context, badBrowser) => ({
  OptionalMemberExpression(node) {
    context.report(node, `Optional Chaining is not supported in ${badBrowser}`)
  }
})
