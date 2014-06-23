package lanpeerscanner.scan;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class AddressesQueueStringTokenizer implements AddressesQueue {

	private ArrayList<String> singleAdresses = new ArrayList<String>();
	
	private ArrayList<String> ipRanges = new ArrayList<String>();
	private String currentIpCurrentRange = "1.1.1.1";
	private String lastIpCurrentRange = "1.1.1.1";
	
	public AddressesQueueStringTokenizer(String addresses) {
		StringTokenizer stringTokenizer = new StringTokenizer(addresses, ";");
	     while (stringTokenizer.hasMoreTokens()) {
	    	 String token = stringTokenizer.nextToken();
	         if (isIP(token)) {
	        	 singleAdresses.add(token);
	         }
	         else if (isShortIPRange(token)) {
	        	 ipRanges.add(token);
	         }
	         else if (isFullIPRange(token)) {
	        	 ipRanges.add(token);
	         }
	         else if (isDomainName(token)) {
	        	 singleAdresses.add(token);
	         }
	     }
	}
	
	public String getNextAdress() {
		
		//first we  add the single addresses
		if (!this.singleAdresses.isEmpty()) {
			return singleAdresses.remove(0);
		}
		
		//then we add the ip ranges
		if (inf(this.currentIpCurrentRange,this.lastIpCurrentRange)) { //the current ip-range is not over
			this.currentIpCurrentRange = nextIp(this.currentIpCurrentRange);
			return currentIpCurrentRange;
		}
		else if (!this.ipRanges.isEmpty()) { //we update the ip range if there are more
			String ipRange = this.ipRanges.remove(0);
			this.currentIpCurrentRange = extractFirstIp(ipRange);
			this.lastIpCurrentRange = extractLastIp(ipRange);
			return currentIpCurrentRange;						
		}

		
		return null;
	}

	public synchronized Boolean isEmpty() {
		return this.singleAdresses.isEmpty() && //no more single adresses
				!inf(this.currentIpCurrentRange,this.lastIpCurrentRange) && //current range over
				this.ipRanges.isEmpty();  //no more ip ranges
	}
	
	public static final String ipRegHex = "((25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9])\\." +
										   "(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\." +
										   "(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\." +
										   "(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9]))";

	
	public static Boolean isDomainName(String string) {
		return string.matches("^[a-zA-Z0-9]+([a-zA-Z0-9\\-\\.]+)?\\.([a-zA-Z0-9]{2,5})$");
	}
	
	public static Boolean isIP(String string) {
		return string.matches("^"+ipRegHex+"$");		
	}
	
	public static Boolean isFullIPRange(String string) {
		return string.matches("^"+ipRegHex + "-" + ipRegHex + "$");
	}
	
	public static Boolean isShortIPRange(String string) {
		return string.matches("^"+ipRegHex + "-" + "(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])" + "$");
	}
	
	
	public static String nextIp(String ip) {
		
		if (ip.equals("255.255.255.255")) {
			return null;
		}

		StringTokenizer stringTokenizer = new StringTokenizer(ip,".");
		Integer[] ipTokens = new Integer[4];
		for (int i = 0; i<4;i++) {
			ipTokens[i]=new Integer(stringTokenizer.nextToken());
		}		
		
		for (int i = ipTokens.length-1; i>=0;i--) {
			if (ipTokens[i]<255) {
				ipTokens[i] = new Integer(ipTokens[i] + 1);
				break;
			}
			else if (ipTokens[i]==255) {
				ipTokens[i] = new Integer(0);
			}
		}
		
		String newIp = new String(	ipTokens[0]+"."+
									ipTokens[1]+"."+
									ipTokens[2]+"."+
									ipTokens[3]
								);
		return newIp;
	}

	public static Boolean inf(String ip1, String ip2) {

		StringTokenizer ip1Tokenizer = new StringTokenizer(ip1,".");
		StringTokenizer ip2Tokenizer = new StringTokenizer(ip2,".");
		
		int nbDigits = ip1Tokenizer.countTokens();
		
		for (int i = 0; i<nbDigits; i++) {
			if (  new Integer(ip1Tokenizer.nextToken())
					< new Integer(ip2Tokenizer.nextToken())   ) {
				return true;
			}
		}
	
		return false;
	}
	
	public static String extractFirstIp(String ipRange) {
		return ipRange.substring(0, ipRange.indexOf("-"));
	}
	
	public static String extractLastIp(String ipRange) {
		if (isFullIPRange(ipRange)) {
			return ipRange.substring(ipRange.indexOf("-")+1);
		}
		else if (isShortIPRange(ipRange)) {
			//we search the beginning of the third digit
			int index = ipRange.indexOf(".", 0);//first "."
			index = ipRange.indexOf(".", index+1);//second "."
			index = ipRange.indexOf(".", index+1);//third "."
			
			return ipRange.substring(0, index) + //3 first numbers
					"." + //"."
					ipRange.substring(ipRange.indexOf("-")+1);
		}
		
		return null;
	}

}
