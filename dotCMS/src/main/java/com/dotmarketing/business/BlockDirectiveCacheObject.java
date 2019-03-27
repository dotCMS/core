package com.dotmarketing.business;

import java.io.Serializable;

public class BlockDirectiveCacheObject implements Serializable {


		private static final long serialVersionUID = 1L;
		String value;
		long created = 0;
		int ttl=0;
		public int getTtl() {
			return ttl;
		}

		public BlockDirectiveCacheObject(String value, int ttl){
			this.ttl = ttl;
			this.value=value;
			created = System.currentTimeMillis();
		}

		public void setTtl(int ttl) {
			this.ttl = ttl;
		}



		public BlockDirectiveCacheObject(){
			created = System.currentTimeMillis();
		}
		
		
		
		public long getCreated() {
			return created;
		}




		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		
		
		
		
		
		

}
