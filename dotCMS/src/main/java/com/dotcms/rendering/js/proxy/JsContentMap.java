package com.dotcms.rendering.js.proxy;

import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.graalvm.polyglot.HostAccess;

import java.io.IOException;
import java.io.Serializable;

/**
 * This class is used to expose the ContentMap object to the javascript engine.
 *
 * @author jsanca
 */
@JsonSerialize(using = JsContentMapSerializer.class)
public class JsContentMap implements Serializable, JsProxyObject<ContentMap> {

    private final ContentMap contentMap;

    public JsContentMap(final ContentMap contentMap) {
        this.contentMap = contentMap;
    }

    @HostAccess.Export
    /**
     * Use to get a value of the field on a content returned from the ContentTool Viewtool
     * This method gets called automatically when you place a "." after the contentmap object in Velocity<br/>
     * EXAMPLE : $mycontent.headline will call this method and return the value for the headline field of a piece of content.<br/>
     * NOTE: This is the last thing that gets called meaning if you do $mycontent.urlMap it will call the actual getUrlMap because that
     * method exists. This is case sensitive and uses standard Java bean reflection. For those not familiar here take note that the way to
     * call the getUrlMap is $mycontent.urlMap the get is removed and the next letter us lowered.<br/>
     *
     * Notes and Examples on Field Types <br/>
     * CATEGORY FIELDS : The category field is a heavier pull.  It is retrieved lazily meaning not until you say $mycon.mycatfield will it get retrieved.
     * It is not a bad performance but certainly slower then not displaying the category fields. Searching for categories doesn't effect the speed at all
     * it is only displaying them that will. The value returned to Velocity are the actual Category Objects. You get an ArrayList of them<br/>
     * <br/>
     * FILE/IMAGE FIELDS: You can get File/Image fields as well. $con.myimage or $con.myfile. It will return a FileAssetMap object which wraps the actual File object from dotCMS.It adds the uri as a variable.
     * All the objects have toString implemented on them which means you can spit it out in velocity and see what it available to you.<br/>
     * <br/>
     * BINARY FIELDS : You can also get at binary field types. $mycon.myBinaryField This return the BinaryMap object to you.<br/>
     * TAG FIELDS : You get a TagList which is an arrayList that lets you get at the raw tag value. Meaning a comma separated list of values. <br />
     * HOST FIELDS OR HOST : Will return a ContentMap of the host or for the Folder the actual Folder <br/>
     * MULTI SELECT FIELDS : Returns MultiSelectMap which provides you Lists for the Options Values and Labels as well as a List of the Selected Values for this Content<br/>
     * SELECT FIELDS : Returns SelectMap which provides you Lists for the Options Values and Labels as well as the Selected Value for this Content<br/>
     * RADIO FIELDS : Returns RadioMap which provides you Lists for the Options Values and Labels as well as the Selected Value for this Content<br/>
     * @param fieldVariableName The velocity Variable name from the structure.
     * @return
     */
    public Object get(final String fieldVariableName) {
        return JsProxyFactory.createProxy(this.contentMap.get(fieldVariableName));
    }

    @HostAccess.Export
    /**
     * Use to get an unparsed value of the field on a content returned from the ContentTool Viewtool, even if it contains velocity code
     * @param fieldVariableName The velocity Variable name from the structure.
     * @return
     */
    public Object getRaw(final String fieldVariableName) {
        return JsProxyFactory.createProxy(this.contentMap.getRaw(fieldVariableName));
    }

    @HostAccess.Export
    /**
     * Recovery the field variables for a content type field (if the field already exists, otherwise returns an empty collection)
     * @param fieldVariableName String field var name
     * @return Map
     */
    public Object getFieldVariables(final String fieldVariableName) {

        return JsProxyFactory.createProxy(this.contentMap.getFieldVariables(fieldVariableName));
    }

    @HostAccess.Export
    /**
     * Recovery the field variables as a json object
     * @param fieldVariableName String field var name
     * @return Map
     */
    public Object getFieldVariablesJson(final String fieldVariableName) {

        return JsProxyFactory.createProxy(this.contentMap.getFieldVariablesJson(fieldVariableName));
    }

    @HostAccess.Export
    /**
     * Returns the returns the identifier based URI for the
     * first doc/file on a piece of content
     * EXAMPLE : $mycontent.shorty
     * @return
     * @throws IOException
     */
    public String getShortyUrl() throws IOException {
        return this.contentMap.getShortyUrl();
    }

    @HostAccess.Export
    /**
     * Returns the valid short version of the
     * identifier
     * @return
     * @throws IOException
     */
    public String getShorty() throws IOException {
        return this.contentMap.getShorty();
    }

    @HostAccess.Export
    /**
     * Returns the valid short version of the
     * inode
     * @return
     * @throws IOException
     */
    public String getShortyInode() throws IOException {
        return this.contentMap.getShortyInode();
    }

    @HostAccess.Export
    /**
     * Returns the returns the identifier based URI for the
     * first doc/file on a piece of content
     * EXAMPLE : $mycontent.shortyInode
     * @return
     * @throws IOException
     */
    public String getShortyUrlInode() throws IOException {
        return this.contentMap.getShortyUrlInode();
    }

    @HostAccess.Export
    public Object getContentType() {
        return JsProxyFactory.createProxy(new StructureTransformer(this.contentMap.getStructure()).from());
    }

    @HostAccess.Export
    public String getContentletsTitle() {
        return contentMap.getContentletsTitle();
    }

    @HostAccess.Export
    public boolean isLive() throws Exception {
        return contentMap.isLive();
    }

    @HostAccess.Export
    public boolean isWorking() throws Exception {
        return contentMap.isWorking();
    }

    @HostAccess.Export
    public String toString() {
        return contentMap.toString();
    }

    @HostAccess.Export
    public boolean isHTMLPage() {
        return contentMap.isHTMLPage();
    }

    /**
     * This is not accessible on js context.
     * @return ContentMap
     */
    public ContentMap getContentMapObject() {
        return this.contentMap;
    }

    @Override
    public ContentMap  getWrappedObject() {
        return this.getContentMapObject();
    }
}
