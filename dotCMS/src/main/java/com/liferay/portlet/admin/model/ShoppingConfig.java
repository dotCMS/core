/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet.admin.model;

import java.io.Serializable;

import com.liferay.util.StringPool;
import com.liferay.util.Validator;

/**
 * <a href="ShoppingConfig.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.14 $
 *
 */
public class ShoppingConfig implements Serializable {

	public static final String SHOPPING_CONFIG = "SHOPPING_CONFIG";

	public static final String[] CC_TYPES =
		new String[] {"cc_visa", "cc_mastercard", "cc_discover", "cc_amex"};

	public static final String[] CURRENCY_IDS =
		new String[] {"USD", "CAD", "EUR", "GBP", "JPY"};

	public static final String DEFAULT_CURRENCY_ID = "USD";

	public static final String DEFAULT_TAX_STATE = "CA";

	public static final double DEFAULT_TAX_RATE = 0.0;

	public static final String SHIPPING_FLAT_AMOUNT = "SHIPPING_FLAT_AMOUNT";

	public static final String SHIPPING_PERCENTAGE = "SHIPPING_PERCENTAGE";

	public static final String DEFAULT_SHIPPING_FORMULA = SHIPPING_FLAT_AMOUNT;

	public static final String[] DEFAULT_SHIPPING = new String[] {
		StringPool.BLANK, StringPool.BLANK, StringPool.BLANK, StringPool.BLANK,
		StringPool.BLANK
	};

	public static final double[] SHIPPING_RANGE = {
		0.01, 9.99, 10.00, 49.99, 50.00, 99.99, 100.00, 199.99, 200.00,
		Double.POSITIVE_INFINITY
	};

	public static final String[][] DEFAULT_ALTERNATIVE_SHIPPING =
		new String[0][0];

	public static final double DEFAULT_MIN_ORDER = 0.0;

	public static final boolean DEFAULT_SHOW_SPECIAL_ITEMS = false;

	public ShoppingConfig() {
	}

	public ShoppingConfig(String payPalEmailAddress, String[] ccTypes,
						  String currencyId, String taxState, double taxRate,
						  String shippingFormula, String[] shipping,
						  String[][] alternativeShipping, double minOrder,
						  boolean showSpecialItems, EmailConfig orderEmail,
						  EmailConfig shippingEmail) {

		_payPalEmailAddress = payPalEmailAddress;
		_ccTypes = ccTypes;
		_currencyId = currencyId;
		_taxState = taxState;
		_taxRate = taxRate;
		_shippingFormula = shippingFormula;
		_shipping = shipping;
		_alternativeShipping = alternativeShipping;
		_minOrder = minOrder;
		_showSpecialItems = showSpecialItems;
		_orderEmail = orderEmail;
		_shippingEmail = shippingEmail;
	}

	public String getPayPalEmailAddress() {
		return _payPalEmailAddress;
	}

	public void setPayPalEmailAddress(String payPalEmailAddress) {
		_payPalEmailAddress = payPalEmailAddress;
	}

	public boolean usePayPal() {
		return Validator.isNotNull(getPayPalEmailAddress());
	}

	public String getCurrencyId() {
		return _currencyId;
	}

	public void setCurrencyId(String currencyId) {
		_currencyId = currencyId;
	}

	public String[] getCcTypes() {
		return _ccTypes;
	}

	public void setCcTypes(String[] ccTypes) {
		_ccTypes = ccTypes;
	}

	public String getTaxState() {
		return _taxState;
	}

	public void setTaxState(String taxState) {
		_taxState = taxState;
	}

	public double getTaxRate() {
		return _taxRate;
	}

	public void setTaxRate(double taxRate) {
		_taxRate = taxRate;
	}

	public String getShippingFormula() {
		return _shippingFormula;
	}

	public void setShippingFormula(String shippingFormula) {
		_shippingFormula = shippingFormula;
	}

	public String[] getShipping() {
		return _shipping;
	}

	public void setShipping(String[] shipping) {
		_shipping = shipping;
	}

	public String[][] getAlternativeShipping() {
		return _alternativeShipping;
	}

	public void setAlternativeShipping(String[][] alternativeShipping) {
		_alternativeShipping = alternativeShipping;
	}

	public boolean useAlternativeShipping() {
		if (_alternativeShipping != null) {
			try {
				for (int i = 0; i < 10; i++) {
					if (Validator.isNotNull(_alternativeShipping[0][i]) &&
						Validator.isNotNull(_alternativeShipping[1][i])) {

						return true;
					}
				}
			}
			catch (Exception e) {
			}
		}

		return false;
	}

	public String getAlternativeShippingName(int altShipping) {
		String altShippingName = StringPool.BLANK;

		try {
			altShippingName = _alternativeShipping[0][altShipping];
		}
		catch (Exception e) {
		}

		return altShippingName;
	}

	public double getMinOrder() {
		return _minOrder;
	}

	public void setMinOrder(double minOrder) {
		_minOrder = minOrder;
	}

	public boolean getShowSpecialItems() {
		return _showSpecialItems;
	}

	public boolean isShowSpecialItems() {
		return _showSpecialItems;
	}

	public void setShowSpecialItems(boolean showSpecialItems) {
		_showSpecialItems = showSpecialItems;
	}

	public EmailConfig getOrderEmail() {
		return _orderEmail;
	}

	public void setOrderEmail(EmailConfig orderEmail) {
		_orderEmail = orderEmail;
	}

	public EmailConfig getShippingEmail() {
		return _shippingEmail;
	}

	public void setShippingEmail(EmailConfig shippingEmail) {
		_shippingEmail = shippingEmail;
	}

	private String _payPalEmailAddress;
	private String[] _ccTypes;
	private String _currencyId;
	private String _taxState;
	private double _taxRate;
	private String _shippingFormula;
	private String[] _shipping;
	private String[][] _alternativeShipping;
	private double _minOrder;
	private boolean _showSpecialItems;
	private EmailConfig _orderEmail;
	private EmailConfig _shippingEmail;

}