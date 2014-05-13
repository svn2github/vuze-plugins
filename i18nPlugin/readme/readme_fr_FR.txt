Version 20140513 v1.4

<b>Avant-Propos</b>

Internationalize Vuze est un plugin de l'application cliente Vuze™ permettant de modifier les fichier de langue de l'application (appelé 'MessagesBundle').  


<b>Installation</b>

Il est recommandé d'installer le plugin en le téléchargeant depuis le site web 'http://www.vuze.com/plugins/?lang=fr_FR'

Vous pouvez aussi l'installer manuellement en suivant ces instructions :

1) Dans Vuze™, choisissez "Plugins" -> "Assistant d'installation"

2) Sélectionner "par fichier", et choisir le fichier "i18nAZ.zip" ou "i18nAZ.jar"

3) Après avoir terminé l'assistant, le plugin devrait apparaitre dans le menu Options -> Plugins.
Dans le cas contraire, redémarrez Vuze™.


<b>Utilisation</b>

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
   
    <u>Remarque pour les nouvelles langues</u>

    Si votre nouvelle langue est spécifique au pays, vous devez renommez le fichier au format suivant: 
    MessageBundles_xx_ZZ.properties, où :
      xx = les 2 lettres du code de langue 
           (En savoir plus sur 'http://fr.wikipedia.org/wiki/Liste_des_codes_ISO_639-1') 
      ZZ = les 2 lettres du code pays
           (En savoir plus sur 'http://fr.wikipedia.org/wiki/ISO_3166')

8) Envoyer votre fichier de langue à la communauté Vuze™.


<b>Affichage de votre langue</b>

Pour voir les modifications apportés aux fichiers de langue dans Vuze™, redémarrez l'application.






<b>Journal des modifications</b>
20140513: aka v1.4
          Nouveau: Possibilité de traduire les plugins qui ne sont pas installés
          Nouveau: Ajout d'un correcteur orthographique (Hunspell avec JNA)
          Nouveau: Ajout d'un traducteur (fourni par Google)
          Nouveau: Possibilité d'importer des fichiers de langue en lecture seule pour comparer
          Nouveau: Ajout d'un onglet dans la boîte de dialogue d'aide qui répertorie les plugins non localisables
          Nouveau: Ajout d'un combo qui liste les différents fichiers de langue présents dans chaque plugin
          Nouveau: Ajout d'une vue de configuration pour désactiver le téléchargement des plugins qui ne sont pas installés et pour réinitialiser le dictionaire personnel
          Nouveau: Affichage du nombre d'entrées non traduites dans la barre latérale
          Nouveau: Affichage de la progression de la traduction pour chaque fichier et chaque plugin
          Correction: Détection de tous les fichiers de langue même ceux qui n'ont pas été défini dans la propriété 'plugin.langfile'
          Correction: La fonction d'exportation crée maintenant un fichier zip avec les différents fichiers de langue enregistrés dans un dossier séparé
          Correction: Modification de la boîte de dialogue permettant de supprimer une colonne de langue
          Correction: Changement du bouton de filtre URL pour n'afficher aussi que les URLs
          
20140408: aka v1.3.1
          Correction: un bug mineur
          
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
