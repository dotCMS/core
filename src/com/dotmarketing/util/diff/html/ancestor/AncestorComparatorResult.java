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
package com.dotmarketing.util.diff.html.ancestor;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.util.diff.html.modification.HtmlLayoutChange;

public class AncestorComparatorResult {
	
	

    private boolean changed = false;

    private String changes = null;
    
    private List<HtmlLayoutChange> htmlLayoutChanges= null;
    
    public AncestorComparatorResult()
	{
    	htmlLayoutChanges = new ArrayList<HtmlLayoutChange>();
	}

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

	/**
	 * @return the htmlChanges
	 */
	public List<HtmlLayoutChange> getHtmlLayoutChanges() {
		return htmlLayoutChanges;
	}

	/**
	 * @param htmlLayoutChanges the htmlChanges to set
	 */
	public void setHtmlLayoutChanges(List<HtmlLayoutChange> htmlLayoutChanges) {
		this.htmlLayoutChanges = htmlLayoutChanges;
	}
    
    
    
    

}
