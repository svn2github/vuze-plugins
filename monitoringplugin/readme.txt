Copy these jar files into a subdirectory in your plugins directory.

*******************************
Azureus Monitoring Plugin (AMP)
*******************************

This Plugin uses Java Management Extenstions (JMX) to gain information about the
local Java Virtual Machine. This includes memory usage, number of threads, gc
time, garbage collection and much more.
The user interface is made with the Eclipse Forms API to create 'Web-like' look.
Java 1.5 is a requirement for this plugin.

********
mini FAQ
********

Why does this plugin require Java 1.5?
Because JMX came with 1.5.

Where can I get Java 1.5 for MacOS?
http://www.apple.com/support/downloads/java2se50release1.html
You need OSX 10.4. Yes, Apple charges for Java :(

The swap sapce or another value in the "Operating System" section is wrong.
That's Sun's fault. It should be fixed with Java 1.6.0-ea_b17.
https://j2se.dev.java.net/

Where can I learn more about the memory types and pools of the Sun JVM?
http://java.sun.com/developer/technicalArticles/J2SE/jconsole.html
http://java.sun.com/j2se/1.5.0/docs/api/java/lang/management/MemoryMXBean.html

Are there other garbage collectors for the Sun JVM? How and when do I use them?
http://java.sun.com/docs/hotspot/gc1.4.2/
http://www-106.ibm.com/developerworks/java/library/j-jtp11253/

I have the Sun VM, is there any other than the client VM?
Yes, the server VM. Pass -server as argument to java.

Why eclipse forms?
Because I like the look and the expandable widgets.

Is thera any non-Sun JVM the plugin has been tested on?
Yeah, WebLogic JRockit from Bea
http://commerce.bea.com/index.jsp

Is this plugin 64bit ready?
Yes.

***********
Legal stuff
***********

This plugin is GPL

Both jface and forms are from the eclipse project
http://eclipse.org/
and CPL
http://www.eclipse.org/legal/cpl-v10.html

The trash icon is a gnome icon
http://art.gnome.org/art-icons/
