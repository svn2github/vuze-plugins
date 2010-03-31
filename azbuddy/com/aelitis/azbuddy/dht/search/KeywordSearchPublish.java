package com.aelitis.azbuddy.dht.search;

import java.util.Arrays;
import java.util.List;

import com.aelitis.azbuddy.dht.DDBKeyValuePair;


class KeywordSearchPublish extends KeywordSearch {
	

	
	
	/**
	 * Publishes the items for a keyword lookup
	 * @param terms the metadata that should be published (metadata item -> value mapping)
	 * @param result the DHT key where a lookup should point to 
	 */
	public KeywordSearchPublish(List<MetaDataItem> terms, DDBKeyValuePair result)
	{
		super(terms);
		state = SEARCH_UPDATE_AND_ESTIMATE_KEYWORD_WEIGHTS;
		for(MetaDataItem i : terms)
			i.queryWeights(dhtMan);
	}
	
	
	void createTuples()
	{
		state = SEARCH_CREATE_TUPLES;
		MetaDataItem[] weightedTerms = terms.clone();
		Arrays.sort(weightedTerms, new MetaDataItem.WeightSorter());
		for(int i=0;i<weightedTerms.length;i++)
		{
			if(weightedTerms[i].uniquenessWeight <= TWO_KEYWORD_TUPLE_WEIGHT_THRESHOLD)
			{
				for(int j = i+1;j<weightedTerms.length;j++)
				{
					tuples.add(new MetaDataNTuple(new MetaDataItem[] {weightedTerms[i],weightedTerms[j]},dhtMan));
				}
			} else {
				tuples.add(new MetaDataNTuple(new MetaDataItem[] {weightedTerms[i]},dhtMan));
			}
		}

	}
	
	void doTupleQueries()
	{
		state = SEARCH_DO_DDB_QUERIES;
		for(MetaDataNTuple i : tuples)
			i.keyMapping.refresh(60, false);
	}
	
	@Override
	void parseResults()
	{
		// TODO Auto-generated method stub
		
	}

}
