package com.dotcms.publisher.myTest;

import java.util.Date;
import java.util.Map;


class PushAssetWrapper {
    private Date copyDate; 
    private Map<String,Object> contentMap;
    
    public Date getCopyDate() {
        return copyDate;
    }
    public void setCopyDate(Date copyDate) {
        this.copyDate = copyDate;
    }
    public Map<String,Object> getContentMap() {
        return contentMap;
    }
    public void setContentMap(Map<String,Object> contentMap) {
        this.contentMap = contentMap;
    }
}

//public class XstreamBinaryTest {
//    
//    public static void main(String[] args) {
//        Map<String,Object> contentMap=new HashMap<String, Object>();
//        contentMap.put("inode", "213213");
//        contentMap.put("identifier", "745715");
//        
//        // you can loop over the contentlet fields and ask if its binary
//        // and then get the bytes for it and put it as a byte[] in the content map
//        contentMap.put("somebinaryfield",
//          "this is a binary file. It might be on disk so we need to copy into a byte array".getBytes());
//        
//        YourWrapper wrapper=new YourWrapper();
//        wrapper.setCopyDate(new Date());
//        wrapper.setContentMap(contentMap);
//        
//        XStream xstream=new XStream(new DomDriver());
//        xstream.registerConverter(new EncodedByteArrayConverter());
//        
//        // note that it also support passing an outputstream. That would be faster
//        String xml=xstream.toXML(wrapper);
//        
//        System.out.println("this is the XML");
//        System.out.println(xml);
//        
//        // and we can read the xml back
//        YourWrapper readed=(YourWrapper)xstream.fromXML(xml);
//        byte[] bytes=(byte[])readed.getContentMap().get("somebinaryfield");
//        System.out.println("this is the text recovered from the encoded bytes: "+new String(bytes));
//        
//    }
//    
//    
//}
