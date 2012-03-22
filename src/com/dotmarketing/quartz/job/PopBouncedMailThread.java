package com.dotmarketing.quartz.job;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import javax.net.ssl.SSLSocketFactory;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.campaigns.model.Campaign;
import com.dotmarketing.portlets.campaigns.model.Recipient;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * This is a quartz kind of job used to retrieved bounced mails notifications from multiple
 * pop 3 accounts configured through the dotmarketing.config.properties
 * @author David
 * @version 1.5
 *
 */
public class PopBouncedMailThread implements Job {

	private JobExecutionContext context;

	public PopBouncedMailThread() {
	}

	/**
	 * @return the context
	 */
	public JobExecutionContext getContext() {
		return context;
	}
	/**
	 * @param context the context to set
	 */
	public void setContext(JobExecutionContext context) {
		this.context = context;
	}

	public void execute(JobExecutionContext context) throws JobExecutionException {

		Logger.debug(this, "Running PopBouncedMailThread - " + new Date());

		pullMail();
		
		Logger.debug(this, "PopBouncedMailThread Finished - " + new Date());
		
	
	}
	
	private Socket openConnection () throws UnknownHostException, IOException {
		
		Socket s = null;
		String pop3Server = Config.getStringProperty("POP3_SERVER");
		int pop3Port = Config.getIntProperty("POP3_PORT");
		boolean isSSL = Config.getBooleanProperty("POP3_SSL_ENABLED");

		//Opening pop3 server connection

		Logger.debug(this, "Connecting to: " + pop3Server + ":" + pop3Port + (isSSL?" using an SSL socket.":""));
		
		if(isSSL) {
			SSLSocketFactory factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
			s = factory.createSocket(pop3Server, pop3Port);
		} else {
			s = new Socket(pop3Server, pop3Port);
		}

		return s;
	}
	
	public void pullMail() {

		Socket s = null;
		
		try {
			
			s = openConnection();
			
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

			int userCounter = 1;
			
			while(Config.containsProperty("POP3_USER_" + userCounter)) {
				String pop3User = Config.getStringProperty("POP3_USER_" + userCounter);
				String pop3Password = Config.getStringProperty("POP3_PASSWORD_" + userCounter);
				try {
					Logger.debug(this, "Retrieving bounces from account: " + pop3User);
					
					loginMail(in, out, pop3User, pop3Password);
					int i = checkMyMail(in, out);
		
					Logger.debug(this, "Found " + i + " number of messages to check on this account!");
	
					for (int j = 1; j <= i; j++) {
						getMail(in, out, j);
					}
					
					// If the mail was removed from the server
					// (see getMail()) then we must COMMIT with
					// the "QUIT" command :
					 send(out, "QUIT");
					 send(out, "");
					 s.close();
					 
				} catch (Exception e) {
					
					Logger.error(this, "An error ocurred trying to process the email from account " + pop3User + "/" + pop3Password + ", will try with the next account.", e);
					
					//resetting the connection to try with the next account
					if(s != null) {
						try {
							s.close();
						} catch (IOException ex) {
							Logger.error(this, "Error ocurred closing the socket opened against the pop3 server", ex);
						}
					}
					
					s = openConnection();
					in = new BufferedReader(new InputStreamReader(s.getInputStream()));
					out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
					
				} finally {
					userCounter++;
					s = openConnection();
					in = new BufferedReader(new InputStreamReader(s.getInputStream()));
					out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
				}
			}
		} catch (Exception e) {
			Logger.warn(this, "e=" + e.getMessage(), e);
		} finally  {
			if(s != null) {
				try {
					s.close();
				} catch (IOException e) {
					Logger.error(this, "Error ocurred closing the socket opened against the pop3 server",  e);
				}
			}
		}
	}

