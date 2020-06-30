package com.dotcms.publisher.integrity;

import com.dotcms.business.CloseDBIfOpened;
import javax.servlet.ServletContext;

import com.dotcms.integritycheckers.IntegrityUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.rest.IntegrityResource.ProcessStatus;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;

public class IntegrityDataGeneratorThread extends Thread {

    private String transactionId;
    public ServletContext servletContext;

    public IntegrityDataGeneratorThread(final String transactionId, final ServletContext servletContext) {
        this.transactionId = transactionId;
        this.servletContext = servletContext;
    }

    @CloseDBIfOpened
    public void run() {

        try {

            if(transactionId==null)
                throw new Exception("Not valid endpoint provided");

            servletContext.setAttribute("integrityDataGenerationStatus", ProcessStatus.PROCESSING);

            IntegrityUtil integrityUtil = new IntegrityUtil();
            integrityUtil.generateDataToCheckZip(transactionId);

        } catch (Exception e) {

            //Special handling if the thread was interrupted
            if ( e instanceof InterruptedException ) {
                //Setting the process status
                servletContext.setAttribute( "integrityDataGenerationStatus", ProcessStatus.CANCELED );
                servletContext.setAttribute( "integrityDataGenerationError", e.getMessage() );
                Logger.debug( IntegrityDataGeneratorThread.class, "Requested interruption of generation of data to check by the user.", e );
                throw new RuntimeException( "Requested interruption of generation of data to check by the user.", e );
            }

            Logger.error(IntegrityDataGeneratorThread.class, "Error generating data to check", e);
            servletContext.setAttribute("integrityDataGenerationStatus", ProcessStatus.ERROR);
            servletContext.setAttribute("integrityDataGenerationError", e.getMessage());
        } finally {
            servletContext.setAttribute("integrityDataGenerationStatus", ProcessStatus.FINISHED);
        }
    }

}
