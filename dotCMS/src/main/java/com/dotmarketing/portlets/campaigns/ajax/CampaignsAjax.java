package com.dotmarketing.portlets.campaigns.ajax;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.util.Config;

/**
 * @author David
 */
public class CampaignsAjax {

	public List<String> getValidBounceCheckedAccount() {
		ArrayList<String> accounts = new ArrayList<String>();
		int accountCounter = 1;
		while (Config.containsProperty("POP3_USER_" + accountCounter)) {
			accounts.add(Config.getStringProperty("POP3_USER_" + accountCounter));
			accountCounter++;
		}
		return accounts;
    }

}