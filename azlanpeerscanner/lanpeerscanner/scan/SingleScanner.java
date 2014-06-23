package lanpeerscanner.scan;

import java.io.IOException;
import java.net.*;


public class SingleScanner extends Thread
{

    private static final String responseMask = "^([0-9]+):([0-9]+)$";
    private AddressesQueue addressesToCheck;
    private SyncPeerMap addressesOk;
    private int port;
    private String message;
    private int timeout;
    private DatagramSocket socket;
    private int id;

    public SingleScanner(int id, AddressesQueue addressesToCheck, SyncPeerMap addressesOk, int port, int timeout, Integer localTcpPort, Integer localUdpPort)
        throws SocketException
    {
        this.addressesToCheck = addressesToCheck;
        this.addressesOk = addressesOk;
        this.port = port;
        message = (new StringBuilder()).append(localTcpPort).append(":").append(localUdpPort).toString();
        this.timeout = timeout;
        this.id = id;
        socket = new DatagramSocket();
    }

    public void run()
    {

        Peer peer;
        for(String address = null; (address = addressesToCheck.getNextAdress()) != null;)
        {
            try
            {
                if((peer = checkPeer(new InetSocketAddress(address, port))) != null)
                {
                    addressesOk.addPeer(peer);
                }
            }
            catch(NoRouteToHostException e)
            {
                interrupt();
                return;
            }
            catch(IOException ioexception) { }
        }

    }

    public Peer checkPeer(InetSocketAddress address) throws IOException  {
    	
            String responseString;
            int indexSep;
            try
            {
            	//we send an udp request to the peer
                socket.connect(address);
                socket.setSoTimeout(timeout);
                byte data[] = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, address);
                socket.send(packet);
                
                //we wait for an answer
                byte receptionBuffer[] = new byte[1000];
                DatagramPacket response = new DatagramPacket(receptionBuffer, receptionBuffer.length, address);
                socket.receive(response);
                
                //we check if the answer is ok, if yes we return a new Peer
                responseString = new String(receptionBuffer, 0, response.getLength());
                System.out.println((new StringBuilder("resp : ")).append(responseString).toString());
                if(responseString.matches(responseMask))
                {
                	indexSep = responseString.indexOf(":");
                    return new Peer(address.getAddress(), new Integer(responseString.substring(0, indexSep)), new Integer(responseString.substring(indexSep + 1)));
                }
            }
            catch(SocketTimeoutException e)
            {
                socket.disconnect();
                return null;
            }
            
            return null;
    }
        
}