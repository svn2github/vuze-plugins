Version 20140408 v1.3

Avant-Propos
============
i18nAZ est un plugin de l'application cliente Vuze™ permettant de modifier les fichier de langue de l'application (appelé 'MessagesBundle').  


Installation
============
Il est recommandé d'installer le plugin en le téléchargeant depuis le site web 'http://www.vuze.com/plugins/?lang=fr_FR'

Vous pouvez aussi l'installer manuellement en suivant ces instructions :

1) Dans Vuze™, choisissez "Plugins" -> "Assistant d'installation"

2) Sélectionner "par fichier", et choisir le fichier "i18nAZ.zip" ou "i18nAZ.jar"

3) Après avoir terminé l'assistant, le plugin devrait apparaitre dans le menu Options -> Plugins.
Dans le cas contraire, redémarrez Vuze™.


Utilisation
===========
1) (Recommandé) Mettre à jour Vuze™. Ce qui vous permettra de travailler avec les fichiers de langue les plus récents.

2) Démarrez Vuze™.

3) Sélectionnez Affichage -> Plugins -> Internationalisation. 
   La langue par défaut (en anglais) se charge. 2 colonnes devraient s'afficher, la colonne 'Clé' et la colonne 'Référence'.

4) Cliquez sur le bouton "Ajouter une langue". Choisissez la langue que vous souhaitez modifier.
   Si vous voulez commencer une nouvelle langue, décochez la case "Afficher seulement celles qui existent" et choisissez une nouvelle langue. 
   Pour charger un fichier de langue externe, vous pouvez l'importer et la liste sera mise à jour.
   
5) Une colonne sera ajoutée avec la langue choisie. Vous pouvez commencer à traduire. (Note: Toutes les modifications sont sauvegardées automatiquement)

6) Touches du clavier: 
         <Flèches> Se déplacez dans le tableau 
         + Développer le nœud
         * Développer le nœud et tous les nœuds enfants 
         - Réduire le nœud 
         <Entrée> Modifier entrée sélectionné. Lors de l'édition: 
                     <Entrée> Enregistrer (si vous n'utilisez pas l'éditeur multiligne) 
                     <Echap> Annuler

7) Lorsque vous avez terminé, cliquez sur "Exporter". Vous serez invité à choisir un dossier de destination. Sélectionnez-en un, et vos fichiers de langue seront enregistrés. 
   
    Remarque pour les nouvelles langues 
    ------------------- 
    Si votre nouvelle langue est spécifique au pays, vous devez renommez le fichier au format suivant: 
    MessageBundles_xx_ZZ.properties, où 
      xx = les 2 lettres du code de langue. (En savoir plus sur 'http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt') 
      Code pays ZZ = 2 lettres (En savoir plus sur 'http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html')

8) Envoyer votre fichier de langue à la communauté Vuze™.


Affichage de votre langue 
========================= 
Pour voir les modifications apportés aux fichiers de langue dans Vuze™, redémarrez l'application.






Journal des modifications 
=========

20140408: aka v1.3
          Correction: Restructuration du code (avec SWTSkin)
          Correction: Intégration dans la barre d’outils de Vuze™
          Correction: Intégration de l’éditeur dans la vue principale du plugin 
          Nouveau: Ajout de commandes de base à l’éditeur (Annuler, Répéter, Couper, Copier, Coller…)
          Nouveau: Ajout de commandes spéciales à l’éditeur (Modificateur de casse, Insertion du caractère spécial ‘™’
          Nouveau: Création d’un menu contextuelle
          Nouveau: Création d’un menu édition dans Vuze™ (qui se masque automatiquement)
          Correction: Filtre d’affichage plus précis
          Correction: Amélioration de l’option de recherche
          Nouveau: Enregistrement automatique des changements
          Nouveau: Mise à jour automatique des changements dans Vuze™ (Après redémarrage)
          Nouveau: Enregistrement automatique des paramètres de l’interface utilisateur (Largeurs de colonne, Filtres sélectionnés…)
          Correction: Interface plus intuitive avec de multiples info-bulles
          Nouveau: Internationalisation du plugin

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

20040224: Version initiale
