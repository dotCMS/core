package com.dotmarketing.util;

import java.io.IOException;

import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

public class DNSUtil {
	public static String reverseDns(String hostIp) throws IOException {
		Record opt = null;
		Resolver res = new ExtendedResolver();
		res.setTimeout(10);
		Name name = ReverseMap.fromAddress(hostIp);
		int type = Type.PTR;
		int dclass = DClass.IN;
		Record rec = Record.newRecord(name, type, dclass);
		Message query = Message.newQuery(rec);
		Message response = res.send(query);

		Record[] answers = response.getSectionArray(Section.ANSWER);
		if (answers.length == 0)
			return hostIp;
		else
			return answers[0].rdataToString();
	}

}
