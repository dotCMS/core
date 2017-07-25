package com.dotcms.api.system.event.local.type;


public class PushPublishEvent {
     private String name = null;

     public String getName(){
         return name;
     }

     public void setName(String name){
         this.name = name;
     }
}
