/******************************************************************************
Cubit distribution
Copyright (C) 2008 Bernard Wong

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

The copyright owner can be contacted by e-mail at bwong@cs.cornell.edu
*******************************************************************************/

package org.cornell.hyper.overlay;

// This (public-domain) code fragment taken from
// http://en.wikibooks.org/wiki/Algorithm_implementation/Strings/Levenshtein_distance#Java

public class EditDistance {
	private static int min3(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}	
	
	public static int computeEditDistance(String str1, String str2) {
		int[][] distance = new int[str1.length()+1][str2.length()+1];
 
		for (int i = 0; i <= str1.length(); i++)
			distance[i][0] = i;
		for (int j = 0; j <= str2.length(); j++)
			distance[0][j] = j;
 
		for (int i = 1; i <= str1.length(); i++)
			for (int j = 1; j <= str2.length(); j++)
				distance[i][j]= min3(
					distance[i-1][j]+1, distance[i][j-1]+1, 
					distance[i-1][j-1]+((str1.charAt(i-1) == str2.charAt(j-1))?0:1));

		return distance[str1.length()][str2.length()];
	}
}
