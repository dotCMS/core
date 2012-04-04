package com.dotmarketing.cms.creditcard.model.linkpoint;

import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import lp.txn.JLinkPointTransaction;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.dotmarketing.cms.creditcard.model.CreditCardProcessor;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * This class specifies the methods to send Credit Card Transactions to the link point gateway.<br>
 * <br>
 * For testing purposes use the following credit card numbers:<br>
 * American Express: 371111111111111<br>
 * Discover: 6011-1111-1111-1111<br>
 * JCB: 311111111111111<br>
 * MasterCard: 5111-1111-1111-1111<br>
 * MasterCard: 5419-8400-0000-0003<br>
 * Visa: 4111-1111-1111-1111<br>
 *
 * Some methods requires specific codes like the country and US states codes.<br>
 *
 * <b>US States Codes:</b><br>
 * &lt;select NAME="bstate" SIZE="1"><br>
 *  &lt;option value=""> ... &lt;/option><br>
 *  &lt;option value="AK"> AK &lt;/option><br>
 *  &lt;option value="AL"> AL &lt;/option><br>
 *  &lt;option value="AR"> AR &lt;/option><br>
 *  &lt;option value="AZ"> AZ &lt;/option><br>
 *  &lt;option value="CA"> CA &lt;/option><br>
 *  &lt;option value="CO"> CO &lt;/option><br>
 *  &lt;option value="CT"> CT &lt;/option><br>
 *  &lt;option value="DC"> DC &lt;/option><br>
 *  &lt;option value="DE"> DE &lt;/option><br>
 *  &lt;option value="FL"> FL &lt;/option><br>
 *  &lt;option value="GA"> GA &lt;/option><br>
 *  &lt;option value="HI"> HI &lt;/option><br>
 *  &lt;option value="IA"> IA &lt;/option><br>
 *  &lt;option value="ID"> ID &lt;/option><br>
 *  &lt;option value="IL"> IL &lt;/option><br>
 *  &lt;option value="IN"> IN &lt;/option><br>
 *  &lt;option value="KS"> KS &lt;/option><br>
 *  &lt;option value="KY"> KY &lt;/option><br>
 *  &lt;option value="LA"> LA &lt;/option><br>
 *  &lt;option value="MA"> MA &lt;/option><br>
 *  &lt;option value="MD"> MD &lt;/option><br>
 *  &lt;option value="ME"> ME &lt;/option><br>
 *  &lt;option value="MI"> MI &lt;/option><br>
 *  &lt;option value="MN"> MN &lt;/option><br>
 *  &lt;option value="MO"> MO &lt;/option><br>
 *  &lt;option value="MS"> MS &lt;/option><br>
 *  &lt;option value="MT"> MT &lt;/option><br>
 *  &lt;option value="NC"> NC &lt;/option><br>
 *  &lt;option value="ND"> ND &lt;/option><br>
 *  &lt;option value="NE"> NE &lt;/option><br>
 *  &lt;option value="NH"> NH &lt;/option><br>
 *  &lt;option value="NJ"> NJ &lt;/option><br>
 *  &lt;option value="NM"> NM &lt;/option><br>
 *  &lt;option value="NV"> NV &lt;/option><br>
 *  &lt;option value="NY"> NY &lt;/option><br>
 *  &lt;option value="OH"> OH &lt;/option><br>
 *  &lt;option value="OK"> OK &lt;/option><br>
 *  &lt;option value="OR"> OR &lt;/option><br>
 *  &lt;option value="PA"> PA &lt;/option><br>
 *  &lt;option value="PR"> PR &lt;/option><br>
 *  &lt;option value="RI"> RI &lt;/option><br>
 *  &lt;option value="SC"> SC &lt;/option><br>
 *  &lt;option value="SD"> SD &lt;/option><br>
 *  &lt;option value="TN"> TN &lt;/option><br>
 *  &lt;option value="TX"> TX &lt;/option><br>
 *  &lt;option value="UT"> UT &lt;/option><br>
 *  &lt;option value="VA"> VA &lt;/option><br>
 *  &lt;option value="VI"> VI &lt;/option><br>
 *  &lt;option value="VT"> VT &lt;/option><br>
 *  &lt;option value="WA"> WA &lt;/option><br>
 *  &lt;option value="WI"> WI &lt;/option><br>
 *  &lt;option value="WV"> WV &lt;/option><br>
 *  &lt;option value="WY"> WY &lt;/option><br>
 * </select><br>
 * <br>
 * <b>Country Codes:</b><br>
 * <br>
 * &lt;select name="bcountry" size="1"><br>
 *  &lt;option value="US" selected="1">United States&lt;/option><br>
 *  &lt;option value="AF">Afghanistan&lt;/option><br>
 *  &lt;option value="AL">Albania&lt;/option><br>
 *  &lt;option value="DZ">Algeria&lt;/option><br>
 *  &lt;option value="AS">American Samoa&lt;/option><br>
 *  &lt;option value="AD">Andorra&lt;/option><br>
 *  &lt;option value="AO">Angola&lt;/option><br>
 *  &lt;option value="AI">Anguilla&lt;/option><br>
 *  &lt;option value="AQ">Antarctica&lt;/option><br>
 *  &lt;option value="AG">Antigua And Barbuda&lt;/option><br>
 *  &lt;option value="AR">Argentina&lt;/option><br>
 *  &lt;option value="AM">Armenia&lt;/option><br>
 *  &lt;option value="AW">Aruba&lt;/option><br>
 *  &lt;option value="AU">Australia&lt;/option><br>
 *  &lt;option value="AT">Austria&lt;/option><br>
 *  &lt;option value="AZ">Azerbaijan&lt;/option><br>
 *  &lt;option value="BS">Bahamas&lt;/option><br>
 *  &lt;option value="BH">Bahrain&lt;/option><br>
 *  &lt;option value="BD">Bangladesh&lt;/option><br>
 *  &lt;option value="BB">Barbados&lt;/option><br>
 *  &lt;option value="BY">Belarus&lt;/option><br>
 *  &lt;option value="BE">Belgium&lt;/option><br>
 *  &lt;option value="BZ">Belize&lt;/option><br>
 *  &lt;option value="BJ">Benin&lt;/option><br>
 *  &lt;option value="BM">Bermuda&lt;/option><br>
 *  &lt;option value="BT">Bhutan&lt;/option><br>
 *  &lt;option value="BO">Bolivia&lt;/option><br>
 *  &lt;option value="BA">Bosnia And Herzegowina&lt;/option><br>
 *  &lt;option value="BW">Botswana&lt;/option><br>
 *  &lt;option value="BV">Bouvet Island&lt;/option><br>
 *  &lt;option value="BR">Brazil&lt;/option><br>
 *  &lt;option value="IO">British Indian Ocean Territory&lt;/option><br>
 *  &lt;option value="BN">Brunei Darussalam&lt;/option><br>
 *  &lt;option value="BG">Bulgaria&lt;/option><br>
 *  &lt;option value="BF">Burkina Faso&lt;/option><br>
 *  &lt;option value="BI">Burundi&lt;/option><br>
 *  &lt;option value="KH">Cambodia&lt;/option><br>
 *  &lt;option value="CM">Cameroon&lt;/option><br>
 *  &lt;option value="CA">Canada&lt;/option><br>
 *  &lt;option value="CV">Cape Verde&lt;/option><br>
 *  &lt;option value="KY">Cayman Islands&lt;/option><br>
 *  &lt;option value="CF">Central African Republic&lt;/option><br>
 *  &lt;option value="TD">Chad&lt;/option><br>
 *  &lt;option value="CL">Chile&lt;/option><br>
 *  &lt;option value="CN">China&lt;/option><br>
 *  &lt;option value="CX">Christmas Island&lt;/option><br>
 *  &lt;option value="CC">Cocos (Keeling) Islands&lt;/option><br>
 *  &lt;option value="CO">Colombia&lt;/option><br>
 *  &lt;option value="KM">Comoros&lt;/option><br>
 *  &lt;option value="CG">Congo&lt;/option><br>
 *  &lt;option value="CK">Cook Islands&lt;/option><br>
 *  &lt;option value="CR">Costa Rica&lt;/option><br>
 *  &lt;option value="CI">Cote D'Ivoire&lt;/option><br>
 *  &lt;option value="HR">Croatia&lt;/option><br>
 *  &lt;option value="CU">Cuba&lt;/option><br>
 *  &lt;option value="CY">Cyprus&lt;/option><br>
 *  &lt;option value="CZ">Czech Republic&lt;/option><br>
 *  &lt;option value="DK">Denmark&lt;/option><br>
 *  &lt;option value="DJ">Djibouti&lt;/option><br>
 *  &lt;option value="DM">Dominica&lt;/option><br>
 *  &lt;option value="DO">Dominican Republic&lt;/option><br>
 *  &lt;option value="TP">East Timor&lt;/option><br>
 *  &lt;option value="EC">Ecuador&lt;/option><br>
 *  &lt;option value="EG">Egypt&lt;/option><br>
 *  &lt;option value="SV">El Salvador&lt;/option><br>
 *  &lt;option value="GQ">Equatorial Guinea&lt;/option><br>
 *  &lt;option value="ER">Eritrea&lt;/option><br>
 *  &lt;option value="EE">Estonia&lt;/option><br>
 *  &lt;option value="ET">Ethiopia&lt;/option><br>
 *  &lt;option value="FK">Falkland Islands&lt;/option><br>
 *  &lt;option value="FO">Faroe Islands&lt;/option><br>
 *  &lt;option value="FJ">Fiji&lt;/option><br>
 *  &lt;option value="FI">Finland&lt;/option><br>
 *  &lt;option value="FR">France&lt;/option><br>
 *  &lt;option value="FX">France, Metropolitan &lt;/option><br>
 *  &lt;option value="GF">French Guiana&lt;/option><br>
 *  &lt;option value="PF">French Polynesia&lt;/option><br>
 *  &lt;option value="TF">French Southern Territories&lt;/option><br>
 *  &lt;option value="GA">Gabon&lt;/option><br>
 *  &lt;option value="GM">Gambia&lt;/option><br>
 *  &lt;option value="GE">Georgia&lt;/option><br>
 *  &lt;option value="DE">Germany&lt;/option><br>
 *  &lt;option value="GH">Ghana&lt;/option><br>
 *  &lt;option value="GI">Gibraltar&lt;/option><br>
 *  &lt;option value="GR">Greece&lt;/option><br>
 *  &lt;option value="GL">Greenland&lt;/option><br>
 *  &lt;option value="GD">Grenada&lt;/option><br>
 *  &lt;option value="GP">Guadeloupe&lt;/option><br>
 *  &lt;option value="GU">Guam&lt;/option><br>
 *  &lt;option value="GT">Guatemala&lt;/option><br>
 *  &lt;option value="GN">Guinea&lt;/option><br>
 *  &lt;option value="GW">Guinea-Bissau&lt;/option><br>
 *  &lt;option value="GY">Guyana&lt;/option><br>
 *  &lt;option value="HT">Haiti&lt;/option><br>
 *  &lt;option value="HM">Heard And Mc Donald Islands&lt;/option><br>
 *  &lt;option value="HN">Honduras&lt;/option><br>
 *  &lt;option value="HK">Hong Kong&lt;/option><br>
 *  &lt;option value="HU">Hungary&lt;/option><br>
 *  &lt;option value="IS">Iceland&lt;/option><br>
 *  &lt;option value="IN">India&lt;/option><br>
 *  &lt;option value="ID">Indonesia&lt;/option><br>
 *  &lt;option value="IR">Iran&lt;/option><br>
 *  &lt;option value="IQ">Iraq&lt;/option><br>
 *  &lt;option value="IE">Ireland&lt;/option><br>
 *  &lt;option value="IL">Israel&lt;/option><br>
 *  &lt;option value="IT">Italy&lt;/option><br>
 *  &lt;option value="JM">Jamaica&lt;/option><br>
 *  &lt;option value="JP">Japan&lt;/option><br>
 *  &lt;option value="JO">Jordan&lt;/option><br>
 *  &lt;option value="KZ">Kazakhstan&lt;/option><br>
 *  &lt;option value="KE">Kenya&lt;/option><br>
 *  &lt;option value="KI">Kiribati&lt;/option><br>
 *  &lt;option value="KP">North Korea&lt;/option><br>
 *  &lt;option value="KR">South Korea&lt;/option><br>
 *  &lt;option value="KW">Kuwait&lt;/option><br>
 *  &lt;option value="KG">Kyrgyzstan&lt;/option><br>
 *  &lt;option value="LA">Lao People's Republic&lt;/option><br>
 *  &lt;option value="LV">Latvia&lt;/option><br>
 *  &lt;option value="LB">Lebanon&lt;/option><br>
 *  &lt;option value="LS">Lesotho&lt;/option><br>
 *  &lt;option value="LR">Liberia&lt;/option><br>
 *  &lt;option value="LY">Libyan Arab Jamahiriya&lt;/option><br>
 *  &lt;option value="LI">Liechtenstein&lt;/option><br>
 *  &lt;option value="LT">Lithuania&lt;/option><br>
 *  &lt;option value="LU">Luxembourg&lt;/option><br>
 *  &lt;option value="MO">Macau&lt;/option><br>
 *  &lt;option value="MK">Macedonia&lt;/option><br>
 *  &lt;option value="MG">Madagascar&lt;/option><br>
 *  &lt;option value="MW">Malawi&lt;/option><br>
 *  &lt;option value="MY">Malaysia&lt;/option><br>
 *  &lt;option value="MV">Maldives&lt;/option><br>
 *  &lt;option value="ML">Mali&lt;/option><br>
 *  &lt;option value="MT">Malta&lt;/option><br>
 *  &lt;option value="MH">Marshall Islands&lt;/option><br>
 *  &lt;option value="MQ">Martinique&lt;/option><br>
 *  &lt;option value="MR">Mauritania&lt;/option><br>
 *  &lt;option value="MU">Mauritius&lt;/option><br>
 *  &lt;option value="YT">Mayotte&lt;/option><br>
 *  &lt;option value="MX">Mexico&lt;/option><br>
 *  &lt;option value="FM">Micronesia&lt;/option><br>
 *  &lt;option value="MD">Moldova&lt;/option><br>
 *  &lt;option value="MC">Monaco&lt;/option><br>
 *  &lt;option value="MN">Mongolia&lt;/option><br>
 *  &lt;option value="MS">Montserrat&lt;/option><br>
 *  &lt;option value="MA">Morocco&lt;/option><br>
 *  &lt;option value="MZ">Mozambique&lt;/option><br>
 *  &lt;option value="MM">Myanmar&lt;/option><br>
 *  &lt;option value="NA">Namibia&lt;/option><br>
 *  &lt;option value="NR">Nauru&lt;/option><br>
 *  &lt;option value="NP">Nepal&lt;/option><br>
 *  &lt;option value="NL">Netherlands&lt;/option><br>
 *  &lt;option value="AN">Netherlands Antilles&lt;/option><br>
 *  &lt;option value="NC">New Caledonia&lt;/option><br>
 *  &lt;option value="NZ">New Zealand&lt;/option><br>
 *  &lt;option value="NI">Nicaragua&lt;/option><br>
 *  &lt;option value="NE">Niger&lt;/option><br>
 *  &lt;option value="NG">Nigeria&lt;/option><br>
 *  &lt;option value="NU">Niue&lt;/option><br>
 *  &lt;option value="NF">Norfolk Island&lt;/option><br>
 *  &lt;option value="MP">Northern Mariana Islands&lt;/option><br>
 *  &lt;option value="NO">Norway&lt;/option><br>
 *  &lt;option value="OM">Oman&lt;/option><br>
 *  &lt;option value="PK">Pakistan&lt;/option><br>
 *  &lt;option value="PW">Palau&lt;/option><br>
 *  &lt;option value="PA">Panama&lt;/option><br>
 *  &lt;option value="PG">Papua New Guinea&lt;/option><br>
 *  &lt;option value="PY">Paraguay&lt;/option><br>
 *  &lt;option value="PE">Peru&lt;/option><br>
 *  &lt;option value="PH">Philippines&lt;/option><br>
 *  &lt;option value="PN">Pitcairn&lt;/option><br>
 *  &lt;option value="PL">Poland&lt;/option><br>
 *  &lt;option value="PT">Portugal&lt;/option><br>
 *  &lt;option value="PR">Puerto Rico&lt;/option><br>
 *  &lt;option value="QA">Qatar&lt;/option><br>
 *  &lt;option value="RE">Reunion&lt;/option><br>
 *  &lt;option value="RO">Romania&lt;/option><br>
 *  &lt;option value="RU">Russian Federation&lt;/option><br>
 *  &lt;option value="RW">Rwanda&lt;/option><br>
 *  &lt;option value="KN">Saint Kitts And Nevis&lt;/option><br>
 *  &lt;option value="LC">Saint Lucia&lt;/option><br>
 *  &lt;option value="VC">Saint Vincent And The Grenadines&lt;/option><br>
 *  &lt;option value="WS">Samoa&lt;/option><br>
 *  &lt;option value="SM">San Marino&lt;/option><br>
 *  &lt;option value="ST">Sao Tome And Principe&lt;/option><br>
 *  &lt;option value="SA">Saudi Arabia&lt;/option><br>
 *  &lt;option value="SN">Senegal&lt;/option><br>
 *  &lt;option value="SC">Seychelles&lt;/option><br>
 *  &lt;option value="SL">Sierra Leone&lt;/option><br>
 *  &lt;option value="SG">Singapore&lt;/option><br>
 *  &lt;option value="SK">Slovakia&lt;/option><br>
 *  &lt;option value="SI">Slovenia&lt;/option><br>
 *  &lt;option value="SB">Solomon Islands&lt;/option><br>
 *  &lt;option value="SO">Somalia&lt;/option><br>
 *  &lt;option value="ZA">South Africa&lt;/option><br>
 *  &lt;option value="GS">South Georgia & South Sandwich Islands&lt;/option><br>
 *  &lt;option value="ES">Spain&lt;/option><br>
 *  &lt;option value="LK">Sri Lanka&lt;/option><br>
 *  &lt;option value="SH">St Helena&lt;/option><br>
 *  &lt;option value="PM">St Pierre and Miquelon&lt;/option><br>
 *  &lt;option value="SD">Sudan&lt;/option><br>
 *  &lt;option value="SR">Suriname&lt;/option><br>
 *  &lt;option value="SJ">Svalbard And Jan Mayen Islands&lt;/option><br>
 *  &lt;option value="SZ">Swaziland&lt;/option><br>
 *  &lt;option value="SE">Sweden&lt;/option><br>
 *  &lt;option value="CH">Switzerland&lt;/option><br>
 *  &lt;option value="SY">Syrian Arab Republic&lt;/option><br>
 *  &lt;option value="TW">Taiwan&lt;/option><br>
 *  &lt;option value="TJ">Tajikistan&lt;/option><br>
 *  &lt;option value="TZ">Tanzania&lt;/option><br>
 *  &lt;option value="TH">Thailand&lt;/option><br>
 *  &lt;option value="TG">Togo&lt;/option><br>
 *  &lt;option value="TK">Tokelau&lt;/option><br>
 *  &lt;option value="TO">Tonga&lt;/option><br>
 *  &lt;option value="TT">Trinidad And Tobago&lt;/option><br>
 *  &lt;option value="TN">Tunisia&lt;/option><br>
 *  &lt;option value="TR">Turkey&lt;/option><br>
 *  &lt;option value="TM">Turkmenistan&lt;/option><br>
 *  &lt;option value="TC">Turks And Caicos Islands&lt;/option><br>
 *  &lt;option value="TV">Tuvalu&lt;/option><br>
 *  &lt;option value="UG">Uganda&lt;/option><br>
 *  &lt;option value="UA">Ukraine&lt;/option><br>
 *  &lt;option value="AE">United Arab Emirates&lt;/option><br>
 *  &lt;option value="GB">United Kingdom/Great Britain&lt;/option><br>
 *  &lt;option value="UM">United States Minor Outlying Islands&lt;/option><br>
 *  &lt;option value="UY">Uruguay&lt;/option><br>
 *  &lt;option value="UZ">Uzbekistan&lt;/option><br>
 *  &lt;option value="VU">Vanuatu&lt;/option><br>
 *  &lt;option value="VA">Vatican City State&lt;/option><br>
 *  &lt;option value="VE">Venezuela&lt;/option><br>
 *  &lt;option value="VN">Viet Nam&lt;/option><br>
 *  &lt;option value="VG">Virgin Islands (British)&lt;/option><br>
 *  &lt;option value="VI">Virgin Islands (U.S.)&lt;/option><br>
 *  &lt;option value="WF">Wallis And Futuna Islands&lt;/option><br>
 *  &lt;option value="EH">Western Sahara&lt;/option><br>
 *  &lt;option value="YE">Yemen&lt;/option><br>
 *  &lt;option value="ZR">Zaire&lt;/option><br>
 *  &lt;option value="ZM">Zambia&lt;/option><br>
 *  &lt;option value="ZW">Zimbabwe&lt;/option><br>
 *  &lt;option value="ZZ">Other-Not Shown&lt;/option><br>
 * &lt;/select><br>
 *
 * @author David
 *
 */
