<%@page import="com.liferay.portal.language.LanguageUtil"%>
<table style="border:0;">
	<tr>
		<td style="width:70px;white-space:nowrap;border:0;vertical-align:top;">
			<%= LanguageUtil.get(pageContext, "Tag") %>
			<a href="#" id="tip1"><span class="infoIcon"></span></a>
		</td>
		<td style="width:260px;border:0;">
			<textarea dojoType="dijit.form.Textarea" name="tagName" id="tagName" style="min-height:100px; width:250px;" onkeyup="suggestTagsForSearch(this, 'suggestedTagsDiv');"></textarea><br/>
			<span class="inputCaption"><%= LanguageUtil.get(pageContext, "Suggested-Tags") %>:</span>
			<div id="suggestedTagsDiv" style="border:1px dotted #ccc;height:20px;"></div>
			
			<input type="hidden" name="cmd" id="cmd" value="" />
			<div class="buttonRow">
				<div class="clear"></div>
				<button dojoType="dijit.form.Button" onClick="javascript:assignTag();" iconClass="plusIcon"><%= LanguageUtil.get(pageContext, "update") %></button>
				<div class="clear"></div>
			</div>
		</td>
		<td style="border:0;vertical-align:top;">
			<table  id="tags_table" class="listingTable" style="width:400px;margin:0 auto;font-size:100%;">
			</table>
		</td>
	</tr>
</table>
		




<div style="position: absolute; top:0px;left:0px; display: none;" id="tag_div"></div>

<span dojoType="dijit.Tooltip" connectId="tip1" id="one_tooltip">
	<p><%= LanguageUtil.get(pageContext, "Tags-are-descriptors-that-you-can-assign-to-users-Tags-are-a-little-bit-like-keywords") %></p>
	<p><b><%= LanguageUtil.get(pageContext, "Tip") %>:</b> <%= LanguageUtil.get(pageContext, "Type-your-tag-You-can-enter-multiple-comma-separated-tags") %></p>
</span>
