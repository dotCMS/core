/*
 * SearchResult.java
 *
 * Created on August 21, 2002, 5:30 PM
 */

package com.dotmarketing.beans;

import com.dotmarketing.util.Config;



/**
 *
 * @author  rocco
 */
public class SearchResult extends Object implements java.io.Serializable {
    
    
    private static final long serialVersionUID = 1L;

	/** Holds value of property title. */
    private String title;
    
    /** Holds value of property url. */
    private String url;
    
    /** Holds value of property desc. */
    private String desc;
    
    /** Holds value of property score. */
    private String score;
    
    /** Creates new Page */
    public SearchResult() {
    }
    

    
    /** Getter for property title.
     * @return Value of property title.
     *
     */
    public String getTitle() {
        return this.title;
    }
    
    /** Setter for property title.
     * @param title New value of property title.
     *
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /** Getter for property url.
     * @return Value of property url.
     *
     */
    public String getUrl() {
        return this.url;
    }
    
    /** Setter for property url.
     * @param url New value of property url.
     *
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /** Getter for property desc.
     * @return Value of property desc.
     *
     */
    public String getDesc() {
        return this.desc;
    }
    
    /** Getter for property desc.
     * @return Value of property desc.
     *
     */
    public String getPrettyDesc() {
	if(this.desc.length() > 3){
		String prettyDesc = this.desc;
		prettyDesc = prettyDesc.replaceAll("&amp;nbsp;"," ");
		prettyDesc = prettyDesc.replaceAll("&nbsp;"," ");
		return prettyDesc;
	}
	return this.desc;
    }
    
    /** Setter for property desc.
     * @param desc New value of property desc.
     *
     */
    public void setDesc(String desc) {
        desc = desc.replaceAll("@import.*;", "");
        this.desc = desc;
    }
    
    /** Getter for property score.
     * @return Value of property score.
     *
     */
    public String getScore() {
        return this.score;
    }
    
    /** Getter for property score.
     * @return Value of property score.
     *
     */
    public int getPercentScore(){
    	int percentScore = 0;
        double fScore = 0;
        try{
            fScore = Float.parseFloat(this.score);
        }catch(Exception e){
            
        }  
        percentScore = (int) (fScore * 100.0f);
        
        return percentScore;
    }
    
    /** Setter for property score.
     * @param score New value of property score.
     *
     */
    public void setScore(String score) {
        this.score = score;
    }


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object arg0) {

		String url1 = ((SearchResult)arg0).getUrl();
		String url2 = this.getUrl();
		
		String indexPage = "index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
		
		if (url1 !=null) url1 = url1.replaceAll(indexPage,"");
		if (url2 !=null) url2 = url2.replaceAll(indexPage,"");
		
		boolean ret = false;
		if (url1!=null) {
			ret = url1.equals(url2);
		}
		
		return ret;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		try {
			return Integer.parseInt(score);
		}
		catch (Exception e) {
			
		}
		return 0;
	}
}
