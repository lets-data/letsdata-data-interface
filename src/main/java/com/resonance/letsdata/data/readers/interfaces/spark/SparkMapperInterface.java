package com.resonance.letsdata.data.readers.interfaces.spark;

import com.amazonaws.auth.AWSSessionCredentials;
import com.google.gson.Gson;
import com.resonance.letsdata.data.util.SecretManagerUtil;

import java.util.Map;

/**
 * LetsData's Spark interfaces are inspired by the original Google Map Reduce paper. We have defined a MAPPER interface and a REDUCER interface. Here is how they work to process a dataset using Spark.
 *
 * Recall that a dataset's amount of work is defined by a manifest file. For example, for S3 read destination, this is essentially a list of files that need to be processed.
 *
 *      "manifestFile": {
 *           "manifestType": "S3ReaderTextManifestFile",
 *           "region": "us-east-1",
 *           "fileContents": "crawl-data/CC-MAIN-2023-50/segments/1700679099281.67/wet/CC-MAIN-20231128083443-20231128113443-00000.warc.wet.gz\n
 *                          crawl-data/CC-MAIN-2023-50/segments/1700679099281.67/wet/CC-MAIN-20231128083443-20231128113443-00001.warc.wet.gz\n
 *                          crawl-data/CC-MAIN-2023-50/segments/1700679099281.67/wet/CC-MAIN-20231128083443-20231128113443-00002.warc.wet.gz\n
 *                          crawl-data/CC-MAIN-2023-50/segments/1700679099281.67/wet/CC-MAIN-20231128083443-20231128113443-00003.warc.wet.gz",
 *           "readerType": "SPARKREADER"
 *      }
 *
 * The manifest file above specifies 4 files that need to be processed.
 *
 * MAPPER Tasks
 * ------------
 *      * Each of manifest files becomes a separate Mapper Task in LetsData.
 *      * And each Mapper Task calls this mapper interface for its file.
 *      * The mapper interface should implement any single partition operations (narrow transformations)
 *      * The dataframe returned by the Mapper Task is written to S3 as an intermediate file by LetsData.
 *      * In this example case, 4 manifest files -> 4 Mapper Tasks -> 4 intermediate files.
 *
 * REDUCER Task
 * ------------
 *      * LetsData creates a reducer task for any reduce operations for the dataset.
 *      * The intermediate files from the mapper phase are read by the reducer, any multi-partition operations, shuffles, aggregates, joins or similar wide transformations are performed on the intermediate files.
 *      * The dataset returned by the Reducer Task is written to the dataset's write destination.
 *      * In this example case, 4 intermediate files from the mapper tasks -> 1 output file by the reducer task
 *                                                                                                                   ----- +
 *     +-----------------+     +-----------------+              +----------------------+      +----------------------+     |
 *     | Manifest File 1 | --> | LetsData Task 1 | -- mapper -> | SparkMapperInterface |--->  | Intermediate File 1  |     |
 *     +-----------------+     +-----------------+              +----------------------+      +----------------------+     |
 *     +-----------------+     +-----------------+              +----------------------+      +----------------------+     |      +----------+
 *     | Manifest File 2 | --> | LetsData Task 2 | -- mapper -> | SparkMapperInterface |--->  | Intermediate File 2  |      >---> |          |
 *     +-----------------+     +-----------------+              +----------------------+      +----------------------+      >---> |  Reducer |              +-----------------------+
 *     +-----------------+     +-----------------+              +----------------------+      +----------------------+      >---> |   Task   |-- reducer -> | SparkReducerInterface | --> Output File at Write Destination
 *     | Manifest File 3 | --> | LetsData Task 3 | -- mapper -> | SparkMapperInterface |--->  | Intermediate File 3  |      >---> |          |              +-----------------------+
 *     +-----------------+     +-----------------+              +----------------------+      +----------------------+     |      +----------+
 *     +-----------------+     +-----------------+              +----------------------+      +----------------------+     |
 *     | Manifest File 4 | --> | LetsData Task 4 | -- mapper -> | SparkMapperInterface |--->  | Intermediate File 4  |     |
 *     +-----------------+     +-----------------+              +----------------------+      +----------------------+     |
 *                                                                                                                   ----- +
 *     Figure: Spark Dataset's Architecture Diagram
 *     ---------------------------------------------
 *
 * LetsData Spark Compute Engine can run in a few different configurations:
 *      * MAPPER_AND_REDUCER: Mapper tasks are created for each manifest file and a single reducer task reduces these to an output file.  (This is what is discussed above and shown in the diagram)
 *      * MAPPER_ONLY: Mapper tasks are created for each manifest file and the output files are written at the write destination (no intermediate files and no reducer tasks.)
 *      * REDUCER_ONLY: Reducer task is created and all manifest files are read by this task, reduced and output files are written to write destination.
 *
 * What to do about errors, logs, metrics and checkpointing?
 *
 *   * logger is available, and logs would be sent to cloudwatch and made available.
 *   * metrics, no metrics support within the mapper interface yet. We generate some high level metrics. We will hopefully enable these soon.
 *   * errors: the letsdata dataset errors are currently not available for spark interfaces. You decide how you want to deal with errors. (Write separate error files (using same write credentials), log them to log file etc.). Any unhandled / terminal failures can be thrown as exceptions and the task will record these and transition to error state.
 *   * checkpointing: the letsdata checkpointing and restart from checkpoints is not available yet for spark interfaces. Interfaces run either completely or fail, in which case the intermediate progress isn't used for reduce. If rerun, the tasks will run from beginning and overwrite any intermediate progress.
 */
public interface SparkMapperInterface {