public class LinkPointCreditCardProcessor extends CreditCardProcessor {

	/**
	 * This method process a credit card order against the Link Point Credit Card Gateway.
	 * Some parameters like the state and country requires a specific codes specified below.
	 *
	 * @param orderId - Required - Order Inode
	 * @param poNumber - Optional - ECommerce transactions with purchase order numbers
	 * @param taxExempt - Required
	 * @param clientIPAddress - Optional - Client IP address - can be obtained using the HttpReqest.getRemoteUser()
	 * @param subtotal - Optional - Before Taxes and Shipping amounts
	 * @param tax - Optional - Tax amount
	 * @param shipping - Optional - Shipping amount
	 * @param total - Required - Total order amount (including taxes and shippinga amounts)
	 * @param cardNumber - Required - Credit Card Number
	 * @param cardExpMonth - Required - The numeric expiration month of the credit card - integer from 1 to 12
	 * @param cardExpYear - Required - The two-digit or four-digit expiration year of the credit card - Integer from 00 to 99 or from 2000 to 2099
	 * @param cvv - Required - 3/4-digit numeric valued typically printed on the signature panel on the back of the credit card.
	 * @param billingName - Required - This should be the customer’s name as it appears on the credit card.
	 * @param billingCompany - Optional - Company name.
	 * @param billingAddress1 - Required - The 1st line of the customer's address.
	 * @param billingAddress2 - Optional - The 2nd line of the customer's address.
	 * @param billingCity - Required - Billing city.
	 * @param billingState - Required - Billing state. For international addresses, you can use this field to hold the province or
	 * 				territory, as applicable. For US states, use one of the US State Codes specified above.
	 * @param billingZip - Required - Billing Zip.
	 * @param billingCountry - Required - Billing country. If passed, must be a valid country code. See Country Codes Above.
	 * @param billingPhone - Optional - Billing phone number.
	 * @param billingFax - Optional -  Billing fax number.
	 * @param billingEmail - Optional -  Billing email address.
	 * @param orderComments - Optional -  Order comments.
	 * @param orderReferred - Optional - Order referred by.
	 * @return CreditCardProcessorResponse The response object with the gateway values.
	 * @throws LinkPointCreditCardProcessorException - Throwed in case of communication error, missing information or credit card denied exception
	 * 			@see LinkPointCreditCardProcessorException
	 */
	
	
	private String poNumber;
	private String orderReferred;
	private String storeId;
	private String orderType;

