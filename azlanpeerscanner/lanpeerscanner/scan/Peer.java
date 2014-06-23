package lanpeerscanner.scan;

import java.net.InetAddress;

public class Peer {
	
	private InetAddress address;
	private Integer tcpPort;
	private Integer udpPort;
		
    public Peer(InetAddress address, Integer tcpPort, Integer udpPort)
    {
        this.address = address;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
    }

	public String getIp() {
		return address.getHostAddress();
	}

	public InetAddress getInetAdress() {
		return address;
	}
	
	public Integer getTcpPort() {
		return tcpPort;
	}

	public Integer getUdpPort() {
		return udpPort;
	}
	
    public String toString()
    {
        return (new StringBuilder(String.valueOf(getIp()))).append(":").append(getTcpPort()).append("|").append(getUdpPort()).toString();
    }
}
