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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Binary bit tree for storing range of long against data (byte)
 * Output formatted to fit GeoIP.dat format.  See the APIs for most languages
 * at http://www.maxmind.com/app/api
 * 
 * This is pretty close to a Patricia trie
 * 
 * @author TuxPaper
 * @created Oct 20, 2005
 *
 */
public class GeoIP_Tree
{

	private static boolean bDebug = false;

	/**
	 * Used by TreeNode to set index 
	 */
	public long lNodeCounter = 0;

	private GeoIP_TreeNode firstNode;

	/**
	 * Keep only one byte array so we don't have to recreate it all the time
	 */
	private byte[] bytes = new byte[3];

	/**
	 * Initialize
	 *
	 */
	public GeoIP_Tree() {
		lNodeCounter = 0;
		firstNode = new GeoIP_TreeNode(this);
	}

	/**
	 * Puts the 3 lower bytes of a long into a byte array
	 * 
	 * @param out store bytes in this array
	 * @param value take 3 lower bytes from this long
	 * @return the same byte array
	 */
	private byte[] toBytes(byte[] out, long value) {
		out[0] = (byte) (value & 0xff);
		out[1] = (byte) ((value >> 8) & 0xff);
		out[2] = (byte) ((value >> 16) & 0xff);
		return out;
	}

	String sIndent = "";

	/**
	 * Adds a range of numbers to the specified node, at the specified bitlevel
	 * the range must already have bits > iBitLevel masked off
	 * 
	 * @param node node to add to
	 * @param iBitLevel what bit level we are on.  Start at 31 and go down
	 * @param lStart start of range (inclusive)
	 * @param lEnd end of range (inclusive)
	 * @param data
	 */
	public void addRange(GeoIP_TreeNode node, int iBitLevel, long lStart,
			long lEnd, int data)
	{
		if (iBitLevel < 0) {
			System.err.println("ERR: " + sIndent + "Level " + iBitLevel + ";IP "
					+ binary32Str(lStart) + " - " + binary32Str(lEnd) + ": " + data);
			return;
		}

		// check if range is split accross bit level
		long lSplitPoint = (1L << iBitLevel);

		if (bDebug)
			System.out.println(sIndent + "Level " + iBitLevel + ";IP "
					+ binary32Str(lStart) + " - " + binary32Str(lEnd) + ": " + data
					+ "; " + binary32Str(lSplitPoint));

		if (lStart < lSplitPoint && lEnd >= lSplitPoint) {
			if (bDebug) {
				sIndent = sIndent + "  ";
				System.out.println(sIndent + ">> Split:Left");
			}

			// split!
			addRange(node, iBitLevel, lStart, lSplitPoint - 1, data);
			if (bDebug) {
				System.out.println(sIndent + "|| Split:Right");
			}
			addRange(node, iBitLevel, lSplitPoint, lEnd, data);
			if (bDebug) {
				sIndent = sIndent.substring(2);
				System.out.println(sIndent + "<< Split");
			}
			return;
		}

		// check if we fill all of left
		if (lStart == 0 && lEnd == lSplitPoint - 1) {
			if (bDebug)
				System.out.println(sIndent + "Fill Left");

			node.setLeftValue(data);
			return;
		}

		long lFillMask = 0xFFFFFFFFL >> (32 - iBitLevel);

		// check if we fill all of right
		if (lStart == lSplitPoint && lEnd == lSplitPoint + lFillMask) {
			if (bDebug)
				System.out.println(sIndent + "Fill Right");

			node.setRightValue(data);
			return;
		}

		if (lEnd < lSplitPoint) {
			if (bDebug)
				System.out.println(sIndent + "Node Left");

			// create a left node and call it to handle the range
			GeoIP_TreeNode newNode = node.getCreateLeft(this);
			// mask off the old range
			addRange(newNode, --iBitLevel, lStart & lFillMask, lEnd & lFillMask, data);
			return;
		}

		if (lStart >= lSplitPoint) {
			if (bDebug)
				System.out.println(sIndent + "Node Right");

			// create a left node and call it to handle the range
			GeoIP_TreeNode newNode = node.getCreateRight(this);
			// mask off the old range
			addRange(newNode, --iBitLevel, lStart & lFillMask, lEnd & lFillMask, data);
			return;
		}

		System.err.println("Shouldn't get here?");
	}

	/**
	 * Adds data to a range of numbers (inclusive) to the tree
	 *  
	 * @param lStart start range
	 * @param lEnd end range
	 * @param data number to store
	 */
	public void addRange(long lStart, long lEnd, int data) {
		addRange(firstNode, 31, lStart, lEnd, data);
	}

	private void outputBinary(OutputStream os, GeoIP_TreeNode node)
		throws IOException
	{

		GeoIP_TreeNode left = node.getLeft();
		GeoIP_TreeNode right = node.getRight();

		os.write(toBytes(bytes, (left == null) ? (node.getLeftValue() | 0xFFFF00L)
				: left.getIndex()));
		os.write(toBytes(bytes, (right == null)
				? (node.getRightValue() | 0xFFFF00L) : right.getIndex()));

		if (left != null)
			outputBinary(os, left);

		if (right != null)
			outputBinary(os, right);

	}

	public static String hex32Str(long l) {
		return Long.toHexString(l + (1L << 32)).substring(1);
	}

	public static String binary32Str(long l) {
		String s = Long.toBinaryString(l + (1L << 32)).substring(1);
		return s.substring(0, 4) + " " + s.substring(4, 8) + " "
				+ s.substring(8, 12) + " " + s.substring(12, 16) + ":" + hex32Str(l);
	}

	/**
	 * Write a binary tree to the outputstream
	 * 
	 * @param os  Open output stream
	 * @throws IOException
	 */
	public void outputBinary(OutputStream os)
		throws IOException
	{
		if (bDebug)
			System.out.println();
		outputBinary(os, firstNode);
		os.write("\00\00\00Converted by csvtodat".getBytes());
		if (bDebug)
			System.out.println();
	}

}