    /**
     * This is the mapper interface. LetsData calls this interface with the read destination uri (s3 file link) for the tasks manifest file. User's spark transformation code is run and an output dataframe is created.
     *
     *    The code has default implementations for the following which wrap the user's spark transformation code:
     *      * the code has default credentials get
     *      * spark session setup
     *      * read the file into the dataframe method calls
     *         <user's spark transformation code>
     *      * write output files to either the intermediate S3 bucket for processing by reduce tasks or to the write destination S3 bucket.
     *
     *    These default implementations can be used as is, advanced users may want to customize these as needed.
     *
     *    For example, the following read connector and write connector parameters would be sent via different function parameters (json annotated below)
     *      "readConnector": {
     *           "connectorDestination": "S3",                     # passed as the readDestination
     *           "artifactImplementationLanguage": "python",
     *           "interfaceECRImageResourceLocation": "Customer",
     *           "interfaceECRImagePath": "151166716410.dkr.ecr.us-east-1.amazonaws.com/letsdata_python_functions:latest",
     *           "readerType": "SPARKREADER",
     *           "bucketName": "commoncrawl",
     *           "bucketResourceLocation": "Customer",
     *           "sparkFileFormat": "text",                        # passed as the readFormat
     *           "sparkReadOptions": {                             # passed as readOptions
     *                "lineSep": "\n\r\n\r\n"
     *           }
     *      }
     *
     *      "manifestFile": {
     *           "manifestType": "S3ReaderTextManifestFile",
     *           "region": "us-east-1",
     *           "fileContents": "crawl-data/CC-MAIN-2023-50/segments/1700679099281.67/wet/CC-MAIN-20231128083443-20231128113443-00000.warc.wet.gz\n     # s3a://<bucketName>/<fileName> is passed as the readUri
     *                          crawl-data/CC-MAIN-2023-50/segments/1700679099281.67/wet/CC-MAIN-20231128083443-20231128113443-00001.warc.wet.gz\n
     *                          crawl-data/CC-MAIN-2023-50/segments/1700679099281.67/wet/CC-MAIN-20231128083443-20231128113443-00002.warc.wet.gz\n
     *                          crawl-data/CC-MAIN-2023-50/segments/1700679099281.67/wet/CC-MAIN-20231128083443-20231128113443-00003.warc.wet.gz",
     *           "readerType": "SPARKREADER"
     *      }
     *
     *
     *      "writeConnector": {
     *           "connectorDestination": "S3",                     # passed as the writeDestination
     *           "resourceLocation": "LetsData",
     *           "writerType": "Spark",
     *           "sparkFileFormat": "json",                        # in case of "MAPPER_AND_REDUCER" run config, 'parquet' is passed, otherwise this value is passed as the format
     *           "sparkWriteOptions": {"compression":"gzip"}       # in case of "MAPPER_AND_REDUCER" run config, '{"compression":"gzip"}' is passed, otherwise this value is passed as the writeOptions
     *      }
     *
     * @param appName - The appName is a system generated spark app name
     * @param readDestination - The readDestination for spark mapper - currently only S3 is supported
     * @param readUri - The readUri for the readDestination (the s3 file link) that the mapper will read
     * @param readFormat - The format of the file being read by the mapper. We currently support csv, text, json and parquet. You can add additional formats, just make sure to add code in spark utils to handle these.
     * @param readOptions - The options for the spark mapper's read. For text files, we can specify the lineSep as an option. Different formats can have different options that can be specified here.
     * @param writeDestination - The readDestination for spark mapper - currently only S3 is supported.
     * @param writeUri - The writeUri for the writeDestination (the s3 file link) that the mapper will write the output file to. In case of "MAPPER_AND_REDUCER" run config, this is a system generated uri that saves the file in the intermediate bucket. In case of "MAPPER_ONLY" run configuration, an output file is written to write destination bucket
     * @param writeFormat - The format of the file being written by the mapper. In case of "MAPPER_AND_REDUCER" run config, 'parquet' is passed, otherwise this is passed as the format. We currently support csv, text, json and parquet. You can add additional formats, just make sure to add code in spark utils to handle these.
     * @param writeMode - The writeMode for spark mapper - currently defaults to 'overwrite'.
     * @param writeOptions - The options for the spark mapper's write. In case of "MAPPER_AND_REDUCER" run config, '{"compression":"gzip"}' is passed, otherwise the dataset's sparkWriteOptions are passed as the writeOptions. Different formats can have different options that can be specified here.
     * @param sparkCredentialsSecretArn - The secret manager arn for credentials for reading and writing to the read / write buckets
     * @returns - The function writes the data to write destination and does not return anything
     */
    void mapper(String appName, String readDestination, String readUri, String readFormat, Map<String, String> readOptions, String writeDestination, String writeUri, String writeFormat, String writeMode, Map<String, String> writeOptions, String sparkCredentialsSecretArn);

    /**
     * Helper function to get the credentials for reading the spark mapper read bucket from secret manager
     * @param gson
     * @param region
     * @param sparkCredentialsSecretArn
     * @return
     */
    default AWSSessionCredentials getReadDestinationCredentials(Gson gson, String region, String sparkCredentialsSecretArn) {
        return SecretManagerUtil.getSparkAwsCredentials(gson, region, sparkCredentialsSecretArn, "mapper", "read");
    }

    /**
     * Helper function to get the credentials for reading the spark mapper write bucket from secret manager
     * @param gson
     * @param region
     * @param sparkCredentialsSecretArn
     * @return
     */
    default AWSSessionCredentials getWriteDestinationCredentials(Gson gson, String region, String sparkCredentialsSecretArn) {
        return SecretManagerUtil.getSparkAwsCredentials(gson, region, sparkCredentialsSecretArn, "mapper", "write");
    }
}
