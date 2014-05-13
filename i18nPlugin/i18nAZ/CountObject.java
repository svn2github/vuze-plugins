/*
 * CountObject.java
 *
 * Created on April 16, 2014, 3:10 AM
 */
package i18nAZ;

import i18nAZ.LocalizablePluginManager.LocalizablePlugin.LangFileObject;
import i18nAZ.TargetLocaleManager.TargetLocale;

/**
 * CountObject class
 * 
 * @author Repris d'injustice
 */
class CountObject
{
    int entryCount = 0;
    int emptyCount = 0;
    int unchangedCount = 0;
    int extraCount = 0;
    int redirectKeyCount = 0;
    int urlsCount = 0;
    void add(CountObject countObject)
    {
        if(countObject == null)
        {
            return;
        }
        this.entryCount += countObject.entryCount ;
        this.emptyCount += countObject.emptyCount;
        this.unchangedCount += countObject.unchangedCount;
        this.extraCount += countObject.extraCount;
        this.redirectKeyCount += countObject.redirectKeyCount;
        this.urlsCount += countObject.urlsCount;
    }
    public void clear()
    {
        this.entryCount = 0 ;
        this.emptyCount = 0;
        this.unchangedCount = 0;
        this.extraCount = 0;
        this.redirectKeyCount = 0;
        this.urlsCount = 0;
    }  
}
class CountEvent
{
    CountObject counts = new CountObject();;
    LangFileObject langFileObject = null;
    TargetLocale targetLocale = null;
    CountEvent(LangFileObject langFileObject, TargetLocale targetLocale)
    {
        this.langFileObject = langFileObject;
        this.targetLocale = targetLocale;
    }
}
interface CountListener
{
    void countChanged(CountEvent e);
}


