package com.dotcms.cluster.bean;

import java.io.Serializable;
import java.util.Date;
import javax.annotation.Generated;


public class Server implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final String serverId;
	public final  String clusterId;
	public final  String ipAddress;
	public final  String host;
	public final  String name;
	public final  Integer esTransportTcpPort;
	public final  Integer esNetworkPort;
	public final  Integer esHttpPort;
	public final  Integer cachePort;
	public final  long lastHeartBeat;
	public final  String key;
	public final  String licenseSerial;
	public final  long startupTime;









	@Override
    public String toString() {
        return "server={serverId=" + serverId + ", clusterId=" + clusterId + ", ipAddress=" + ipAddress + ", host="
                        + host + ", name=" + name + ", esTransportTcpPort=" + esTransportTcpPort + ", esNetworkPort="
                        + esNetworkPort + ", esHttpPort=" + esHttpPort + ", cachePort=" + cachePort + ", lastHeartBeat="
                        + lastHeartBeat + ", key=" + key + ", licenseSerial=" + licenseSerial + ", startupTime="
                        + startupTime + "}";
    }






    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cachePort == null) ? 0 : cachePort.hashCode());
        result = prime * result + ((clusterId == null) ? 0 : clusterId.hashCode());
        result = prime * result + ((esHttpPort == null) ? 0 : esHttpPort.hashCode());
        result = prime * result + ((esNetworkPort == null) ? 0 : esNetworkPort.hashCode());
        result = prime * result + ((esTransportTcpPort == null) ? 0 : esTransportTcpPort.hashCode());
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((licenseSerial == null) ? 0 : licenseSerial.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
        //result = prime * result + (int) (startupTime ^ (startupTime >>> 32));
        return result;
    }






    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Server other = (Server) obj;
        if (cachePort == null) {
            if (other.cachePort != null)
                return false;
        } else if (!cachePort.equals(other.cachePort))
            return false;
        if (clusterId == null) {
            if (other.clusterId != null)
                return false;
        } else if (!clusterId.equals(other.clusterId))
            return false;
        if (esHttpPort == null) {
            if (other.esHttpPort != null)
                return false;
        } else if (!esHttpPort.equals(other.esHttpPort))
            return false;
        if (esNetworkPort == null) {
            if (other.esNetworkPort != null)
                return false;
        } else if (!esNetworkPort.equals(other.esNetworkPort))
            return false;
        if (esTransportTcpPort == null) {
            if (other.esTransportTcpPort != null)
                return false;
        } else if (!esTransportTcpPort.equals(other.esTransportTcpPort))
            return false;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (ipAddress == null) {
            if (other.ipAddress != null)
                return false;
        } else if (!ipAddress.equals(other.ipAddress))
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (licenseSerial == null) {
            if (other.licenseSerial != null)
                return false;
        } else if (!licenseSerial.equals(other.licenseSerial))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (serverId == null) {
            if (other.serverId != null)
                return false;
        } else if (!serverId.equals(other.serverId))
            return false;
        //if (startupTime != other.startupTime)
        //    return false;
        return true;
    }






    @Generated("SparkTools")
	private Server(Builder builder) {
		this.serverId = builder.serverId;
		this.clusterId = builder.clusterId;
		this.ipAddress = builder.ipAddress;
		this.host = builder.host;
		this.name = builder.name;
		this.esTransportTcpPort = builder.esTransportTcpPort;
		this.esNetworkPort = builder.esNetworkPort;
		this.esHttpPort = builder.esHttpPort;
		this.cachePort = builder.cachePort;
		this.lastHeartBeat = builder.lastHeartBeat;
		this.key = builder.key;
		this.licenseSerial = builder.licenseSerial;
		this.startupTime=builder.startupTime;
	}
	
	public long getStartupTime() {
	    return startupTime;
	}

	public String getServerId() {
		return serverId;
	}

	public String getClusterId() {
		return clusterId;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public String getHost() {
		return host;
	}

	public String getName() {
		return name;
	}

	public Integer getEsTransportTcpPort() {
		return esTransportTcpPort;
	}

	public Integer getEsNetworkPort() {
		return esNetworkPort;
	}

	public Integer getEsHttpPort() {
		return esHttpPort;
	}

	public Integer getCachePort() {
		return cachePort;
	}

	public Date getLastHeartBeat() {
		return new Date(lastHeartBeat);
	}

	public String getKey() {
	    return this.key;
	}

	public String getLicenseSerial() {
		return licenseSerial;
	}

	/**
	 * Creates builder to build {@link Server}.
	 * @return created builder
	 */
	@Generated("SparkTools")
	public static Builder builder() {
		return new Builder();
	}
	
	/**
	 * Creates builder to build {@link Server}.
	 * @return created builder
	 */
	@Generated("SparkTools")
	public static Builder builder(final Server server) {
		return new Builder(server);
	}
	/**
	 * Builder to build {@link Server}.
	 */
	@Generated("SparkTools")
	public static final class Builder {
		private String serverId;
		private String clusterId;
		private String ipAddress;
		private String host;
		private String name;
		private Integer esTransportTcpPort;
		private Integer esNetworkPort;
		private Integer esHttpPort;
		private Integer cachePort;
		private long lastHeartBeat;
		private String key;
		private String licenseSerial;
		private long startupTime;
		
		public Builder withServer(final Server server){
			this.cachePort = server.cachePort;
			this.clusterId = server.clusterId;
			this.esHttpPort = server.esHttpPort;
			this.esNetworkPort = server.esNetworkPort;
			this.esTransportTcpPort = server.esTransportTcpPort;
			this.host = server.host;
			this.ipAddress = server.ipAddress;
			this.key = server.key;
			this.lastHeartBeat = server.lastHeartBeat;
			this.licenseSerial = server.licenseSerial;
			this.name = server.name;
			this.serverId = server.serverId;
			this.startupTime=server.startupTime;
			return this;

		}
		

		public Builder(final Server server){
			withServer(server); 
		}
		private Builder() {
		}

		public Builder withServerId(String serverId) {
			this.serverId = serverId;
			return this;
		}

		public Builder withClusterId(String clusterId) {
			this.clusterId = clusterId;
			return this;
		}

		public Builder withIpAddress(String ipAddress) {
			this.ipAddress = ipAddress;
			return this;
		}

		public Builder withHost(String host) {
			this.host = host;
			return this;
		}
		
        public Builder withStartupTime(long startupTime) {
            this.startupTime = startupTime;
            return this;
        }		
		
		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withEsTransportTcpPort(Integer esTransportTcpPort) {
			this.esTransportTcpPort = esTransportTcpPort;
			return this;
		}

		public Builder withEsNetworkPort(Integer esNetworkPort) {
			this.esNetworkPort = esNetworkPort;
			return this;
		}

		public Builder withEsHttpPort(Integer esHttpPort) {
			this.esHttpPort = esHttpPort;
			return this;
		}

		public Builder withCachePort(Integer cachePort) {
			this.cachePort = cachePort;
			return this;
		}

		public Builder withLastHeartBeat(long lastHeartBeat) {
			this.lastHeartBeat = lastHeartBeat;
			return this;
		}

		public Builder withKey(String key) {
			this.key = key;
			return this;
		}

		public Builder withLicenseSerial(String licenseSerial) {
			this.licenseSerial = licenseSerial;
			return this;
		}

		public Server build() {
			return new Server(this);
		}
	}

}
