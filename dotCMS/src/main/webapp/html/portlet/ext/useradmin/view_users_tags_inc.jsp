<%@page import="com.liferay.portal.language.LanguageUtil"%>

<div class="view-users__section-content-cols">
	<div class="form-horizontal view-users__marketing-tag-form">
		<dl>
			<dt>
				<label for="tagName">
					<%= LanguageUtil.get(pageContext, "Tag") %>:
					<a href="#" id="tip1"><span class="infoIcon"></span></a>
				</label>
			</dt>
			<dd>
				<textarea dojoType="dijit.form.Textarea" name="tagName" id="tagName" style="min-height:100px; width:250px;" onkeyup="suggestTagsForSearch(this, 'suggestedTagsDiv');"></textarea><br />
			</dd>
		</dl>
		<dl>
			<dt>
				<span class="inputCaption"><%= LanguageUtil.get(pageContext, "Suggested-Tags") %>:</span>
			</dt>
			<dd>
				<div id="suggestedTagsDiv" style="border:1px dotted #ccc;width:250px;height:100px;white-space:pre-wrap;overflow:auto;"></div>
				<input type="hidden" name="cmd" id="cmd" value="" />
			</dd>
		</dl>
	</div>
	<div class="view-users__marketing-tag-content">
		<table id="tags_table" class="listingTable" style="width:400px;margin:0 auto;font-size:100%;"></table>
		<div style="position: absolute; top:0px;left:0px; display: none;" id="tag_div"></div>
	</div>
</div>

<div class="buttonRow">
	<button dojoType="dijit.form.Button" onClick="javascript:assignTag();"><%= LanguageUtil.get(pageContext, "update") %></button>
</div>

<span dojoType="dijit.Tooltip" connectId="tip1" id="one_tooltip">
	<p><%= LanguageUtil.get(pageContext, "Tags-are-descriptors-that-you-can-assign-to-users-Tags-are-a-little-bit-like-keywords") %></p>
	<p><b><%= LanguageUtil.get(pageContext, "Tip") %>:</b> <%= LanguageUtil.get(pageContext, "Type-your-tag-You-can-enter-multiple-comma-separated-tags") %></p>
</span>
