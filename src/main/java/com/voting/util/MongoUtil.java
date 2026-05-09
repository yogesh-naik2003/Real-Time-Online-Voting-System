package com.voting.util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.voting.config.AppConfig;

public final class MongoUtil {
    private static final MongoClient CLIENT = MongoClients.create(AppConfig.get("mongodb.uri", "MONGODB_URI"));

    private MongoUtil() {
    }

    public static MongoDatabase getDatabase() {
        return CLIENT.getDatabase(AppConfig.get("mongodb.database", "MONGODB_DATABASE"));
    }
}
