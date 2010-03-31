/**
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.maxmind.geoip.csvtodat;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.maxmind.geoip.LookupService;

/**
 * Convert a CSV file to a (binary bit tree) GeoIP.dat format
 * 
 * @author TuxPaper
 * @created Oct 19, 2005
 *
 */
public class CsvToDat
{

	/**
	 * Generic regex string for parsing csv files. Some formats supported:<br>
	 *
	 * "2.6.190.56","2.6.190.63","33996344","33996351","GB","United Kingdom"
	 * <br>
	 * "0","16777215","IANA","410227200","ZZ","ZZZ","RESERVED"
	 * <br>
	 * "3758096384","4294967295","US","USA","UNITED STATES"
	 * <p>
	 *
	 * Basically, any cvs that has the start end IPs in long format next to 
	 * each other, without any other 2 pairs of longs before it, plus the
	 * first 2 character string after the IP pair.
	 * <p>
	 * .*\"([0-9]+)\",\"([0-9]+)\".*\"([A-Z][A-Z])\".*
	 */
	public final static String MATCHSTR = ".*\\\"([0-9]+)\\\",\\\"([0-9]+)\\\".*\\\"([A-Z][A-Z])\\\".*";

	private final static HashMap mapCountryCodetoIndex = new HashMap(250);

	private final static String[] countryCode = LookupService.getCountryCodes();

	static {
		for (int i = 0; i < countryCode.length; i++) {
			mapCountryCodetoIndex.put(countryCode[i], new Integer(i));
		}
	}

	/**
	 * Just a test
	 * 
	 * @param os
	 * @return tree
	 * @throws IOException
	 */
	public GeoIP_Tree test(OutputStream os)
		throws IOException
	{
		GeoIP_Tree tree = new GeoIP_Tree();

		tree.addRange(0x00000000, 0x206BE37,
				((Integer) mapCountryCodetoIndex.get("--")).intValue());
		tree.addRange(0x206BE38, 0x206BE3F,
				((Integer) mapCountryCodetoIndex.get("GB")).intValue());

		tree.outputBinary(os);
		os.close();
		return tree;
	}

	/**
	 * Convert a csv file to GeoIP.dat format
	 * 
	 * @param matchString RegEx string for matching 2 IPs (long) and country 
	 *                     code (2 chars).  See generic {@link #MATCHSTR} 
	 * @param reader csv input
	 * @param os write to
	 * @return number of groups added
	 * @throws IOException
	 */
	public long csvToDat(String matchString, Reader reader, OutputStream os)
		throws IOException
	{
		GeoIP_Tree tree = new GeoIP_Tree();

		BufferedReader br;

		if (reader instanceof BufferedReader)
			br = (BufferedReader) reader;
		else
			br = new BufferedReader(reader);

		Pattern p = Pattern.compile(matchString);
		long lLastIPEnd = -1;
		long count = 0;

		//System.out.println("Reading..");
		String sLine = br.readLine();

		while (sLine != null) {
			if (sLine == "" || sLine.startsWith("#")) {
				sLine = br.readLine();
				continue;
			}

			Matcher m = p.matcher(sLine);
			if (m.matches()) {
				long lIPStart = Long.valueOf(m.group(1)).longValue();
				long lIPEnd = Long.valueOf(m.group(2)).longValue();
				String sCountry = m.group(3);

				if (sCountry.equals("UK"))
					sCountry = "GB";

				if (lIPStart != lLastIPEnd + 1) {
					tree.addRange(lLastIPEnd + 1, lIPStart - 1, 0);
					count++;
				}

				Integer countryCodeIndex = (Integer) mapCountryCodetoIndex.get(sCountry);
				if (countryCodeIndex == null) {
					System.out.println(sCountry + " is unknown");
					countryCodeIndex = new Integer(0);
				}
				tree.addRange(lIPStart, lIPEnd, countryCodeIndex.intValue());
				lLastIPEnd = lIPEnd;

				count++;
			}

			sLine = br.readLine();
		}

		long lIPStart = lLastIPEnd + 1;
		long lIPEnd = 0xFFFFFFFFL;
		if (lIPStart < lIPEnd) {
			tree.addRange(lIPStart, lIPEnd, 0);
			count++;
		}
		//System.out.println("" + count + " entries parsed correctly");

		tree.outputBinary(os);
		os.close();

		return count;
	}

	/**
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		final boolean MAKE = false;
		//final String CSVFILE = "E:\\AzDev\\GeoIPCountryWhois.csv";
		final String CSVFILE = "E:\\AzDev\\IPtoCountry.csv";
		final String OUTFILE = "D:\\Program Files\\Azureus\\Plugins\\CountryLocator\\GeoIP.dat";

		System.out.println("Hi.  " + countryCode.length + " Country Codes");

		CsvToDat instance = new CsvToDat();

		try {
			File outFile = new File(OUTFILE);
			if (MAKE) {
				File file = new File(CSVFILE);
				OutputStream os;
				os = new FileOutputStream(outFile);
				BufferedReader br = new BufferedReader(new FileReader(file));

				instance.csvToDat(CsvToDat.MATCHSTR, br, os);
				//Tree tree = instance.test2(os);
			}

			System.out.println("Done");

			LookupService ls = new LookupService(outFile);

			String[] testIPs = { "69.17.93.101", // US
					"255.17.93.101", // NA 
					"62.72.229.9", //fi
					"128.0.0.0", // ZZ/US
					"193.216.110.31", // NO
					"62.58.189.0", // Belgium
					"81.177.255.254",
					"81.177.255.255", // Russia 
					"81.178.0.0",
					"81.178.0.1", // UK
					"81.178.161.4",// UK
					"81.179.255.254",
					"81.179.255.255", // UK 
					"81.180.0.0", // Romania
					"220.99.129.74" //JA
			};

			for (int i = 0; i < testIPs.length; i++) {
				System.out.print(testIPs[i] + " = ");
				System.out.println(" " + ls.getCountry(testIPs[i]).getName());
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

	}

}
