package com.dotmarketing.logConsole.business;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.dotmarketing.logConsole.model.LogMapper;
import com.dotmarketing.logConsole.model.LogMapperRow;
import com.dotmarketing.portal.struts.DotPortletAction;

public class UpdateLogConfigAction extends DotPortletAction {

	ConsoleLogFactory clf = new ConsoleLogFactoryImpl();

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res) throws Exception {

		System.out.println("EXECUTE LOG-CONFIGURATION ACTION");
		return super.execute(mapping, form, req, res);
	}

	@Override
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {

		String[] activeLogs = (String[]) req.getParameterValues("logs");

		_processUpdate(activeLogs);

		LogMapper.getInstance().updateLogsList();

	}

	@Override
	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {

		return mapping.findForward("portlet.ext.plugins.logMan.struts");
	}

	private void _processUpdate(String[] activeLogs) {

		List<LogMapperRow> l = LogMapper.getInstance().getLogList();

		for (int j = 0; j < l.size(); j++) {
			LogMapperRow lmr = (LogMapperRow) l.get(j);

			boolean checked = false;

			if (activeLogs != null) {
				for (int i = 0; i < activeLogs.length; i++) {

					if (Integer.parseInt(activeLogs[i]) == j) {
						lmr.setEnabled(1);

						checked = true;
					}
				}

				if (!checked) {
					lmr.setEnabled(0);

				}
			} else {
				lmr.setEnabled(0);
			}

		}

	}

}
