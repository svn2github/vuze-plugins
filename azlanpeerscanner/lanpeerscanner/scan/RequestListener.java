package lanpeerscanner.scan;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import lanpeerscanner.Plugin;

import org.gudy.azureus2.plugins.logging.LoggerChannel;

public class RequestListener implements Runnable {

    private static final String requestMask = "^([0-9]+):([0-9]+)$";
    private DatagramSocket socket;
    private Integer listeningPort;
    private byte data[];
    private PeerHandler peerHandler;

    public RequestListener(Integer listeningPort, Integer tcpPort, Integer udpPort, PeerHandler peerHandler)
        throws SocketException
    {
        this.listeningPort = listeningPort;
        data = (new String((new StringBuilder()).append(tcpPort).append(":").append(udpPort).toString())).getBytes();
        socket = new DatagramSocket(this.listeningPort.intValue());
        this.peerHandler = peerHandler;
    }

    public void run()
    {
    	
        DatagramPacket receptionPacket = new DatagramPacket(new byte[512], 512);
        Plugin.logger.log(LoggerChannel.LT_INFORMATION,"Listening on port " + this.listeningPort);
        
        while (true) {
        	
        	//reception of a request
            try {
                socket.receive(receptionPacket);
            }
            catch(IOException e)
            {
                e.printStackTrace();
                continue;
            }
            
            //if the request came from this plugin and doesn't originate from this client we handle it
            String receptionString = new String(receptionPacket.getData(), 0, receptionPacket.getLength());
            if(receptionString.matches(requestMask) && !isSelfOriginated(receptionPacket.getAddress()))  {
            	
                int indexSep = receptionString.indexOf(":");
                final Peer peer = new Peer(receptionPacket.getAddress(), new Integer(receptionString.substring(0, indexSep)), new Integer(receptionString.substring(indexSep + 1)));
                Thread threadpeerHandler = new Thread() {
                    public void run()
                    {
                        peerHandler.handleFoundPeer(peer);
                    }
                };
                
                threadpeerHandler.start();
                DatagramPacket answerPacket = new DatagramPacket(data, data.length, receptionPacket.getAddress(), receptionPacket.getPort());
                try
                {
                    socket.send(answerPacket);
                }
                catch(IOException e)
                {
                	Plugin.logger.log("error answering to a request", e);
                }
            }
        }
    }
    
    public Boolean isSelfOriginated(InetAddress address) {
    	try {
			return (NetworkInterface.getByInetAddress(address)!=null);
		} catch (SocketException e) {
			return false;
		}
    }

    public void closeSocket() {
    	if (socket!=null) {
    		if (socket.isConnected()) {
    			socket.disconnect();
    		}
        	if (socket.isClosed()) {
        		socket.close();
        	}
    	}
    }
}
