package com.codethen.profile;

import com.codethen.telegram.lanxatbot.exception.ProfileNotConfiguredException;
import com.codethen.telegram.lanxatbot.exception.ProfileNotExistsException;
import com.codethen.telegram.lanxatbot.profile.UserProfile;
import com.codethen.telegram.lanxatbot.profile.UserProfileRepository;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class CachedUserProfileRepository implements UserProfileRepository {

    private final LoadingCache<Integer, UserProfile> repoCache;
    private final UserProfileRepository internalRepo;

    public CachedUserProfileRepository(UserProfileRepository internalRepo) {

        this.internalRepo = internalRepo;

        repoCache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .build(new CacheLoader<Integer, UserProfile>() {
                    @Override
                    public UserProfile load(Integer userId) {
                        final UserProfile profile = internalRepo.getProfileById(userId);
                        if (profile == null) throw new ProfileNotExistsException(userId);
                        if (profile.getYandexApiKey() == null) throw new ProfileNotConfiguredException(profile);
                        return profile;
                    }
                });
    }

    @Override
    public UserProfile getProfileById(Integer userId) {
        try {
            System.out.println("Loading profile, maybe from cache: " + userId);
            return repoCache.get(userId);
        } catch (UncheckedExecutionException e) {
            throw (RuntimeException) e.getCause(); // TODO: Is this the ProfileNotExistsException I throw?
        } catch (ExecutionException e) {
            throw new RuntimeException("Error accessing cache for userId " + userId, e);
        }
    }

    @Override
    public void saveOrUpdate(UserProfile profile) {

        System.out.println("Storing profile and invalidating cache entry for userId: " + profile.getId());
        internalRepo.saveOrUpdate(profile);
        repoCache.invalidate(profile.getId());
    }
}
