package com.dotmarketing.cms.formpipeline.business;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class FormPipeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public void init(ServletConfig config) throws ServletException {

    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

       
        FormPipeBean fpb = new FormPipeBean();
        try {
            fpb.setUnderlyingRequest(request);
            fpb.setUnderlyingResponse(response);
            boolean rollBack = false;
            List<FormPipe> pipes = fpb.getPipes();
            HibernateUtil.startTransaction();

            for (FormPipe pipe : pipes) {

                try {
                    pipe.runForm(fpb);
                } catch (FormPipeException fpe) {

                    if (fpe.isRollBack()) {
                        rollBack = true;
                    }
                    if (fpe.isStopProcessing()) {
                        break;
                    }
                }
            }
            if (rollBack) {
                HibernateUtil.rollbackTransaction();

            } else {
                HibernateUtil.commitTransaction();
            }
        } catch (FormPipeException e) {
            Logger.info(this, "Error in the FormPipe : " + fpb.getFormPipe());
            Logger.debug(this, "Error in the FormPipe : " + fpb.getFormPipe(), e);
        }
        catch(DotHibernateException dhe){
            Logger.error(this, "Error in the FormPipe : " + fpb.getFormPipe(), dhe);
        }

        
 

        if (UtilMethods.isSet(fpb.isRedirect())) {
            if (fpb.getMessages() != null && fpb.getMessages().size() > 0) {
                request.getSession().setAttribute("com.dotmarketing.formpipe.messages", fpb.getMessages());
            }
            if (fpb.getErrorMessages() != null && fpb.getErrorMessages().size() > 0) {
                request.getSession().setAttribute("com.dotmarketing.formpipe.errors", fpb.getErrorMessages());
            }
            response.sendRedirect(fpb.getReturnUrl());
            return;

        } else {
            if (fpb.getMessages() != null && fpb.getMessages().size() > 0) {
                request.setAttribute("com.dotmarketing.formpipe.messages", fpb.getMessages());
            }
            if (fpb.getErrorMessages() != null && fpb.getErrorMessages().size() > 0) {
                request.setAttribute("com.dotmarketing.formpipe.errors", fpb.getErrorMessages());
            }
            request.getRequestDispatcher(fpb.getReturnUrl()).forward(request, response);
        }

    }

}
