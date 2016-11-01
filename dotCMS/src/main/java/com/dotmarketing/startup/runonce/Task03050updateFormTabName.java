package com.dotmarketing.startup.runonce;

import java.util.List;

import com.dotmarketing.startup.*;

public class Task03050updateFormTabName extends AbstractJDBCStartupTask {

	@Override
	public String getMSSQLScript() {
		return "UPDATE cms_layout SET layout_name='Forms & Polls' WHERE id='81c3bb50-9f64-4a39-9027-46ee92192799'; ";
	}

	@Override
	public String getMySQLScript() {
		return "UPDATE cms_layout SET layout_name='Forms & Polls' WHERE id='81c3bb50-9f64-4a39-9027-46ee92192799'; ";
	}

	@Override
	public String getOracleScript() {
		return "UPDATE cms_layout SET layout_name='Forms & Polls' WHERE id='81c3bb50-9f64-4a39-9027-46ee92192799'; ";
	}

	@Override
	public String getPostgresScript() {
		return "UPDATE cms_layout SET layout_name='Forms & Polls' WHERE id='81c3bb50-9f64-4a39-9027-46ee92192799'; ";
	}

	@Override
	protected List<String> getTablesToDropConstraints() {
		return null;
	}

	public boolean forceRun() {
		return true;
	}

    @Override
    public String getH2Script() {
        return "UPDATE cms_layout SET layout_name='Forms & Polls' WHERE id='81c3bb50-9f64-4a39-9027-46ee92192799'; ";
    }

}
