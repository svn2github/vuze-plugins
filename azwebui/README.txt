From version 1.3.2 the plugin configuration has moved from the plugin.properties file to the SWT plugin configuration tab.

The old properties description is included here for information:

# the port that Azureus will use to support the remote ui

port=6883

# protocol used. Can be http or https (the latter requires Azureus config for SSL, see FAQ)

protocol=http

# homepage is the default page loaded when browsing to <protocol>://<host>:<port>

homepage=index.html

# rootdir gives the root dir for serving HTML. By default this is the plugin dir

rootdir=

# rootresource is used when HTML and other resources are packaged in the plugin JAR file
# and as such gives the path within the JAR that identifies the resourceroot

rootresource=

# mode can be missing (implies "full"), "full" or "view"
# full = all operations available
# view = view only (but can update refresh frequency)

mode=full

# access can be 
#   "local" 	- meaning only the local machine can connect
#   "all"   	- unrestricted access
#   IP		- e.g. 192.168.0.2                  one IP only
#   IP1-IP2	- e.g. 192.168.0.1-192.168.0.255    inclusive range of IPs

access=all


