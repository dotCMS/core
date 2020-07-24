package com.dotcms.rendering.velocity.viewtools;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.util.Config;

public class ConfigTool implements ViewTool {


    @Override
    public void init(Object initData) {}

    public String getStringProperty(String name, String defValue) {
        return Config.getStringProperty(name, defValue);
    }

    public String getStringProperty(String name) {
        return this.getStringProperty(name, null);
    }

    public String[] getStringArrayProperty(String name) {
        return Config.getStringArrayProperty(name);
    }

    public int getIntProperty(String name) {
        return this.getIntProperty(name, 0);
    }

    public int getIntProperty(String name, int defValue) {
        return Config.getIntProperty(name, defValue);
    }

    public long getLongProperty(String name) {
        return this.getLongProperty(name, 0);
    }

    public long getLongProperty(String name, int defValue) {
        return Config.getLongProperty(name, defValue);
    }

    public float getFloatProperty(String name) {
        return this.getFloatProperty(name, 0f);
    }


    public float getFloatProperty(String name, float defaultVal) {
        return Config.getFloatProperty(name, defaultVal);
    }


    public boolean getBooleanProperty(String name) {
        return this.getBooleanProperty(name, false);
    }

    public boolean getBooleanProperty(String name, boolean defaultVal) {
        return Config.getBooleanProperty(name, defaultVal);
    }
}
