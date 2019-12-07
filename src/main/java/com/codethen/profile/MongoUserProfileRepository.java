package com.codethen.profile;

import com.codethen.ApiKeys;
import com.codethen.telegram.lanxatbot.profile.UserProfile;
import com.codethen.telegram.lanxatbot.profile.UserProfileRepository;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import javax.annotation.Nullable;

public class MongoUserProfileRepository implements UserProfileRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoUserProfileRepository() {

        mongoTemplate = new ReactiveMongoTemplate(MongoClients.create(
                "mongodb+srv://" + ApiKeys.MONGO_CREDENTIALS + "@" +
                        "bts-cluster-r1swc.mongodb.net/lanxatbot" +
                        "?retryWrites=true&w=majority"),
                "lanxatbot");
    }

    @Nullable
    @Override
    public UserProfile getProfileById(Integer userId) {

        // TODO: Add cache
        return mongoTemplate.findOne(
                Query.query(Criteria.where("userId").is(userId)),
                UserProfile.class, "users")
                .block();
    }

    @Override
    public void saveOrUpdate(UserProfile profile) {

        // TODO: Update cache
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
