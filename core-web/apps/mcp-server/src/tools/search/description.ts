export const searchDescription = `
Searches content using Lucene syntax. Use this tool to query dotCMS content using Lucene queries. Only indexed fields can be searched. See the documentation for Lucene syntax instructions.

Only fields that are indexed can be searched. System fields like "title" or "modDate" are always indexed. For custom fields, the field must have the System Indexed option checked in your Content Type definition. To search a custom field, you must prefix it with the content type variable name. For example, if you have a "Products" content type and a "productType" field, you would write:

"+products.productType:etf"

For system fields, you do not need the content type prefix. For example:

"+title:bond"

instead of

"+products.title:bond".

A basic search term is written as "field:value". To make sure a term must be present, add "+" in front of it. To exclude a term, add "-". For example:

"+contentType:Blog +Blog.body:(+"investment" "JetBlue")"

This means: must be a Blog, body must contain investment, body may contain JetBlue.

Operators include plus for required, minus for prohibited, AND written as "&&", OR written as "||", and NOT written as "!".

Example using AND:

"+Blog.body:("business" && "Apple")"

This finds blog posts with both the word business and Apple in the body.

Example using OR:

"+Blog.body:("analyst" || "investment")"

This finds blog posts with either analyst or investment in the body.

Example using NOT:

"+Blog.body:("investment" !"JetBlue")"

This finds posts that have investment but do not have JetBlue.

Wildcards are supported. Use "*" for multiple characters and "?" for a single character.

Example multiple character wildcard:

"+Employee.firstName:R*"

This finds employees whose first name starts with R.

Example single character wildcard:

"+Employee.firstName:Mari?"

This finds employees whose first name is Maria or Marie.

Fuzzy search is supported using a tilde "~".

Example:

"+title:dotCMS~"

This finds matches that are close to dotCMS, like dotcms or dotCMSs.

Use square brackets with TO for a value range. Use the strict date format "yyyyMMddHHmmss" for dates.

Example number range:

"+News.title_dotraw:[A TO C]*"

This finds News items with a title starting A through C.

Example date range:

"+News.sysPublishDate:[20240101000000 TO 20241231235959]"

This finds News items published in 2024.

Use "catchall" to search all fields.

Example:

"+catchall:*seo*"

This searches for seo anywhere in the content.

Use a caret "^" to boost some terms so they rank higher.

Example:

"+News.title:("American"^10 "invest"^2)"

This gives more weight to results with American than invest.

Lucene treats a space between terms as OR by default. Always use explicit "||" or "&&" for clarity if needed.

Example:

"+Blog.body:("finance" "investment")"

This means finance OR investment.

Better:

"+Blog.body:("finance" && "investment")"

if you want both.

When you want to match multiple values for the same field, wrap them in parentheses.

Example:

"+field:(term1 || term2)"

or

"+(field:term1 field:term2)"

When you need an exact phrase, use double quotes.

Example:

"+"Blog.body":"final exam""

This matches final exam as a whole phrase in that order.

Lucene does not directly check for null or empty values. If you need to find records with any value in a field, you can use a range hack. For example:

"+typeVar.binaryVal:([0 TO 9] [a TO z] [A TO Z])"

Always escape special characters so they are not treated as operators. Use a backslash. For example.

Lucene syntax does not let you search relationships directly. If you need to work with related content, use "$dotcontent.pullRelated()" in Velocity or use the Content API "depth" parameter to pull related contentlets.

Example queries you can use for reference:

Find blog posts about investment but not JetBlue:

"+contentType:Blog +Blog.body:("investment" !"JetBlue")"

Find news with title starting A through C:

"+contentType:News +News.title_dotraw:[A TO C]*"

Find employees whose first name starts with R but last name does not start with A:

"+contentType:Employee +(Employee.firstName:R*) -(Employee.lastName:A*)"

Do a fuzzy search for dotCMS in the title:

"+title:dotCMS~"

Find news published in 2024:

"+News.sysPublishDate:[20240101000000 TO 20241231235959]"

When writing dotCMS Lucene queries, always use the correct content type and field format, explicit operators, escape special characters, stick to the strict date format, and do not generate raw OpenSearch JSON unless you really need advanced features.
`;