	public void getMail(BufferedReader in, BufferedWriter out, int i) throws Exception {

		String s = "";
		
		String errorMessage = null;
		
		Logger.debug(this, "RETR: before:" +i);

		send(out, "RETR " + i);

		Logger.debug(this, "RETR: after:" +i);

        boolean removeEmail = false;

		Recipient r = new Recipient();
        
		while (((s = in.readLine()) != null) && (!(s.equals(".")))) {
			//t += s + "\n";
			//get error message
			
			if (s.startsWith("<<< ")) {
				errorMessage = s.substring(4).trim();

			}

			//get recipient id
			if (s.startsWith("X-RecipientId:")) {
				java.util.StringTokenizer st = new java.util.StringTokenizer(s, ": ");
				st.nextToken();

				Logger.debug(this, "Found a X-Recipient ID=" + s);

				try {
					r = (Recipient) InodeFactory.getInode(st.nextToken(), Recipient.class);
				} catch (Exception e) {
					Logger.debug(this, "Recipient=" + r.getInode() + ", not found might be that the campaign was deleted.");
				}
				removeEmail = true;

				Logger.debug(this, "Found recipient=" + r.getInode());
			}

		}

		//if we have a recipient
        if (InodeUtils.isSet(r.getInode())) {
            r.setLastResult(500);

        	if(errorMessage == null){
	            r.setLastMessage("Email Bounced");
        	}
        	else {
	            r.setLastMessage(errorMessage);
        	}
            
            HibernateUtil.saveOrUpdate(r);

            Logger.debug(this, "Saved recipient with 500 last result" + r.getEmail());
            
            User user = APILocator.getUserAPI().loadByUserByEmail(r.getEmail(), APILocator.getUserAPI().getSystemUser(), false);
            UserProxy sub = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false); 

            Logger.debug(this, "Subscriber =" + sub.getInode());

            if(InodeUtils.isSet(sub.getInode())){

                Logger.debug(this, "errorMessage: " +errorMessage);

            	if(errorMessage == null){
            			sub.setLastMessage("Email Bounced");
            	}
            	else{
                	sub.setLastMessage(errorMessage.replaceAll("<","&lt;").replaceAll(">", "&gt;"));

            	}
                sub.setLastResult(500);

                HibernateUtil.saveOrUpdate(sub);

                Campaign c = (Campaign) InodeFactory.getParentOfClass(r, Campaign.class);
                if(c != null && InodeUtils.isSet(c.getInode())) {
					MailingList ml = (MailingList) InodeFactory.getChildOfClass(c,MailingList.class);
	                if(ml != null && InodeUtils.isSet(ml.getInode()))
						MailingListFactory.markAsBounceFromMailingList(ml, sub);
                }
                
            }
            Logger.info(this, "Found a bounce for recipient: " + r.getInode());
        }

        if(removeEmail) {
            Logger.debug(this, "Before deleting message");
            send(out, "DELE " + i);
            receive(in);
            Logger.debug(this, "After deleting message");
        }
        
        Logger.debug(this, "Returning from getMail");
        
	}

	private void send(BufferedWriter out, String s) throws IOException {
		out.write(s + "\r\n");
		out.flush();
	}

	private String receive(BufferedReader in) throws IOException {
		return in.readLine();
	}

	private void loginMail(BufferedReader in, BufferedWriter out, String user, String pass) throws IOException {
        Logger.debug(this, "Logging in with user=" + user + " pwd=" + pass);
		String resp = receive(in);
		send(out, "USER " + user);
		resp = receive(in);
		send(out, "PASS " + pass);
		resp = receive(in);
		if(!resp.startsWith("+OK")) {
			throw new IOException ("Unable to login with user = " + user);
		}
        Logger.debug(this, "Logged in!");
	}

	private int checkMyMail(BufferedReader in, BufferedWriter out) throws IOException {
		return GetNumberOfMessages(in, out);
	}

	public int GetNumberOfMessages(BufferedReader in, BufferedWriter out) throws IOException {
		int i = 0;
		String s;

		send(out, "LIST");
		receive(in);
		while ((s = receive(in)) != null) {
	        Logger.debug(this, "Getting number of messages=" + s);
			if (!(s.equals("."))) {
				i++;
			} else
				return i;
		}
		return 0;
	}
	
	
}