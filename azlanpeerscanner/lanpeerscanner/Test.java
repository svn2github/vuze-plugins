package lanpeerscanner;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class Test {

	private ArrayList<String> regExpressions = new ArrayList<String>();
	private String currentIpCurrentRegEx =null;
	private String currentRegEx;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String currentIp = "10.201.0.0";
		String regHex = "^10\\.201\\.[1-3][0-1]\\.[1-2]?[0-9]$";
		while (currentIp!=null) {
			System.out.println(currentIp);
			currentIp = getNextIpMatchingRegEx(currentIp , regHex);
		}

	}
	
	public String getNextAdress() {
		
		
		//if necessary we load a regular expression
		if (this.currentIpCurrentRegEx==null) { 
			if (!this.regExpressions.isEmpty()) {
				this.currentRegEx = this.regExpressions.remove(0);
				this.currentIpCurrentRegEx = "1.0.0.0";				
			}
		}
		
		if (this.currentIpCurrentRegEx!=null) { //the current regular expression is not over
			this.currentIpCurrentRegEx=getNextIpMatchingRegEx(this.currentIpCurrentRegEx, this.currentRegEx);
			if (this.currentIpCurrentRegEx!=null) {
				return this.currentIpCurrentRegEx;
			}
			else {
				this.currentIpCurrentRegEx = null;
				return getNextAdress();
			}
		}

		
		return null;
	}
	
	public static String getNextIpMatchingRegEx(String currentIp, String regularExpression) {

		String nextIpMatching = nextIp(currentIp);
		
		//we look for the next ip that matches with the regular expression
		while (nextIpMatching!=null && !nextIpMatching.matches(regularExpression)) {
			nextIpMatching=nextIp(nextIpMatching);
		}		
		
		//if we have found one we return it
		if (nextIpMatching!=null) {
			return nextIpMatching;
		}

		return null;

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
}
