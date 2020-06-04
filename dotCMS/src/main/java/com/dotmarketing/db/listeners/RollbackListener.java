package com.dotmarketing.db.listeners;

public class RollbackListener implements DotListener {
    private final String key;
    private final Runnable listener;
    public RollbackListener(DotListener listener) {
        this.key = listener.key();
        this.listener = listener;
        
    }
    public RollbackListener(String key,Runnable listener) {
        this.key = key;
        this.listener = listener;
        
    }    
    
    @Override
    public void run() {
        listener.run();
        
    }
    @Override
    public String key() {
        return this.getClass().getName() + ":" + key;
    }

    @Override
    public final boolean equals(Object obj) {
        
        
        if(obj == null ||!( obj instanceof RollbackListener)) {
            return false;
        }
        RollbackListener other = (RollbackListener)obj;
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
