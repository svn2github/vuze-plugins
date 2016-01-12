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

var trace_on = false;

function trace( str ){
	if ( trace_on ){
		console.log( str );
	}
}

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

function debug(){}

function now() {
    return new Date().getTime()
}

var graph = window.graph = TorrentGraph('#svgWrap')

graph.add({ id: 'You', me: true });


var timer = 
	setInterval(
		function()
		{
			var now = new Date().getTime();
			
			for ( var offer_id in peers ){
				
			    if( peers.hasOwnProperty(offer_id)){
			    	
			    	var peer = peers[offer_id];
			    	
			    	var age = now - peer.create_time;
			    	
			    	if ( age > 2*60*1000 ){
			    	
			    		var ice_state = peer.iceConnectionState;

			    		if ( ice_state == 'connected' || ice_state == 'completed' || ice_state == 'disconnected' ){
			    			
			    			// looks ok
			    			
			    		}else{
			    			
			    			removePeer( peer );
			    		}
			    	}
			    	
			    	trace( offer_id + ": age=" + age + ", state=" + ice_state );
			    } 
			  }  
		}, 15000 );

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
	
	peer.create_time = new Date().getTime();

	peers[ offer_id ] = peer;
	
	trace( "addPeer: " + offer_id + " -> " + Object.keys(peers).length );
}

function havePeer( peer ){
	var offer_id = peer.offer_id;
	
	if ( peers[ offer_id ] ){
		
		return( true );
		
	}else{
		
		return( false );
	}
}

function removePeer( peer )
{
	var offer_id = peer.offer_id;
	
	if ( peers[ offer_id ] ){
		
		var channel = peer.vuzedc;
		
		if ( channel ){
			
			try{
				channel.close();
				
			}catch( err ){				
			}
		}
		
		var ws = peer.vuzews;
		
		if ( ws ){
			
			graph.disconnect( 'You', offer_id );
			
			graph.remove( offer_id );
			
			if ( ws.readyState != WebSocket.CONNECTING ){
				try{
					ws.close();
					
				}catch( err ){	
				}
			}
		}
		
		try{
			peer.close();
		
		}catch( err ){
		}
		
		delete peers[ peer.offer_id ];
		
		trace( "removePeer: " + offer_id + " -> " + Object.keys(peers).length );
	}
}

control_ws.onmessage = 
	function( event ) 
	{
		var x = JSON.parse( event.data );
	  
		if ( x.type == undefined ){
			
			return;
		}
		
		trace( x );

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
					trace( "ice_state=" + state );
					
					if ( state == 'failed' || state == 'closed' ){
						
						if ( peers[ offer_id ] ){
							
							trace( "icestate->" + state );
							
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
				
				trace( "offer: remote sdp: " + remoteSessionDescription );
				
				peer.setRemoteDescription( remoteSessionDescription );
				
				var answerOptions = {};
				
				peer.createAnswer(setLocalAndSendMessage, null, answerOptions );
			}

		}else if ( x.type == 'answer' ){
	  
			var peer = peers[offer_id];
		  
			if ( peer ){
			  
				var browser_rtc = getBrowserRTC();
	
				var remoteSessionDescription = new browser_rtc.RTCSessionDescription( x );
			
				trace( "answer: remote sdp: " + remoteSessionDescription );
			  
				peer.setRemoteDescription( remoteSessionDescription );
			}	
		}
	}

control_ws.onerror = 
	function( event )
	{
	  trace(event);
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
				trace( "ondatachannel" );
				
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

	peer.pending_messages = [];

	channel.onopen = function (){
		trace("datachannel open" );
		
		peer.getStats(
			function( stats )
			{
				var remote_ip = "";

				stats.result().forEach(
					function( result ){
						var item = {}
						result.names().forEach(
							function (name) {
								item[name] = result.stat(name)
							});
						
						item.id = result.id
						item.type = result.type
						item.timestamp = result.timestamp
										
						if ( item.type == 'remotecandidate' ){
													
							remote_ip += (remote_ip==""?"":",") + item.ipAddress;
						}
					});
				
				setupChannelWS( peer, channel, offer_id, hash, incoming, remote_ip )	
				
				//trace( items );
			}, null, 
			function( stats )
			{
				setupChannelWS( peer, channel, offer_id, hash, incoming, "" )
			});
		};
		
		// buffer any messages we receive while waiting for stats
			
	channel.onmessage = 
		function( event )
		{
			trace( "pending message1" );
		
			peer.pending_messages.push( event.data );
		};
	
	channel.onclose = 
		function ()
		{
			// trace("datachannel close");
		
			removePeer( peer );
		};
		
	channel.onerror = 
		function () 
		{
			// trace("datachannel error");
					
			removePeer( peer );
		};
}
		
function setupChannelWS( peer, channel, offer_id, hash, incoming, remote_ip )
{
	var peer_ws = new WebSocket( ws_url_prefix + "?type=peer" + "&id=" + ws_id + "&offer_id=" + offer_id + "&hash=" + hash + "&incoming=" + incoming + "&remote=" + remote_ip );

	peer.vuzews = peer_ws;

	graph.add({ id: peer.offer_id, ip: remote_ip });
	
	graph.connect('You', peer.offer_id)

	peer_ws.binaryType = "arraybuffer";
	
	peer_ws.onmessage = 
		function( event ) 
		{
			//trace( "got message from peer_ws" );
			
			var array_buffer = event.data;
			
			if ( trace_on ){
				trace( "datachannel send: " + ab2str( array_buffer ));
			}
			
			try{
				channel.send( array_buffer );
			
			}catch( err ){
								
				removePeer( peer );
			}
			/*
			var fr = new FileReader();
			
			fr.onload = function(){
				
				var array_buffer = this.result;
				
				trace( "datachannel send: " + ab2str( array_buffer ));
				
				channel.send( array_buffer );
			};
			
			fr.readAsArrayBuffer( event.data );	// it's a Blob		
			*/
		};
		
	peer_ws.onerror = 
		function( event )
		{
			// trace("peerws error");
						
			removePeer( peer );
		}
	
	peer_ws.onclose = 
		function( event )
		{
			// trace("peerws close");
						
			removePeer( peer );
		}
	
	peer_ws.onopen = 
		function( event )
		{
			if ( !havePeer( peer )){
				
				try{
					peer_ws.close();
					
				}catch( err ){
				}
			}		
			
			if ( peer.pending_messages.length > 0 ){

				try{
					for	( i=0; i<peer.pending_messages.length; i++ ){
	
						trace( "Sending pending message" );
						
						peer_ws.send( peer.pending_messages[i] );
					}
				}catch( err ){
					
					removePeer( peer );
				}
				
				peer.pending_messages = [];
			}
		}
	
	channel.onmessage = 
		function( event )
		{
			if ( trace_on ){
				trace( "datachannel recv: " + ab2str( event.data ));
			}
			
			try{
				var state = peer_ws.readyState;
				
				if ( state == WebSocket.CONNECTING  ){
					
					trace( "pending message2" );
					
					peer.pending_messages.push( event.data );
					
				}else if ( state == WebSocket.OPEN ){
					
					peer_ws.send( event.data );
					
				}else{
					
					removePeer( peer );
				}
			}catch( err ){
							
				removePeer( peer );
			}
		};
}
