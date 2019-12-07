package com.codethen.profile;

import com.codethen.telegram.lanxatbot.profile.UserProfile;
import com.codethen.telegram.lanxatbot.profile.UserProfileRepository;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClient;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import javax.annotation.Nullable;

public class MongoUserProfileRepository implements UserProfileRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoUserProfileRepository(MongoClient mongoClient, String databaseName) {
        mongoTemplate = new ReactiveMongoTemplate(mongoClient, databaseName);
    }

    @Nullable
    @Override
    public UserProfile getProfileById(Integer userId) {

        System.out.println("Loading profile from database: " + userId);

        return mongoTemplate.findOne(
                Query.query(Criteria.where("userId").is(userId)),
                UserProfile.class, "users")
                .block();
    }

    @Override
    public void saveOrUpdate(UserProfile profile) {

        System.out.println("Storing profile into database: " + profile);

        final UpdateResult result = mongoTemplate.upsert(
                Query.query(Criteria.where("userId").is(profile.getUserId())),
                new Update() // TODO: How to update the whole object?
                        .set("langFrom", profile.getLangFrom())
                        .set("langTo", profile.getLangTo())
                        .set("langOtherFrom", profile.getLangOtherFrom())
                        .set("langOtherTo", profile.getLangOtherTo())
                        .set("yandexApiKey", profile.getYandexApiKey()),
                UserProfile.class, "users")
                .block();

        System.out.println("Update result: " + result);
    }
}
