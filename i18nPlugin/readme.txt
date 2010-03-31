Release 20070515 v1.2

About
=====
i18nAZ is an Azureus plugin for modifying Azureus' language files (called
MessagesBundle).  Although the plugin is for internationalizing Azureus, the
plugin itself is not currently internationalized.  This means that the plugin
controls are in English.


Installation
============
It's recommended that you install the plugin via the Installation Wizard using
the sourceforge site.  If you can't, you can install it manually as follows:

1) In Azureus, choose "Plugins"->"Installation Wizard"

2) Select "By File", and choose either the i18nAZ zip or jar file

3) After completing the wizard, there should be a menu item in the plugin menu.
   If not, restart AZ.


Usage
=====
1) (Recommended) Get the CVS version of Azureus from 
   http://azureus.sourceforge.net/index_CVS.php .  This will provide you with
   recent MessagesBundles.

2) Start Azureus

3) Select View -> Plugins -> Internationalize Azureus
   The default language (english) will load.  You will initially have to 
   columns: Key, and the default language.

4) Click the "Add Language" button.  Choose the Language you wish to edit.
   If you want to start a new language, click the "New Language" button instead.
   If you have already made your own, you can change to that directory and 
   the list will update.
   
5) A Column will be added with the language you chose.

6) Table Keys:
        <Arrows> Move you within the table
        +        Expand a tree
        *        Expand tree and every subtree
        -        Collapse a tree
        <Enter>  Edit selected entry.  While editing:
                    <Enter>                 Save (When NOT using Fancier Editor)
                    <Esc>                   Cancel

7) When you are done, press "Save..".  You will be asked for a destination 
   folder. Select one, and your lanuage file will be written.  
   
   Note for new languages
   ---- --- --- ---------
   If your new language is country specific, you exit close the plugin window
   and rename the file to the following format:
   MessageBundles_xx_ZZ.properties, where
     xx = and 2 letter language code.  This will already be set for you (see 
          http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt )
     ZZ = 2 letter country code ( see 
          http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html )

8) Send your language file to the Azureus people


Viewing Your Language
=====================
If you saved your language file(s) in Azureus' program directory, they will 
show up on the "Languages" menu the next you start Azureus.


Known Bugs
==========
1) If you add the same Language/Country pair twice (even if they are in 
   different locations), saving will only save one of them (the last one)


Future
======
In order of Priority:
- Delete entry (currently, blanking a value will erase it when saving.  You
                won't notice until you re-load though)

- Load any .properties file allowing this app to be used as a general JAVA 
  bundle editor.

- Add entry


changelog
=========

20070515: aka v1.2
          New: Comments are shown and saved
          New: Help button showing this file
          Fix: Better handling of multi-lined resource values

20060431: aka v1.1
          New: Filter
          Fix: Update to newer Azureus Plugin API

20040315: Fix: Closing Window & re-opening
          New: "Fancy Editor" showing \n as a new line and allowing <Enter> to
               be pressed.
          New: New Language Button.

20040302: readme.txt had order of language/country code reversed.

20040301: New: Improved editor window
          New: "No Tree" option

20040226: Fixed bug where '\n' were turned into 'n' on modified entries
        : Save order is not based on Reference Language 
            (MessagesBundles.properties)
        : Entries that did not exist in Reference Language are now properly
          shown and tagged.
        : Minor fixes to the table cursor

20040224: Initial Release