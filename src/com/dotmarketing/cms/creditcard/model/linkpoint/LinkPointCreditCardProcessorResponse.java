package com.dotmarketing.cms.creditcard.model.linkpoint;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.cms.creditcard.model.CreditCardProcessorResponse;

public class LinkPointCreditCardProcessorResponse extends CreditCardProcessorResponse{
	private String avs;

	private String ordernum;

	private String error;

	private String approved;
	
	private String time;

	private String ref;

	private String tdate;

	private String score;

	public boolean orderApproved() {
		return approved.trim().toUpperCase().equals("APPROVED");
	}

	public LinkPointCreditCardProcessorResponse(String approved, String avs,
			String code, String error, String message, String ordernum,
			String ref, String score, String tdate, String time) {
		super();
		this.approved = approved;
		this.avs = avs;		
		this.error = error;
		
		this.ordernum = ordernum;
		this.ref = ref;
		this.score = score;
		this.tdate = tdate;
		this.time = time;
	}

	/**
	 * Returns the approval status string
	 * 
	 * @return Posible values APPROVED, DECLINED, or FRAUD
	 */
	public String getApproved() {
		return approved;
	}

	/*
	 * The Address Verification System (AVS) response for this transaction. The
	 * first character indicates whether the contents of the addrnum tag match
	 * the address number on file for the billing address. The second character
	 * indicates whether the billing zip code matches the billing records. The
	 * third character is the raw AVS response from the card-issuing bank. The
	 * last character indicates whether the cvmvalue was correct and may be �M�
	 * for Match, N for No Match, or Z if the match could not determined.
	 */
	public String getAvs() {
		return avs;
	}

	/**
	 * 
	 * @return Any error message associated with this transaction.
	 */
	public String getError() {
		return error;
	}

	/**
	 * 
	 * @return The order number associated with this transaction.
	 */
	public String getOrdernum() {
		return ordernum;
	}

	/**
	 * 
	 * @return The reference number returned by the credit card processor.
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * 
	 * @return A server time-date stamp for this transaction. Used to
	 *  uniquely identify a specific transaction where one order
	 *  number may apply to several individual transactions. See
	 *  the Transaction Details Data Fields section for further
	 *  information and an example of tdate.
	 */
	public String getTdate() {
		return tdate;
	}

	/**
	 * 
	 * @return The time and date of the transaction server response.
	 */
	public String getTime() {
		return time;
	}

	/**
	 * 
	 * @return If LinkShieldTM is enabled on the account, the LinkShield
	 *  fraud risk score will be returned in this field. A value of 0
	 *  indicates a low risk of fraud; a value of 99 indicates a high
	 *  risk of fraud.
	 */
	public String getScore() {
		return score;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	
}
