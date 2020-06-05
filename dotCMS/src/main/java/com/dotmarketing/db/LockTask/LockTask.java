package com.dotmarketing.db.LockTask;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LockTask implements Serializable {

    final public String task;
    final public String serverId;
    final public Date lockedUntil;
    final public Date currentTime;
    
    public LockTask(final Map<String,Object> dbMap) {
        task=(String) dbMap.get("task_id");
        serverId=(String) dbMap.get("server_id");
        lockedUntil = (Date) dbMap.get("locked_until");     
        currentTime = (Date) dbMap.get("nowsers");     
    }
    
    
    public LockTask(final List<Map<String,Object>> dbList) {
        this(dbList.get(0));
    }


    @Override
    public String toString() {
        return "LockTask:{task=" + task + ", serverId=" + serverId + ", lockedUntil=" + lockedUntil + "}";
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((lockedUntil == null) ? 0 : lockedUntil.hashCode());
        result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
        result = prime * result + ((task == null) ? 0 : task.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LockTask other = (LockTask) obj;
        if (lockedUntil == null) {
            if (other.lockedUntil != null)
                return false;
        } else if (!lockedUntil.equals(other.lockedUntil))
            return false;
        if (serverId == null) {
            if (other.serverId != null)
                return false;
        } else if (!serverId.equals(other.serverId))
            return false;
        if (task == null) {
            if (other.task != null)
                return false;
        } else if (!task.equals(other.task))
            return false;
        return true;
    }
    
    
}
