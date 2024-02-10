package com.resonance.letsdata.data.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecretManagerUtil {
    private static final Logger logger = LoggerFactory.getLogger(SecretManagerUtil.class);
    private static final ClientConfiguration clientConfiguration = new ClientConfiguration().
            withMaxConnections(350).
            withConnectionTimeout(50000).
            withConnectionMaxIdleMillis(120000).
            withReaper(false).
            withTcpKeepAlive(true);
    private final AWSSecretsManager awsSecretsManager;
    private final ConcurrentHashMap<String, String> cache;

    private static final HashMap<Regions, SecretManagerUtil> instanceMap = new HashMap<>();

    public static synchronized SecretManagerUtil getInstance(String region) {
        return getInstance(Regions.fromName(region));
    }

    public static synchronized SecretManagerUtil getInstance(Regions region) {
        if (!instanceMap.containsKey(region)) {
            instanceMap.put(region, new SecretManagerUtil(region));
        }
        return instanceMap.get(region);
    }

    public static synchronized void setInstance(HashMap<Regions, SecretManagerUtil> instance) {
        instanceMap.clear();
        instanceMap.putAll(instance);
    }

    public SecretManagerUtil(Regions regions) {
        awsSecretsManager = AWSSecretsManagerClientBuilder.standard().
                withRegion(regions).
                withCredentials(new EnvironmentVariableCredentialsProvider()).
                withClientConfiguration(clientConfiguration).build();
        cache = new ConcurrentHashMap<>();
    }

    // Test constructor
    public SecretManagerUtil(Regions regions, Map<String, String> mapValues) {
        awsSecretsManager = AWSSecretsManagerClientBuilder.standard().
                withRegion(regions).
                withCredentials(new EnvironmentVariableCredentialsProvider()).
                withClientConfiguration(clientConfiguration).build();
        cache = new ConcurrentHashMap<>(mapValues);
    }

    public static String getSecretValueString(String region, String secretArn) {
        return SecretManagerUtil.getInstance(region).getSecretsValue(secretArn);
    }

    public String getSecretsValue(String secretArn) {
        String cachedValue = cache.get(secretArn);
        if ( cachedValue != null) {
            return cachedValue;
        }

        GetSecretValueResult result;
        try {
            GetSecretValueRequest request = new GetSecretValueRequest().withSecretId(secretArn);
            result = awsSecretsManager.getSecretValue(request);
        } catch (Exception e) {
            throw new RuntimeException("get secrets value threw exception", e);
        }

        cache.put(secretArn, result.getSecretString());
        return result.getSecretString();
    }

    public static AWSSessionCredentials getSparkAwsCredentials(Gson gson, String region, String sparkCredentialsSecretArn, String methodName, String destinationType) {
        String secretValue = SecretManagerUtil.getInstance(region).getSecretsValue(sparkCredentialsSecretArn);
        Type credentialsJsonType = new TypeToken<Map<String, Map<String, Map<String, String>>>>() {}.getType();
        Map<String, Map<String, Map<String, String>>> credentialsMap = gson.fromJson(secretValue, credentialsJsonType);

        return new AWSSessionCredentials() {
            @Override
            public String getSessionToken() {
                return credentialsMap.get(methodName).get(destinationType).get("AWS_SESSION_TOKEN");
            }

            @Override
            public String getAWSAccessKeyId() {
                return credentialsMap.get(methodName).get(destinationType).get("AWS_ACCESS_KEY_ID");
            }

            @Override
            public String getAWSSecretKey() {
                return credentialsMap.get(methodName).get(destinationType).get("AWS_SECRET_ACCESS_KEY");
            }
        };
    }
}
