package com.dotmarketing.db.listeners;

public abstract class CommitListener implements DotListener {

    @Override
    public final boolean equals(Object obj) {
        
        
        if(obj == null ||!( obj instanceof CommitListener)) {
            return false;
        }
        CommitListener other = (CommitListener)obj;
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
