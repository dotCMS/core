package com.dotmarketing.db.listeners;

public abstract class FlushCacheListener implements DotListener {
    
    @Override
    public final boolean equals(Object obj) {
        
        if(obj == null ||!( obj instanceof FlushCacheListener)) {
            return false;
        }
        FlushCacheListener other = (FlushCacheListener)obj;
        if(this.key() ==null ) {
            return false;
        }
        return this.key().toLowerCase().equalsIgnoreCase(other.key());

    }
    
    @Override
    public final String toString() {
        
        return this.getClass().getName() + ":" + this.key();
        
    }
    
    

}
