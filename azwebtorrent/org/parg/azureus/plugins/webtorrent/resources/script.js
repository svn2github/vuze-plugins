/*
 * Created on Dec 18, 2015
 * Created by Paul Gardner
 * 
 * Copyright 2015 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


function getBrowserRTC () {
  if (typeof window === 'undefined') return null
  var wrtc = {
    RTCPeerConnection: window.mozRTCPeerConnection || window.RTCPeerConnection ||
      window.webkitRTCPeerConnection,
    RTCSessionDescription: window.mozRTCSessionDescription ||
      window.RTCSessionDescription || window.webkitRTCSessionDescription,
    RTCIceCandidate: window.mozRTCIceCandidate || window.RTCIceCandidate ||
      window.webkitRTCIceCandidate
  }
  if (!wrtc.RTCPeerConnection) return null
  return wrtc
}

var ws_id = "";

var allcookies = document.cookie;

var cookiearray = allcookies.split(';');

for ( var i=0;i<cookiearray.length; i++){
	
	var bits = cookiearray[i].split('=');
   
	if ( bits[0].trim() == "vuze-ws-id" ){
	   
		ws_id = bits[1].trim();
	}
}

var control_ws = new WebSocket("ws://127.0.0.1:8025/websockets/vuze?type=control" + "&id=" + ws_id );

control_ws.onmessage = 
	function( event ) 
	{
	  console.log(event.data);
	}

control_ws.onerror = 
	function( event )
	{
	  console.log(event);
	}

control_ws.onopen = 
	function( event )
	{
		var browser_rtc = getBrowserRTC();
		
		var peer_config = {"iceServers": [{"url": "stun:stun.l.google.com:19302"}]};
		
		var peer = new browser_rtc.RTCPeerConnection( peer_config );
		
		peer.onicecandidate = 
			function (event) 
			{ 
				if ( event.candidate ){
					
					control_ws.send( JSON.stringify( event.candidate.candidate ));
					
				}else{
					
					control_ws.send( JSON.stringify( {} ));
				}
			}
		
		function setLocalAndSendMessage(sessionDescription) {
			 
			peer.setLocalDescription(sessionDescription);
			 
		}
		
		peer.createDataChannel( "bleh" );
		
		var offerOptions = {};
		
		peer.createOffer(setLocalAndSendMessage, null, offerOptions );
	}


/*
var client = new WebTorrent()

var magnetUri = 'magnet:?xt=urn:btih:NKLVTP75LQFPMUYZS6P3PAZBRH2PHQ25&dn=sintel.mp4&tr=wss%3A%2F%2Ftracker.webtorrent.io&tr=wss%3A%2F%2Ftracker.btorrent.xyz&ws=https%3A%2F%2Fwebtorrent.io%2Ftorrents%2Fsintel-1024-surround.mp4&tag=kimchi'

client.add(magnetUri, function (torrent) {
  // Got torrent metadata!
  console.log('Client is downloading:', torrent.infoHash)

  torrent.files.forEach(function (file) {
    // Display the file by appending it to the DOM. Supports video, audio, images, and
    // more. Specify a container element (CSS selector or reference to DOM node).
    file.appendTo('body')
  })
})
*/