	public String getOrderType() {
		return orderType;
	}

	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}

	public String getOrderReferred() {
		return orderReferred;
	}

	public void setOrderReferred(String orderReferred) {
		this.orderReferred = orderReferred;
	}

	public String getPoNumber() {
		return poNumber;
	}

	public void setPoNumber(String poNumber) {
		this.poNumber = poNumber;
	}

	public String getStoreId() {
		return storeId;
	}

	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}

	private static LinkPointCreditCardProcessorResponse processCreditCardOrder (String orderId, String poNumber, boolean taxExempt, String clientIPAddress,
			Float subtotal, Float tax, Float shipping, float total,
			String cardNumber, int cardExpMonth, int cardExpYear, String cvv,
			String billingName, String billingCompany,
			String billingAddress1, String billingAddress2, String billingCity,
			String billingState, String billingZip, String billingCountry,
			String billingPhone, String billingFax, String billingEmail,
			String orderComments, String orderReferred, String storeId, String orderType) throws LinkPointCreditCardProcessorException {

		//Setting up the link point connection
		JLinkPointTransaction txn = new JLinkPointTransaction();
		String sClientCertPath = "./bin/" + storeId + ".p12";

		txn.setClientCertificatePath(sClientCertPath);
		txn.setPassword(Config.getStringProperty("LP_CLIENT_CERT_PASS"));		
		txn.setHost(Config.getStringProperty("LP_HOST"));
		txn.setPort(Config.getIntProperty("LP_PORT"));

		//Building the order XML
		StringBuffer orderXml = new StringBuffer ();

		orderXml.append("<order>\n");

		orderXml.append("	<merchantinfo>\n");
		orderXml.append("		<configfile>"+ storeId + "</configfile>\n");
		orderXml.append("	</merchantinfo>\n");

		if (UtilMethods.isSet(orderType)) {
			//orderType can be either: SALE or PREAUTH
			orderXml.append("	<orderoptions>\n");
			orderXml.append("		<ordertype>"+orderType+"</ordertype>\n");
			orderXml.append("		<result>"+Config.getStringProperty("LP_ORDERS_RESULT")+"</result>\n");
			orderXml.append("	</orderoptions>\n");
		}
		else {
			orderXml.append("	<orderoptions>\n");
			orderXml.append("		<ordertype>"+Config.getStringProperty("LP_ORDER_TYPE")+"</ordertype>\n");
			orderXml.append("		<result>"+Config.getStringProperty("LP_ORDERS_RESULT")+"</result>\n");
			orderXml.append("	</orderoptions>\n");
		}

		orderXml.append("	<transactiondetails>\n");
		orderXml.append("		<transactionorigin>ECI</transactionorigin>\n");
		orderXml.append("		<oid>"+orderId+"</oid>\n");
		if (UtilMethods.isSet(poNumber)) {
			orderXml.append("		<ponumber>"+poNumber+"</ponumber>\n");
		}
		orderXml.append("		<taxexempt>" + (taxExempt?"Y":"N") + "</taxexempt>\n");
		orderXml.append("		<terminaltype>UNSPECIFIED</terminaltype>\n");
		if (UtilMethods.isSet(clientIPAddress)) {
			orderXml.append("		<ip>"+clientIPAddress+"</ip>\n");
		}
		orderXml.append("	</transactiondetails>\n");


		orderXml.append("	<payment>\n");
		if (UtilMethods.isSet(subtotal)) {
			orderXml.append("		<subtotal>"+UtilMethods.dollarFormat(subtotal)+"</subtotal>\n");
		}
		if (UtilMethods.isSet(tax)) {
			orderXml.append("		<tax>0"+UtilMethods.dollarFormat(tax)+"</tax>\n");
			orderXml.append("		<vattax>0.0</vattax>\n");
		}
		if (UtilMethods.isSet(shipping)) {
			orderXml.append("		<shipping>"+UtilMethods.dollarFormat(shipping)+"</shipping>\n");
		}
		orderXml.append("		<chargetotal>"+UtilMethods.dollarFormat(total)+"</chargetotal>\n");
		orderXml.append("	</payment>\n");


		orderXml.append("	<creditcard>\n");
		if (UtilMethods.isSet(cardNumber)) {
			orderXml.append("		<cardnumber>" + cardNumber + "</cardnumber>\n");
		} else {
			throw new LinkPointCreditCardProcessorException (LinkPointCreditCardProcessorException.DATA_MISSING, "Card Number Required");
		}
		if (cardExpMonth >= 1 && cardExpMonth <= 12) {
			orderXml.append("		<cardexpmonth>" + cardExpMonth + "</cardexpmonth>\n");
		} else {
			throw new LinkPointCreditCardProcessorException (LinkPointCreditCardProcessorException.DATA_MISSING, "Invalid Card Expiration Month (Valid range 1-12)");
		}
		if (cardExpYear >= 0 && cardExpYear <= 99)
			orderXml.append("		<cardexpyear>" + cardExpYear + "</cardexpyear>\n");
		else if (cardExpYear >= 2000 && cardExpYear <= 2099) {
			cardExpYear = cardExpYear - 2000;
			orderXml.append("		<cardexpyear>" + cardExpYear + "</cardexpyear>\n");
		} else {
			throw new LinkPointCreditCardProcessorException (LinkPointCreditCardProcessorException.DATA_MISSING, "Invalid Card Expiration Year (Ranges Valid 0-99 or 2000-2099)");
		}
		if (UtilMethods.isSet(cvv)) {
			orderXml.append("		<cvmvalue>" + cvv + "</cvmvalue>\n");
			orderXml.append("		<cvmindicator>provided</cvmindicator>\n");
		} else {
			//throw new CreditCardProcessorException (CreditCardProcessorException.DATA_MISSING, "cvv Required");
		}
		orderXml.append("	</creditcard>\n");

		orderXml.append("	<billing>\n");
		if (UtilMethods.isSet(billingAddress1)) {
			String[] addressSplitted = billingAddress1.split(" ");
			if (addressSplitted.length > 0) {
				try {
					int addrnum = Integer.parseInt(addressSplitted[0]);
					orderXml.append("		<addrnum>" + addrnum + "</addrnum>\n");
				} catch (Exception e) {
				}
			}
		} else {
			throw new LinkPointCreditCardProcessorException (LinkPointCreditCardProcessorException.DATA_MISSING, "billingAddress1 Value Required");
		}
		if (UtilMethods.isSet(billingZip)) {
			orderXml.append("		<zip>" + billingZip + "</zip>\n");
		} else {
			throw new LinkPointCreditCardProcessorException (LinkPointCreditCardProcessorException.DATA_MISSING, "billingZip Value Required");
		}
		if (UtilMethods.isSet(billingName)) {
			orderXml.append("		<name>" + billingName + "</name>\n");
		} else {
			throw new LinkPointCreditCardProcessorException (LinkPointCreditCardProcessorException.DATA_MISSING, "billingName Value Required");
		}
		if (UtilMethods.isSet(billingCompany)) {
			orderXml.append("		<company>" + billingCompany + "</company>\n");
		}
		orderXml.append("		<address1>" + billingAddress1 + "</address1>\n");
		if (UtilMethods.isSet(billingAddress2)) {
			orderXml.append("		<address2>" + billingAddress2 + "</address2>\n");
		}
		if (UtilMethods.isSet(billingCity)) {
			orderXml.append("		<city>" + billingCity + "</city>\n");
		} else {
			throw new LinkPointCreditCardProcessorException (LinkPointCreditCardProcessorException.DATA_MISSING, "billingCity Value Required");
		}
		if (UtilMethods.isSet(billingState)) {
			orderXml.append("		<state>" + billingState + "</state>\n");
		} else {
			throw new LinkPointCreditCardProcessorException (LinkPointCreditCardProcessorException.DATA_MISSING, "billingState Value Required");
		}
		if (UtilMethods.isSet(billingCountry)) {
			orderXml.append("		<country>" + billingCountry + "</country>\n");
		} else {
			throw new LinkPointCreditCardProcessorException (LinkPointCreditCardProcessorException.DATA_MISSING, "billingCountry Value Required");
		}
		if (UtilMethods.isSet(billingPhone)) {
			orderXml.append("		<phone>" + billingPhone + "</phone>\n");
		}
		if (UtilMethods.isSet(billingFax)) {
			orderXml.append("		<fax>" + billingFax + "</fax>\n");
		}
		//if (UtilMethods.isSet(billingEmail)) {
		//	orderXml.append("		<email>" + billingEmail + "</email>\n");
		//}
		orderXml.append("	</billing>\n");

		if (UtilMethods.isSet(orderComments) || UtilMethods.isSet(orderReferred)) {
			orderXml.append("	<notes>\n");
			if (UtilMethods.isSet(orderComments))
				orderXml.append("		<comments>" + orderComments + "</comments>\n");
			if (UtilMethods.isSet(orderReferred))
				orderXml.append("		<referred>" + orderReferred + "</referred>\n");

			orderXml.append("	</notes>\n");
		}

		orderXml.append("</order>\n");

		if (Config.getBooleanProperty("LP_LOG_ORDERS"))
			Logger.info(LinkPointCreditCardProcessor.class, "Placing an order: \n" + orderXml);

		try {
			String sResponse = "<response>" + txn.send(orderXml.toString()) + "</response>";
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse( new InputSource ( new StringReader (sResponse)) );

			String approved = "";
			String avs = "";
			String ordernum = "";
			String error = "";
			String code = "";
			String message = "";
			String time = "";
			String ref = "";
			String tdate = "";
			String score = "";

			NodeList nl = document.getElementsByTagName("r_avs");
			if (nl.item(0) != null && nl.item(0).getFirstChild() != null) {
				avs = nl.item(0).getFirstChild().getNodeValue();
			}
			nl = document.getElementsByTagName("r_ordernum");
			if (nl.item(0) != null && nl.item(0).getFirstChild() != null) {
				ordernum = nl.item(0).getFirstChild().getNodeValue();
			}
			nl = document.getElementsByTagName("r_error");
			if (nl.item(0) != null && nl.item(0).getFirstChild() != null) {
				error = nl.item(0).getFirstChild().getNodeValue();
			}
			nl = document.getElementsByTagName("r_approved");
			if (nl.item(0) != null && nl.item(0).getFirstChild() != null) {
				approved = nl.item(0).getFirstChild().getNodeValue();
			}
			nl = document.getElementsByTagName("r_code");
			if (nl.item(0) != null && nl.item(0).getFirstChild() != null) {
				code = nl.item(0).getFirstChild().getNodeValue();
			}
			nl = document.getElementsByTagName("r_message");
			if (nl.item(0) != null && nl.item(0).getFirstChild() != null) {
				message = nl.item(0).getFirstChild().getNodeValue();
			}
			nl = document.getElementsByTagName("r_time");
			if (nl.item(0) != null && nl.item(0).getFirstChild() != null) {
				time = nl.item(0).getFirstChild().getNodeValue();
			}
			nl = document.getElementsByTagName("r_ref");
			if (nl.item(0) != null && nl.item(0).getFirstChild() != null) {
				ref = nl.item(0).getFirstChild().getNodeValue();
			}
			nl = document.getElementsByTagName("r_tdate");
			if (nl.item(0) != null && nl.item(0).getFirstChild() != null) {
				tdate = nl.item(0).getFirstChild().getNodeValue();
			}
			nl = document.getElementsByTagName("r_score");
			if (nl.item(0) != null && nl.item(0).getFirstChild() != null) {
				score = nl.item(0).getFirstChild().getNodeValue();
			}

			LinkPointCreditCardProcessorResponse orderResponse = new LinkPointCreditCardProcessorResponse (approved, avs, code, error, message, ordernum, ref, score, tdate, time);

			if (Config.getBooleanProperty("LP_LOG_ORDERS_RESPONSE"))
				Logger.info(LinkPointCreditCardProcessor.class, "Order Response: " + orderResponse.toString());

			return orderResponse;

		} 
		catch (Exception e) 
		{		
			Logger.error(LinkPointCreditCardProcessor.class, e.toString());
			throw new LinkPointCreditCardProcessorException (LinkPointCreditCardProcessorException.COMMUNICATION_ERROR, e.getMessage(), e);
		}
	}
	
	public LinkPointCreditCardProcessorResponse process() throws LinkPointCreditCardProcessorException
	{		
		Date creditCardExpirationDate = getCreditCardExpirationDate();
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(creditCardExpirationDate);
		int cardExpMonth = gc.get(Calendar.MONTH);
		cardExpMonth++;
		int cardExpYear = gc.get(Calendar.YEAR);
		String billingName = billingFirstName + " " + billingLastName;
		
		if (!UtilMethods.isSet(storeId)) {
			storeId = Config.getStringProperty("LP_ECOMM_STORE_ID");
		}
		
		if ((subtotal +  tax + shipping) != amount) {
			if ((tax == 0) && (shipping == 0)) {
				subtotal = amount;
			}
		}
		
		try
		{
		return processCreditCardOrder (getOrderId(),  poNumber, isTaxExempt(),  getClientIPAddress(),
				 subtotal,  tax,  shipping,  amount,
				 creditCardNumber,  cardExpMonth,  cardExpYear,  creditCardCVV,
				 billingName,  billingCompany,
				 billingStreet,  billingStreet2,  billingCity,
				 billingState,  billingZip,  billingCountry,
				 billingPhone,  billingFax,  billingEmailAdress,
				 getOrderComments(),  orderReferred,  storeId,  orderType);
		}
		catch(LinkPointCreditCardProcessorException ex)
		{
			Logger.debug(this,ex.toString());
			throw ex;
		}
	}
}
