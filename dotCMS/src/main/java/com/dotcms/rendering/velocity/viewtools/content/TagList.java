/**
 * 
 */
package com.dotcms.rendering.velocity.viewtools.content;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.dotmarketing.tag.model.Tag;

/**
 * Used as a wrapper around an ArrayList of Tags to return to the front-end of dotCMS from the 
 * ContentTool. 
 * 
 * @author Jason Tesser
 * @since 1.9.1.3
 *
 */
public class TagList extends ArrayList<String> {

  final List<String> tags;
	protected TagList(List<Tag> tagValue) {
		tags=tagValue.stream().map(t->t.getTagName()).collect(Collectors.toList());
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -2259803707187904180L;


	/**
	 * @return the rawTagValues
	 */
	public String getRawTagValues() {
		return String.join(",",tags);
	}
	
	public List<String> getTags() {
	  return tags;
	}
}
