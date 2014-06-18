/******************************************************************************
Cubit distribution
Copyright (C) 2008 Bernard Wong

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

The copyright owner can be contacted by e-mail at bwong@cs.cornell.edu
*******************************************************************************/

package org.cornell.hyper.overlay;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public class EntryContainer {	
	private HashMap<String, HashSet<MovieEntry>> 	keyToMovie;
	HashMap<MovieEntry, MovieEntry>					allEntries;
	private Set<String> 							commonWords;
	private Set<String> 							punct;
	
	private static Logger log = Logger.getLogger(EntryContainer.class.getName());
		
	public EntryContainer(Set<String> inPunct, Set<String> inCommonWords) {
		punct 		= new HashSet<String>(inPunct);				
		keyToMovie 	= new HashMap<String, HashSet<MovieEntry>>();
		allEntries 	= new HashMap<MovieEntry, MovieEntry>();
		commonWords	= new HashSet<String>();
		
		Iterator<String> commonIt = inCommonWords.iterator();
		while (commonIt.hasNext()) {
			commonWords.add(commonIt.next().toLowerCase());
		}
	}

	// NOTE: Used to remove punctuation. Made it synchronized just in 
	// case we later want the punctuation set to be malleable.
	public synchronized String[] removeSymbols(String fullString) {
		Iterator<String> punctIt = punct.iterator();
		while (punctIt.hasNext()) {			
			String curPunct = punctIt.next();
			String[] words = fullString.split(curPunct);
			StringBuffer stringBuf = new StringBuffer();
			for (int i = 0; i < words.length; i++) {
				stringBuf.append(words[i].trim().toLowerCase());
				stringBuf.append(" ");
			}
			fullString = stringBuf.toString().trim();
		}
		return fullString.trim().split(" ");
	}
		
	// TODO: May want to check that curWord is a word within the movie title
	public synchronized void addKeyEntry(String curWord, MovieEntry curMovieEntry) {				
		curWord = curWord.toLowerCase();
		// Check that this keyword actually belongs to this movie
		boolean matchFound = false;
		String[] wordsArray = removeSymbols(curMovieEntry.getMovName());		
		for (int i = 0; i < wordsArray.length; i++) {
			if (curWord.equals(wordsArray[i].toLowerCase())) {
				matchFound = true;
				break;
			}
		}
		if (!matchFound) { 	
			log.warn("Keyword not found in movie");
			return;	// Match not found, do not add entry
		}		
		if (commonWords.contains(curWord)) {
			log.warn("Skipping common word key");
			return;
		}
		if (!keyToMovie.containsKey(curWord)) {
			keyToMovie.put(curWord, new HashSet<MovieEntry>());
		}
		if (allEntries.containsKey(curMovieEntry)) {
			MovieEntry oldEntry = allEntries.get(curMovieEntry);
			// Old entry is expires before the new one. Replace it
			if (oldEntry.getExpireDate().before(
					curMovieEntry.getExpireDate())) {
				removeMovieEntry(oldEntry);
				allEntries.put(curMovieEntry, curMovieEntry);
				keyToMovie.get(curWord).add(allEntries.get(curMovieEntry));
			}			
		} else {
			allEntries.put(curMovieEntry, curMovieEntry);
			keyToMovie.get(curWord).add(allEntries.get(curMovieEntry));
		}		
		//keyToMovie.get(curWord).add(curMovieEntry);
		//allEntries.add(curMovieEntry);
	}
	
	public synchronized Set<MovieEntry> getEntries(String keyword) {
		if (!keyToMovie.containsKey(keyword)) {
			return null;
		}
		return new HashSet<MovieEntry>(keyToMovie.get(keyword));		
	}
	
	public synchronized Set<String> getStrictCloserKeys(
			String localKey, String remoteKey) {
		Set<String> retSet = new HashSet<String>();
		// Iterate through the set of keys
		Iterator<String> keyIt = keyToMovie.keySet().iterator();
		while (keyIt.hasNext()) {
			String curWord = keyIt.next();
			//HashSet<MovieEntry> movieSet = keyToMovie.get(curWord);
			int localDist = EditDistance.computeEditDistance(localKey, curWord);
			int remoteDist = EditDistance.computeEditDistance(remoteKey, curWord);
			if (remoteDist < localDist) {
				retSet.add(curWord);
			}
		}
		return retSet;		
	}
	
	/*
	// Get the entries from the K closest keys, by edit distance.
	public synchronized Map<String, Set<MovieEntry>> getCloserKeys(
			String localKey, String remoteKey) {
		Map<String, Set<MovieEntry>> retMap = new HashMap<String, Set<MovieEntry>>();
		// Iterate through the set of keys
		Iterator<String> keyIt = keyToMovie.keySet().iterator();
		while (keyIt.hasNext()) {
			String curWord = keyIt.next();
			HashSet<MovieEntry> movieSet = keyToMovie.get(curWord);
			int localDist = EditDistance.computeEditDistance(localKey, curWord);
			int remoteDist = EditDistance.computeEditDistance(remoteKey, curWord);
			if (remoteDist < localDist) {
				Set<MovieEntry> newMovieSet = new HashSet<MovieEntry>();
				newMovieSet.addAll(movieSet);
				// Adding current key and the movie set associated with this key 
				retMap.put(curWord, newMovieSet);
			}
		}
		return retMap;
	}
	*/
	
	public synchronized List<String> normalizeKeywords(List<String> keywords) {
		List<String> retList = new ArrayList<String>();
		Iterator<String> keyIt = keywords.iterator();
		while (keyIt.hasNext()) {
			String curKey = keyIt.next().toLowerCase();
			if (commonWords.contains(curKey)) {
				continue;
			}
			retList.add(curKey);
		}
		return retList;
	}
	
	private static int computeMinDist(String curKey, List<String> movieWords) {
		assert(movieWords.size() > 0);
		int minDist = -1;
		Iterator<String> strIt = movieWords.iterator();
		while (strIt.hasNext()) {		
			int curDist = EditDistance.computeEditDistance(curKey, strIt.next());
			if (minDist == -1 || curDist < minDist) {
				minDist = curDist;
			} 
		}
		return minDist;
	}
	
	public synchronized int computeDistSum(String movieTitle, List<String> keywords) {
		int distSum = 0;
		List<String> movieList = new ArrayList<String>();
		String[] movieKeys = removeSymbols(movieTitle);
		for (int i = 0; i < movieKeys.length; i++) {
			String curKey = movieKeys[i].toLowerCase();
			if (commonWords.contains(curKey)) {
				continue;
			}
			movieList.add(curKey);			
		}
		Iterator<String> keyIt = keywords.iterator(); 
		while (keyIt.hasNext()) {
			distSum += computeMinDist(keyIt.next().toLowerCase(), movieList);
		}
		return distSum;		
	}	
	
	public synchronized Set<MovieEntry> getKClosestKeys(
			int kVal, List<String> searchTerms) {
		Date curDate = new Date();	// Current time
		ArrayList<MovieEntry> expired = new ArrayList<MovieEntry>();		
		TreeMap<Integer, List<MovieEntry>> sortedMovies 
				= new TreeMap<Integer, List<MovieEntry>>();
		Iterator<MovieEntry> movieIt = allEntries.keySet().iterator();
		while (movieIt.hasNext()) {
			MovieEntry curMovie = movieIt.next();
			if (curMovie.getExpireDate().before(curDate)) {
				expired.add(curMovie);
				continue;
			}
			int dist = computeDistSum(curMovie.getMovName(), searchTerms);
			if (!sortedMovies.containsKey(dist)) {
				sortedMovies.put(dist, new ArrayList<MovieEntry>());
			}
			sortedMovies.get(dist).add(curMovie);			
		}		
		// Remove all the expired movies
		for (int i = 0; i < expired.size(); i++) {
			removeMovieEntry(expired.get(i));
		}
		Set<MovieEntry> retSet = new HashSet<MovieEntry>();
		Iterator<Integer> sortedIt = sortedMovies.keySet().iterator();
		while (sortedIt.hasNext()) {
			if (retSet.size() > kVal) {
				break;
			}
			List<MovieEntry> curList = sortedMovies.get(sortedIt.next());
			retSet.addAll(curList);
		}
		return retSet;		
	}
		
	// Remove all the keywords
	public synchronized void removeMovieEntry(MovieEntry inEntry) {
		if (!allEntries.containsKey(inEntry)) {
			return;	// Entry not in the container.			
		}
		String entryTitle = inEntry.getMovName();
		String[] words = removeSymbols(entryTitle);
		for (int i = 0; i < words.length; i++) {
			String curWord = words[i].toLowerCase();
			if (commonWords.contains(curWord)) {
				continue;	// Don't bother if it is common
			}						
			HashSet<MovieEntry> curMovieSet = keyToMovie.get(curWord);
			if (curMovieSet == null) {				
				continue;
			}			
			if (!curMovieSet.contains(inEntry)) {				
				continue;				
			}
			curMovieSet.remove(inEntry);
			// If the number of movies for this keyword is now 0, movie
			// the keyword from the map.
			if (curMovieSet.size() == 0) {
				keyToMovie.remove(curWord);
			}
		}
		allEntries.remove(inEntry);
	}
	
	// Get a random MovieEntry
	public synchronized MovieEntry getRandomEntry(){
		if (allEntries.size() == 0) {
			return null;
		}
		MovieEntry retVal = null;
		int numIter = (new Random()).nextInt(allEntries.size()) + 1;
		Iterator<MovieEntry> movieIt = allEntries.keySet().iterator();
		while (movieIt.hasNext() && numIter-- > 0) {
			retVal = movieIt.next();
		}
		return retVal;
	}
	
	public synchronized List<String> getRandomKeys(int numKeys) {
		log.info("In getRandomKeys: numKeys is " + numKeys);
		ArrayList<String> keyList = new ArrayList<String>(keyToMovie.keySet());		
		ArrayList<String> retList = new ArrayList<String>();
		Random randEval = new Random();
		for (int i = 0; i < numKeys && keyList.size() > 0; i++) {
			int randIndex = randEval.nextInt(keyList.size());
			retList.add(keyList.get(randIndex));
			keyList.remove(randIndex);
		}		
		return retList;
	}	
}
