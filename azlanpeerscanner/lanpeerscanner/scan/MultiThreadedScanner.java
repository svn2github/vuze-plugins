package lanpeerscanner.scan;

	
	
import java.net.SocketException;
import java.util.Date;
import java.util.Map;

import lanpeerscanner.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;


public class MultiThreadedScanner {

	private PluginInterface pluginInterface;
	
    public MultiThreadedScanner(PluginInterface pluginInterface) {
    	this.pluginInterface = pluginInterface;
    }

    public Map<String, Peer> scan(int nbThreads, AddressesQueue addressesToCheck, int requestPort, int udpRequestTimeout, long fullScanTimeout, Integer localTcpPort, Integer localUdpPort)
    {
        SyncPeerMap addressesOk = new SyncPeerMap();
        SingleScanner scanners[] = new SingleScanner[nbThreads];
        for(int i = 0; i < scanners.length; i++)
        {
            try
            {
                scanners[i] = new SingleScanner((new Integer(i)).intValue(), addressesToCheck, addressesOk, requestPort, udpRequestTimeout, localTcpPort, localUdpPort);
            }
            catch(SocketException e)
            {
                Plugin.logger.log("Error creating the single scanner", e);
            }
        }

        for(int i = 0; i < scanners.length; i++)
        {
            //scanners[i].start();
        	this.pluginInterface.getUtilities().createThread("single_scaner_id_" + i, scanners[i]);
        }

        Date launchDate = new Date();
        Long sleepFinishCheckTime = new Long(5 * udpRequestTimeout);
        while(!addressesToCheck.isEmpty().booleanValue() && (new Date()).getTime() - launchDate.getTime() < fullScanTimeout) 
        {
            try
            {
                Thread.sleep(sleepFinishCheckTime.longValue());
            }
            catch(InterruptedException e)
            {
            	Plugin.logger.log("sleep interrupted exception",e);
            }
        }
        for(int i = 0; i < scanners.length; i++)
        {
            scanners[i].interrupt();
        }

        return addressesOk.getPeers();
    }
    
    
}
