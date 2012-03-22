package com.dotcms.content.elasticsearch.business;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class ESContentPersisterFS extends ESContentPersister {

	private List<Contentlet> contentlets;

	public void setContentlet(Contentlet con) {
		contentlets = new ArrayList<Contentlet>();
		contentlets.add(con);
	}

	public void setContentlets(List<Contentlet> contentlets) {
		this.contentlets = contentlets;
	}

	@Override
	public void run() {
		for (Contentlet contentlet : contentlets) {

			if (contentlet == null || contentlet.getInode() == null) {
				throw new DotStateException("writing a contentlet must have an inode");

			}
			String id = contentlet.getInode();

			String fileName = "/assets/" + id.charAt(0) + "/" + id.charAt(1) + "/" + id + ".dotJson";
			FileWriter fw = null;
			try {
				fw = new FileWriter(new File(Config.CONTEXT.getRealPath(fileName)));
				fw.write(new ESMappingAPIImpl().toJson(contentlet));

			} catch (Exception e) {
				Logger.warn(this.getClass(), e.getMessage());
				throw new DotStateException("Failing to write " + contentlet.getInode() + ":" + e);
			} finally {
				if (fw != null) {
					try {
						fw.close();
						fw = null;
						contentlet = null;

					} catch (Exception e) {
						throw new DotStateException("Failing to close " + contentlet.getInode() + ":" + e);
					}
				}

			}
		}

	}

}
