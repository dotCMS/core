package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.Date;

import com.dotmarketing.util.Config;

public class Rating implements Serializable {

	private static final long serialVersionUID = -8677407173268659402L;
	private String userId;
	private String sessionId;
	private String identifier;
	private long id;
	private Date ratingDate = new Date();
	private float rating;
	private String longLiveCookiesId;
	private String userIP;
	private int ratingMaxValue;
	
	public Rating(){
		this.ratingMaxValue = Config.getIntProperty("RATING_MAX_VALUE");
	}
	
	public Rating(int ratingMaxValue){
		this.ratingMaxValue = ratingMaxValue;
	}
	
	public String getLongLiveCookiesId() {
		return longLiveCookiesId;
	}

	public void setLongLiveCookiesId(String longLiveCookiesId) {
		this.longLiveCookiesId = longLiveCookiesId;
	}

	public String getUserIP() {
		return userIP;
	}

	public void setUserIP(String userIP) {
		this.userIP = userIP;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public float getRating() {
		return rating;
	}

	public void setRating(float rating) {
		if(rating > ratingMaxValue ) rating = ratingMaxValue;
		this.rating = rating;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Date getRatingDate() {
		return ratingDate;
	}

	public void setRatingDate(Date ratingDate) {
		this.ratingDate = ratingDate;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getSessionId() {
		return sessionId;
	}
	public int getRatingMaxValue() {
		return ratingMaxValue;
	}

	public void setRatingMaxValue(int ratingMaxValue) {
		this.ratingMaxValue = ratingMaxValue;
	}

}