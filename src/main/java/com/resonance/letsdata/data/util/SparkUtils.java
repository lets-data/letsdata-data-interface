package com.resonance.letsdata.data.util;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkUtils {
    private static final Logger logger = LoggerFactory.getLogger(SparkUtils.class);

    public static SparkSession createSparkSession(String appName, String readDestination, String writeDestination, String readUri, String writeUri, AWSSessionCredentials readCredentials, AWSSessionCredentials writeCredentials) {
        if (!SparkSession.getActiveSession().isEmpty()) {
            SparkSession.getActiveSession().get().stop();
        }

        SparkSession.Builder builder = SparkSession.builder()
                .appName(appName)
                .master("local")
                .config("spark.ui.enabled", "false")
                .config("spark.driver.bindAddress", "127.0.0.1");

        if (readDestination.toUpperCase().equals("S3") || writeDestination.toUpperCase().equals("S3")) {
            builder = builder.config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");

            String readBucket = null;
            String writeBucket = null;

            if (readDestination.toUpperCase().equals("S3")) {
                readBucket = parseBucketNameFromS3Uri(readUri);
                builder = builder.config("spark.hadoop.fs.s3a.bucket." + readBucket + ".access.key",
                        readCredentials.getAWSAccessKeyId());
                builder = builder.config("spark.hadoop.fs.s3a.bucket." + readBucket + ".secret.key",
                        readCredentials.getAWSSecretKey());
                builder = builder.config("spark.hadoop.fs.s3a.bucket." + readBucket + ".session.token",
                        readCredentials.getSessionToken());
            }

            if (writeDestination.toUpperCase().equals("S3")) {
                writeBucket = parseBucketNameFromS3Uri(writeUri);
                if (!readBucket.equals(writeBucket)) {
                    builder = builder.config("spark.hadoop.fs.s3a.bucket." + writeBucket + ".access.key",
                            writeCredentials.getAWSAccessKeyId());
                    builder = builder.config("spark.hadoop.fs.s3a.bucket." + writeBucket + ".secret.key",
                            writeCredentials.getAWSSecretKey());
                    builder = builder.config("spark.hadoop.fs.s3a.bucket." + writeBucket + ".session.token",
                            writeCredentials.getSessionToken());
                }
            }
        } else {
            throw new RuntimeException(readDestination + " readDestination not supported");
        }

        final SparkSession spark = builder.getOrCreate();
        return spark;
    }

    public static Dataset<Row> readSparkDataframe(SparkSession spark, String readDestination, String readUri, String readFormat, Map<String, String> readOptions) {
        Dataset<Row> df = null;
        if (readDestination.toUpperCase().equals("S3")) {
            org.apache.spark.sql.DataFrameReader dfReader = spark.read();
            String lineSep = null;

            if (readOptions != null && !readOptions.isEmpty()) {
                for (Map.Entry<String, String> entry : readOptions.entrySet()) {
                    if (entry.getKey().toLowerCase().equals("linesep")) {
                        lineSep = entry.getValue();
                    } else {
                        dfReader = dfReader.option(entry.getKey(), entry.getValue());
                    }
                }
            }

            if (readFormat.equals("json")) {
                dfReader = dfReader.format(readFormat);
                df = dfReader.load(readUri);
            } else if (readFormat.equals("parquet")) {
                dfReader = dfReader.format(readFormat);
                df = dfReader.load(readUri);
            } else if (readFormat.equals("csv")) {
                dfReader = dfReader.format(readFormat);
                df = dfReader.load(readUri);
            } else if (readFormat.equals("text")) {
                if (lineSep != null) {
                    dfReader = dfReader.option("lineSep", lineSep);
                }
                df = dfReader.text(readUri);
            } else {
                throw new RuntimeException(readFormat + " readFormat not supported");
            }
        } else {
            throw new RuntimeException(readDestination + " readDestination not supported");
        }

        return df;
    }

    public static void writeSparkDataframe(SparkSession spark, String writeDestination, String writeUri, String writeFormat, String writeMode, Map<String, String> writeOptions, Dataset<Row> df) {
        if (writeDestination.toUpperCase().equals("S3")) {
            org.apache.spark.sql.DataFrameWriter<Row> dfWriter = df.write().mode(writeMode);

            if (writeOptions != null && !writeOptions.isEmpty()) {
                for (Map.Entry<String, String> entry : writeOptions.entrySet()) {
                    dfWriter = dfWriter.option(entry.getKey(), entry.getValue());
                }
            }

            if (writeFormat.equals("json")) {
                dfWriter = dfWriter.format(writeFormat);
            } else if (writeFormat.equals("csv")) {
                dfWriter = dfWriter.format(writeFormat);
            } else if (writeFormat.equals("parquet")) {
                dfWriter = dfWriter.format(writeFormat);
            } else {
                throw new RuntimeException(writeFormat + " writeFormat not supported");
            }

            dfWriter.save(writeUri);
        } else {
            throw new RuntimeException(writeDestination + " writeDestination not supported");
        }
    }

    public static void dumpEnvironmentVariables() {
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            logger.info(entry.getKey() + ": " + entry.getValue());
        }
    }

    public static String parseBucketNameFromS3Uri(String s3Uri) {
        try {
            URI parsedUri = new URI(s3Uri);

            if ("s3a".equals(parsedUri.getScheme()) && parsedUri.getHost() != null) {
                return parsedUri.getHost();
            } else {
                throw new IllegalArgumentException("Not a valid S3 URI: " + s3Uri);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI: " + s3Uri, e);
        }
    }
}
