package com.dotcms.contenttype.model.type;

/** One of nine abstract subclasses in the real tree. Sealing does not stop at ContentType. */
public sealed abstract class SimpleContentType extends ContentType permits ImmutableSimpleContentType {
}
