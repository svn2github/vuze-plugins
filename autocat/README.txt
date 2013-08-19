AutoCat 1.0.0
Copyright(c) 2005 Chris Rose and AIMedia.
(GPL, blah blah...)

Here's the new toy.  Nothing fancy, just a basic rule-based automatic torrent
categorization plugin.  Provide it with regular expressions, and it will
automagically categorize plugins that match them.  It's not exactly a work of
programming art, by any stretch of the imagination, but it does do the trick.

Be advised that it will OVERWRITE any categories that you have set manually, if
there is a rule match.  This is not currently preventable, so if you have a
careful and elaborate taxonomy of categories, I'd recommend that you not install
this plugin, unless you want to spend some time swearing at me.  Which you are
welcome to do, but not for that "bug".  That's a feature.

Bug reports can be filed at plugins@offlineblog.com

Version History
==============================================================================
0.1 	- initial release
	- Matched regular expressions to torrent file names

1.0.0	- The good one
	- Rewrote the whole thing from scratch (well, almost)
	- New config file format (backwards compatible, so far)
	- Now has the ability to match torrents based on tracker URL as well
		(by popular request)
1.0.1	- The better one
	- Fixed a bug where the config file would not be created if it didn't
		already exist.  For me, this didn't really happen, since I already
		had one.  Sorry, guys.
	- There were some missing messages in the Messages file.  Added those that
		I knew about.
1.2.0 - Other things are improved
    - The rules are now stored as properties, making it (slightly) easier to
      modify them by hand without the UI.
    - Autocat no longer overwrites user-selected categories if that is the desired
      behaviour ([ACAT-5](http://offby1.no-ip.org:8080/browse/ACAT-5))

If you have not received this file either from http://azureus.sourceforge.net/
or http://offby1.net/ I would suggest that you do so, since anyone could
have done anything with it.  There will always be a current version at
http://www.offby1.net/plugins/autocat-current.zip

Enjoy!