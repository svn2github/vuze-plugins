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

/** One node in the binary bit tree
 * 
 * @author TuxPaper
 * @created Oct 20, 2005
 *
 */
class GeoIP_TreeNode
{
	long leftValue = -1;

	long rightValue = -1;

	private long lIndex = -1;

	private GeoIP_TreeNode right;

	private GeoIP_TreeNode left;

	/**
	 * Initialize
	 * 
	 * @param tree owner
	 */
	public GeoIP_TreeNode(GeoIP_Tree tree) {
		this.lIndex = tree.lNodeCounter++;
	}

	/**
	 * Get left node, create if not present
	 * 
	 * @param tree owner
	 * @return left node
	 */
	public GeoIP_TreeNode getCreateLeft(GeoIP_Tree tree) {
		if (left == null) {
			left = new GeoIP_TreeNode(tree);
		}

		return left;
	}

	/**
	 * Get right node, create if not present
	 * 
	 * @param tree owner
	 * @return right node
	 */
	public GeoIP_TreeNode getCreateRight(GeoIP_Tree tree) {
		if (right == null) {
			right = new GeoIP_TreeNode(tree);
		}

		return right;
	}

	/**
	 * Get left node
	 * 
	 * @return left node, null if not present
	 */
	public GeoIP_TreeNode getLeft() {
		return left;
	}

	/**
	 * Get right node
	 * 
	 * @return right node, null if not present
	 */
	public GeoIP_TreeNode getRight() {
		return right;
	}

	/**
	 * Get index of node in tree
	 * 
	 * @return index
	 */
	public long getIndex() {
		return lIndex;
	}

	public long getLeftValue() {
		return leftValue;
	}

	public void setLeftValue(long leftValue) {
		this.leftValue = leftValue;
	}

	public long getRightValue() {
		return rightValue;
	}

	public void setRightValue(long rightValue) {
		this.rightValue = rightValue;
	}
}