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

function ab2str(buf) {
  return String.fromCharCode.apply(null, new Uint8Array(buf));
}

function str2ab(str) {
  var buf = new ArrayBuffer(str.length*2); // 2 bytes for each char
  var bufView = new Uint16Array(buf);
  for (var i=0, strLen=str.length; i<strLen; i++) {
    bufView[i] = str.charCodeAt(i);
  }
  return buf;
}

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

var ws_config 	= {};

var allcookies = document.cookie;

var cookiearray = allcookies.split(';');

for ( var i=0;i<cookiearray.length; i++){
	
	var bits = cookiearray[i].split('=');
   
	if ( bits[0].trim() == "vuze-ws-config" ){
	   
		ws_config = JSON.parse( bits[1].trim());
	}
}

var ws_id	= ws_config.id;

var peer_config = ws_config.peer_config;

var ws_url_prefix = ws_config.url_prefix;

var control_ws = new WebSocket( ws_url_prefix + "?type=control" + "&id=" + ws_id );

var peers = {};


function createPeer( offer_id )
{
	var browser_rtc = getBrowserRTC();
	
	var peer = new browser_rtc.RTCPeerConnection( peer_config );
		
	peer.offer_id = offer_id;
	
	addPeer( peer );
	
	return( peer );
}

function addPeer( peer )
{	
	var offer_id = peer.offer_id;
	
	peers[ offer_id ] = peer;
	
	console.log( "addPeer: " + offer_id + " -> " + Object.keys(peers).length );
}

function removePeer( peer )
{
	var offer_id = peer.offer_id;
	
	if ( peers[ offer_id ] ){
		
		peer.close();
		
		delete peers[ peer.offer_id ];
		
		console.log( "removePeer: " + offer_id + " -> " + Object.keys(peers).length );
	}
}

control_ws.onmessage = 
	function( event ) 
	{
		var x = JSON.parse( event.data );
	  
		if ( x.type == undefined ){
			
			return;
		}
		
		console.log( x );

		var	offer_id	= x.offer_id;
		var	hash		= x.info_hash;
		
		if ( x.type == 'create_offer' || x.type == 'offer' ){
		  						
			var peer = createPeer( offer_id );
			
			peer.onicecandidate = 
				function (event) 
				{ 
					if ( event.candidate ){
						
						var message = {};
						
						message.type 		= "ice_candidate";
						message.offer_id	= offer_id;
						message.candidate 	= event.candidate.candidate;
						
						control_ws.send( JSON.stringify( message ));
						
					}else{
						
						var message = {};
						
						message.type 		= "ice_candidate";
						message.offer_id	= offer_id;
						message.candidate 	= "";
						
						control_ws.send( JSON.stringify( message ));
					}
				}
			
			peer.oniceconnectionstatechange = 
				function (event) 
				{ 
					var state = peer.iceConnectionState;
					console.log( "ice_state=" + state );
					
					if ( state == 'failed' || state == 'closed' ){
						
						if ( peers[ offer_id ] ){
							
							console.log( "icestate->" + state );
							
							removePeer( peer );
						}
					}
				};
				
			createChannel( peer, offer_id, hash, x.type == 'offer' );

			function setLocalAndSendMessage(sessionDescription) {
				 
				var message = {};
				
				message.type 		= "sdp";
				message.offer_id	= offer_id;

				message.sdp = sessionDescription.sdp;
				
				control_ws.send( JSON.stringify( message ));
								
				peer.setLocalDescription(sessionDescription); 
			}
							
			if ( x.type == 'create_offer' ){
				
				var offerOptions = {};
				
				peer.createOffer(setLocalAndSendMessage, null, offerOptions );
				
			}else{
			
				var browser_rtc = getBrowserRTC();
				
				var remoteSessionDescription = new browser_rtc.RTCSessionDescription( x );
				
				console.log( "offer: remote sdp: " + remoteSessionDescription );
				
				peer.setRemoteDescription( remoteSessionDescription );
				
				var answerOptions = {};
				
				peer.createAnswer(setLocalAndSendMessage, null, answerOptions );
			}

		}else if ( x.type == 'answer' ){
	  
			var peer = peers[offer_id];
		  
			if ( peer ){
			  
				var browser_rtc = getBrowserRTC();
	
				var remoteSessionDescription = new browser_rtc.RTCSessionDescription( x );
			
				console.log( "answer: remote sdp: " + remoteSessionDescription );
			  
				peer.setRemoteDescription( remoteSessionDescription );
			}	
		}
	}

control_ws.onerror = 
	function( event )
	{
	  console.log(event);
	}

control_ws.onopen = 
	function( event )
	{
	}

function createChannel( peer, offer_id, hash, incoming )
{
	if ( incoming ){
		
		peer.ondatachannel = 
			function( event )
			{
				console.log( "ondatachannel" );
				
				setupChannel( peer, event.channel, offer_id, hash, incoming );
			}
	}else{

		var channel = peer.createDataChannel( "vuzedc." + offer_id );
		
		setupChannel( peer, channel, offer_id, hash, incoming );
	}
}

function setupChannel( peer, channel, offer_id, hash, incoming )
{	
	channel.binaryType = "arraybuffer";
	
	peer.vuzedc = channel;

	channel.onopen = function (){
		console.log("datachannel open: pid=" + peer.peerIdentity );
		
		var peer_ws = new WebSocket( ws_url_prefix + "?type=peer" + "&id=" + ws_id + "&offer_id=" + offer_id + "&hash=" + hash + "&incoming=" + incoming );

		peer_ws.binaryType = "arraybuffer";
		
		peer_ws.onmessage = 
			function( event ) 
			{
				//console.log( "got message from peer_ws" );
				
				var array_buffer = event.data;
				
				console.log( "datachannel send: " + ab2str( array_buffer ));
				
				try{
					channel.send( array_buffer );
				
				}catch( err ){
					
					peer_ws.close();
					
					channel.close();
				
					removePeer( peer );
				}
				/*
				var fr = new FileReader();
				
				fr.onload = function(){
					
					var array_buffer = this.result;
					
					console.log( "datachannel send: " + ab2str( array_buffer ));
					
					channel.send( array_buffer );
				};
				
				fr.readAsArrayBuffer( event.data );	// it's a Blob		
				*/
			};
			
		peer_ws.onerror = 
			function( event )
			{
				// console.log("peerws error");
				
				peer_ws.close();
				
				channel.close();
			
				removePeer( peer );
			}
		
		peer_ws.onclose = 
			function( event )
			{
				// console.log("peerws close");
				
				channel.close();
			
				removePeer( peer );
			}
		
		channel.onmessage = 
			function( event )
			{
				console.log( "datachannel recv: " + ab2str( event.data ));
				
				try{
					peer_ws.send( event.data );
					
				}catch( err ){
					
					peer_ws.close();
					
					channel.close();
				
					removePeer( peer );
				}
			};
	};
	
	channel.onclose = 
		function ()
		{
			// console.log("datachannel close");
		
			removePeer( peer );
		};
		
	channel.onerror = 
		function () 
		{
			// console.log("datachannel error");
		
			channel.close();
			
			removePeer( peer );
		};
}
