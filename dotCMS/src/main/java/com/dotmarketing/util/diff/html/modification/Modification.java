/*
 * Copyright 2007 Guy Van den Broeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dotmarketing.util.diff.html.modification;

import java.util.List;

public class Modification implements Cloneable {

    private ModificationType type;

    private long id = -1;

    private Modification prevMod = null;

    private Modification nextMod = null;

    private boolean firstOfID = false;
    
    private List<HtmlLayoutChange> htmlLayoutChanges = null;

    public Modification(ModificationType type) {
        this.type = type;
    }

    @Override
    public Modification clone() {
        Modification newM = new Modification(this.getType());
        newM.setID(getID());
        newM.setChanges(getChanges());
        newM.setHtmlLayoutChanges(getHtmlLayoutChanges());
        newM.setFirstOfID(isFirstOfID());
        newM.setNext(getNext());
        newM.setPrevious(getPrevious());
        return newM;
    }

    public ModificationType getType() {
        return type;
    }

    public void setID(long id) {
        this.id = id;
    }

    public long getID() {
        return id;
    }

    public void setPrevious(Modification m) {
        this.prevMod = m;
    }

    public Modification getPrevious() {
        return prevMod;
    }

    public void setNext(Modification m) {
        this.nextMod = m;
    }

    public Modification getNext() {
        return nextMod;
    }

    private String changes;

    public void setChanges(final String changes) {
        this.changes = changes;
    }

    public String getChanges() {
        return changes;
    }

    public boolean isFirstOfID() {
        return firstOfID;
    }

    public void setFirstOfID(boolean firstOfID) {
        this.firstOfID = firstOfID;
    }

	/**
	 * @return the htmlLayoutChanges
	 */
	public List<HtmlLayoutChange> getHtmlLayoutChanges() {
		return htmlLayoutChanges;
	}

	/**
	 * @param htmlLayoutChanges the htmlLayoutChanges to set
	 */
	public void setHtmlLayoutChanges(List<HtmlLayoutChange> htmlLayoutChanges) {
		this.htmlLayoutChanges = htmlLayoutChanges;
	}

	
    
    

}
