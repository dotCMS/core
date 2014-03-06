package com.tonicsystems.jarjar;

import java.util.Comparator;

/**
 * @author Jonathan Gamba
 *         Date: 12/11/13
 */
public class CustomRule extends Rule {

    String parent;

    public String getParent () {
        return parent;
    }

    public void setParent ( String parent ) {
        this.parent = parent;
    }

    public class RuleSortByParent implements Comparator<CustomRule> {

        String parent;

        public RuleSortByParent ( String parent ) {
            this.parent = parent;
        }

        String getParent () {
            return parent;
        }

        void setParent ( String parent ) {
            this.parent = parent;
        }

        @Override
        public int compare ( CustomRule rule1, CustomRule rule2 ) {

            if ( rule1.getParent() == null ) {
                return -1;
            }

            if ( rule1.getParent().equals( this.getParent() ) ) {
                return -1;
            } else if ( !rule1.getParent().equals( this.getParent() ) ) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}