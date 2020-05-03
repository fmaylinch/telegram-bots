package com.codethen.search;

import com.codethen.telegram.lanxatbot.search.SearchEntry;
import com.codethen.telegram.lanxatbot.search.SearchRepository;
import com.mongodb.reactivestreams.client.MongoClient;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

public class MongoSearchRepository implements SearchRepository {

    private static final String COLLECTION_NAME = "searches";

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoSearchRepository(MongoClient mongoClient, String databaseName) {
        mongoTemplate = new ReactiveMongoTemplate(mongoClient, databaseName);
    }

    @Override
    public void registerSearch(SearchEntry searchEntry) {
        mongoTemplate.insert(searchEntry, COLLECTION_NAME).block();
    }
}
