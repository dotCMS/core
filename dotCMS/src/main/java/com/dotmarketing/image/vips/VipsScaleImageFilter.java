package com.dotmarketing.image.vips;

/**
 * Backwards-compatible alias for the {@code scale_w}/{@code scale_h} filter, mirroring
 * {@link com.dotmarketing.image.filter.ScaleImageFilter}. Inherits all behaviour from
 * {@link VipsResizeImageFilter}; only the parameter prefix differs ({@code scale_}).
 */
public class VipsScaleImageFilter extends VipsResizeImageFilter {
}
