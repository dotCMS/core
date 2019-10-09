package com.dotmarketing.portlets.hostvariable.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into {@link HostVariable}
 * instances
 */
public class HostVariableTransformer implements DBTransformer {
    final List<HostVariable> list;


    public HostVariableTransformer(List<Map<String, Object>> initList){
        List<HostVariable> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<HostVariable> asList() {
        return this.list;
    }

    @NotNull
    private static HostVariable transform(Map<String, Object> map)  {
        final HostVariable variable;
        variable = new HostVariable();
        variable.setId((String) map.get("id"));
        variable.setHostId((String) map.get("host_id"));
        variable.setName((String) map.get("variable_name"));
        variable.setKey((String) map.get("variable_key"));
        variable.setValue((String) map.get("variable_value"));
        variable.setLastModifierId((String) map.get("user_id"));
        variable.setLastModDate((Date) map.get("last_mod_date"));
        return variable;
    }
}


