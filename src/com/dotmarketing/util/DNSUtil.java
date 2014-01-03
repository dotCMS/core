package com.dotmarketing.util;

import java.io.IOException;

import com.dotcms.repackage.dnsjava_2_0_8.org.xbill.DNS.DClass;
import com.dotcms.repackage.dnsjava_2_0_8.org.xbill.DNS.ExtendedResolver;
import com.dotcms.repackage.dnsjava_2_0_8.org.xbill.DNS.Message;
import com.dotcms.repackage.dnsjava_2_0_8.org.xbill.DNS.Name;
import com.dotcms.repackage.dnsjava_2_0_8.org.xbill.DNS.Record;
import com.dotcms.repackage.dnsjava_2_0_8.org.xbill.DNS.Resolver;
import com.dotcms.repackage.dnsjava_2_0_8.org.xbill.DNS.ReverseMap;
import com.dotcms.repackage.dnsjava_2_0_8.org.xbill.DNS.Section;
import com.dotcms.repackage.dnsjava_2_0_8.org.xbill.DNS.Type;

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
