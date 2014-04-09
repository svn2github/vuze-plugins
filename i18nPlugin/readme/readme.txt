Release 20140408 v1.3

About
=====
i18nAZ is an Vuze™ plugin for modifying Vuze™ language files (called MessagesBundle).


Installation
============
It's recommended that you install the plugin by downloading it from the website 'http://www.vuze.com/plugins/'

If you can't, you can install it manually as follows:

1) In Vuze™, choose "Plugins"->"Installation Wizard"

2) Select "By File", and choose either the "i18nAZ.zip" or "i18nAZ.jar"

3) After completing the wizard, there should be a menu item in the plugin menu.
   If not, restart Vuze™.


Usage
=====
1) (Recommended) Update Vuze™. This will provide you with recent MessagesBundles.

2) Start Vuze™

3) Select View -> Plugins -> Internationalize Azureus
   The default language (english) will load.  You will initially have two columns: 'Key' and 'Reference'.

4) Click the "Add Language" button. Choose the Language you wish to edit.
   If you want to start a new language, uncheck the case "Show only existing" and choose a new language.
   If you have already made your own, you can import it and the list will update.
   
5) A Column will be added with the language you chose. You can begin to translate. (Note: All changes are automatically saved)

6) Table Keys:
        <Arrows> Move you within the table
        +        Expand a tree
        *        Expand tree and every subtree
        -        Collapse a tree
        <Enter>  Edit selected entry.  While editing:
                    <Enter>                 Save (When NOT using Fancier Editor)
                    <Esc>                   Cancel

7) When you are done, press "Export".  You will be asked for a destination folder. Select one, and your language file will be written.  
   
   Note for new languages
   ---- --- --- ---------
   If your new language is country specific, you exit close the plugin window
   and rename the file to the following format:
   MessageBundles_xx_ZZ.properties, where
     xx = and 2 letter language code.  This will already be set for you (see 
          http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt )
     ZZ = 2 letter country code ( see 
          http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html )

8) Send your language file to the  Vuze™ people


Viewing Your Language
=====================
To see the changes made ​​to the language files in Vuze ™, restart the application.






changelog
=========

20140408: aka v1.3
          Fix: Restructuring of code (with SWTSkin)
          Fix: Integration into Vuze™ toolbar
          Fix: Integration of the editor in the plugin main view
          New: Adding basic commands to the editor (Undo, Redo, Cut, Copy, Paste ...)
          New: Add special commands to the editor (Case modifier, Insert special character '™'
          New: Creating a popup menu
          New: Creating an edition menu in Vuze™ (which automatically hides)
          Fix: Display filter more accurate
          Fix: Improving of search option
          New: Auto-saving changes
          New: Auto-updating changes in Vuze™ (After restart)
          New: Auto-saving of interface user settings (Columns widths, Selected filters...)
          Fix: More intuitive interface with multiple tooltips
          New: Internationalization of plugin

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