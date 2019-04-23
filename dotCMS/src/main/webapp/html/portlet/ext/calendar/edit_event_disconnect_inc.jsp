
<%
    final boolean disconnectedEvent = eventForm.isDisconnectEvent();
    if(!contentlet.isNew() && !UtilMethods.isSet(contentlet.getStringProperty("disconnectedFrom")) ){
%>
<div style="margin-top: 30px;"></div>
<div  id="propagateChange" class="form-horizontal">
    <dl>
         <dt>
           <label for="disconnectEvent"><%= LanguageUtil.get(pageContext, "update-all-events-in-the-series") %></label>
         </dt>
         <dd>
             <div class="radio">
                 <input dojoType="dijit.form.RadioButton" type="radio" onclick="" name="disconnectEvent" id="doNotDisconnectEvent" value="false" <%=!disconnectedEvent ? "checked" : "" %> />
                 <label for="doNotDisconnectEvent"><%= LanguageUtil.get(pageContext, "All-events-in-the-series") %></label>
             </div>
             <div class="radio">
                 <input dojoType="dijit.form.RadioButton" type="radio" onclick="" name="disconnectEvent" id="disconnectEvent" value="true" <%=disconnectedEvent ? "checked" : "" %> />
                 <label for="disconnectEvent"><%= LanguageUtil.get(pageContext, "Only-this-instance") %></label>
             </div>
         </dd>

    </dl>
</div>
<%
   }
%>