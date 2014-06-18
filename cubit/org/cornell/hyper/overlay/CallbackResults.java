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

import java.util.ArrayList;
import java.util.List;
import org.apache.xmlrpc.client.TimingOutCallback;
import org.apache.xmlrpc.client.TimingOutCallback.TimeoutException;

// This class is unused
public class CallbackResults {		
	private ArrayList<KeyVal<Object, TimingOutCallback>> 	callbackList;
	private ArrayList<Object>								failedCallbacks;
	private ArrayList<KeyVal<Object, Object>>				successResults;
	
	public CallbackResults() {
		callbackList = new ArrayList<KeyVal<Object, TimingOutCallback>>();
		failedCallbacks = new ArrayList<Object>();
		successResults = new ArrayList<KeyVal<Object, Object>>();
	}
	
	public void addCallback(Object keyObj, TimingOutCallback callback) {
		callbackList.add(new KeyVal<Object, TimingOutCallback>(keyObj, callback));
	}
	
	public int numCallbacks() {
		return callbackList.size();
	}
		
	public void eval() {
		for (int i = 0; i < callbackList.size(); i++) {
			// Get the key/callback objects
			KeyVal<Object, TimingOutCallback> curKeyVal = callbackList.get(i);
			Object keyObj = curKeyVal.getKey();
			TimingOutCallback callback = curKeyVal.getVal();
	
			// Wait for the RPC to complete
			try {
				System.out.println("Success!");
				Object resObj = callback.waitForResponse();
				successResults.add(new KeyVal<Object, Object>(keyObj, resObj));
			} catch (TimeoutException e) {
				System.out.println("Failure 1.");
				failedCallbacks.add(keyObj);	
			} catch (Throwable e) {
				System.out.println("Failure 2.");
				failedCallbacks.add(keyObj);
			}
		}
	}
	
	public List<KeyVal<Object, Object>> getSuccess() {
		return successResults;
	}
	
	public List<Object> getFail() {
		return failedCallbacks;
	}
}
