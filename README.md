# LetsData Java Interface
The letsdata data interfaces package for the java language.

The #Let'sData infrastructure simplifies the data processing for the customers. Our promise to the customers is that "Focus on the data - we'll manage the infrastructure".

The #Let'sData infrastructure implements a control plane for the data tasks, reads and writes to different destinations, scales the processing infrastructure as needed, builds in monitoring and instrumentation. 

However, it does not know what the data is, how it is stored in the data files and how to read the data and create transformed documents. This is where the #Let'sData infrastructure requires the customer code.
The customer needs to implement user data handlers that tell us what makes a data record in the files - we'll then send the records to these data handlers where the user can transform the records into documents that can be sent to the write destination. This requires implementation of the user data handler interfaces. 

This package defines these user data handler interfaces for java language.

(You can also view the # Let's Data docs to see how to implement different usecases, examples and more: https://www.letsdata.io/docs)

## Documents
#Let's Data defines the following data model class / interfaces for the transformed document implementations that are required in data processing. These are defined in the namespace `com.resonance.letsdata.data.documents.interfaces` and defined as follows:

* **DocumentInterface**: The `com.resonance.letsdata.data.documents.interfaces.DocumentInterface` is the base interface for any document that can be returned by the user handlers. All other document interfaces and documents either extend or implement this interface.
* **SingleDocInterface**: The `com.resonance.letsdata.data.documents.interfaces.SingleDocInterface` extends the "DocumentInterface" is the base interface for any documents that are transformed from single records and are returned by the user handlers. The java doc on the interface below explain these in detail.
* **CompositeDocInterface**: The `com.resonance.letsdata.data.documents.interfaces.CompositeDocInterface` extends the "DocumentInterface" is the base interface for any documents that are composited from multiple single docs and are returned by the user handlers. The java doc on the interface below explain these in detail.
* **ErrorDocInterface**: The `com.resonance.letsdata.data.documents.interfaces.ErrorDocInterface` extends the "DocumentInterface" is the base interface for any error documents that are returned by the user handlers. A default implementation for the interface is provided at `com.resonance.letsdata.data.documents.implementation.ErrorDoc` which is used by default. Customers can return errors from handlers using this default implementation or write their own Error docs and return these during processing.
* **SkipDocInterface**: The `com.resonance.letsdata.data.documents.interfaces.SkipDocInterface` extends the "DocumentInterface" is the base interface for any skip documents that are returned by the user handlers. A skip document is returned when the processor determines that the record from the file is not of interest to the current processor and should be skipped from being written to the write destination. A default implementation for the interface is provided at `com.resonance.letsdata.data.documents.implementation.SkipDoc` which is used by default. Customers can return skip records from handlers using this default implementation or write their own Skip docs and return these during processing.
## Parsers Interfaces
* **S3 - SingleFileParser**: The parser interface for "Single File" reader usecase. This is where you tell us how to parse the individual records from the file. Since this is single file reader, there is no state machine maintained.
* **S3 - SingleFileStateMachineParser**: The parser interface for "Single File State Machine" reader usecase. This is where you tell us how to parse the different records from a file. This class maintains the overall state machine for the file parser. It will create the extracted document from different file records that are being read from the files.
## Reader Interfaces
* **S3 - SingleFileStateMachineReader**: The SingleFileStateMachineReader implements the logic to combine the individual records parsed by the SingleFileStateMachine parser and output them to a composite doc. For example, if we have a DATAFILE which contains 2 types of records {metadata record, data record} and the output doc is constructed by the combining these two docs, then the SingleFileStateMachineReader combines each {metadata, data} record pair into an output doc.
* **S3 - MultipleFileStateMachineReader**: The reader interface for "Multiple File State Machine" reader. This is where you tell us how to make sense of the individual records that are parsed from multiple files. This class would maintain the overall state machine across the files. It will create the extracted document from different file records that are being read from the files.
* **Kinesis - KinesisRecordReader**: The `com.resonance.letsdata.data.readers.interfaces.kinesis.KinesisRecordReader` is the parser interface for processing a kinesis record. This is where you transform a Kinesis record to a document.
* **DynamoDB Streams - DynamoDBStreamsRecordReader**: The `com.resonance.letsdata.data.readers.interfaces.dynamodbstreams.DynamoDBStreamsRecordReader` is the parser interface for processing a dynamodb streams record. This is where you transform a DynamoDB Streams record to a document.
* **DynamoDB Table - DynamoDBTableItemReader**: The `com.resonance.letsdata.data.readers.interfaces.dynamodb.DynamoDBTableItemReader` is the parser interface for processing a dynamodb table item. This is where you transform a DynamoDB Item to a document.
* **SQS - QueueMessageReader**: The `com.resonance.letsdata.data.readers.interfaces.sqs.QueueMessageReader` is the parser interface for processing an sqs message. This is where you transform an sqs message to a document.
* **Sagemaker - SagemakerVectorsInterface**: The `com.resonance.letsdata.data.readers.interfaces.sagemaker` is the interface for processing documents for AWS Sagemaker vector embeddings generation. This is where you extract the document that needs vectorizationfrom the feature doc in `extractDocumentElementsForVectorization` and construct an output doc from the vectors in `constructVectorDoc`.
* **Spark - SparkMapperInterface**: The `com.resonance.letsdata.data.readers.interfaces.spark.SparkMapperInterface` is the spark mapper interface. Dataset's each read manifest file entry is mapped to a single mapper partition. This interface should implement any single partition operations and then return a dataframe which will be written to S3 as an intermediate file. The intermediate file forms the input for the reducer phase.
* **Spark - SparkReducerInterface**: The `com.resonance.letsdata.data.readers.interfaces.spark.SparkReducerInterface` is the interface for any reduce operations that need to be done by the spark job. Its input is the intermediate files from the mapper step and any reduced dataframes are written to the write destination.
## Utils
* **SparkUtils:** Spark users should look at `com.resonance.letsdata.data.util.SparkUtils` which has the spark common code to create a spark session, read from the read destination (S3) and write to the write destination. The default implementations should work well as is out of the box. Advanced users may want to customize these as needed.
* **SecretManagerUtil:** `com.resonance.letsdata.data.util.SecretManagerUtil` Util to retrieve secrets from AWS Secrets Manager. This is used internally by the system and users should not have to use it directly. 
* **Matcher:** `com.resonance.letsdata.data.util.Matcher` - Efficient string / pattern searching utility (Boyer-Moore algo)
## End to End Examples
Do look at our end to end examples on the LetsData website which have a step by step instructions for data processing examples using LetsData datasets.
* **Spark Extract and Map Reduce:**  Reads files (web crawl archive files) from S3 using Spark code and extracts the web crawl header and the web page content as a LetsData Document. It then map reduces these documents using Spark to compute the 90th percentile contentLength grouped by language and writes the results as a json document to S3. [Spark Map and Reduce Example](https://www.letsdata.io/docs/examples?tab=spark-extractandmapreduce)
* **Extract Uris from S3 WebCrawl Archives:** Reads files (web crawl archive files) from S3 and extracts the urls that were crawled. It then writes these urls as a string in Kinesis/Kafka. It uses AWS Lambda compute to run this dataset. Errors are written to S3. [Extract Uris from S3 WebCrawl Archives](https://www.letsdata.io/docs/examples?tab=uriextractor)
* **Generate Vector Embeddings Using Lambda and Sagemaker Compute Engine:**  Reads the web crawl archive files from S3 and extracts the contents as documents that can be indexed (feature doc). It then implementats the LetsData SagemakerVectorsInterface - which 1./ extract the text that needs to be vectorized from the feature doc 2./ construct an output document (vector document) from the feature doc and the sagemaker vectors. It uses an existing Hugging Face model (Sentence Transformer) for computing vectors. Vector document writes to Kinesis stream. [Generate Vector Embeddings Using Lambda and Sagemaker Compute Engine](https://www.letsdata.io/docs/examples?tab=generatevectorembeddings)
  
## Interfaces - Example Implementations
Examples implementations for all these interfaces are available in the [letsdata-common-crawl](https://github.com/lets-data/letsdata-common-crawl) GitHub Repo. They are also linked for each interface in the following section (Usecases - Implementation Requirements).

## Usecases - Implementation Requirements 
* **S3 - Single File Reader:** You'll need to implement the `SingleFileReaderParserInterface`. 
  * [Single File Reader Docs](https://www.letsdata.io/docs/sdk-interface/#singleFileReaderInterfaceRequirementDetailsContainer) 
  * [Single File Reader Example Interface Implementation](https://github.com/lets-data/letsdata-examples-target-uri-extractor/blob/main/src/main/java/com/letsdata/example/TargetUriExtractor.java)
* **Spark Reader:** Spark Compute Engine configuration requires a runSparkInterfaces attribute - when this value is set to `MAPPER_AND_REDUCER`, you'll need to implement `SparkMapperInterface` and `SparkReducerInterface`. When `runSparkInterfaces: MAPPER_ONLY`, implement the `SparkMapperInterface` only. For `runSparkInterfaces: REDUCER_ONLY`, implement the `SparkReducerInterface` only.
  * [Spark Reader Docs](https://www.letsdata.io/docs/sdk-interface/#sparkReaderInterfaceRequirementDetailsContainer)
  * [SparkMapperInterface - Example Interface Implementation](https://github.com/lets-data/letsdata-common-crawl/blob/main/src/main/java/com/letsdata/commoncrawl/interfaces/implementations/spark/CommonCrawlSparkMapper.java)
  * [SparkReducerInterface - Example Interface Implementation](https://github.com/lets-data/letsdata-common-crawl/blob/main/src/main/java/com/letsdata/commoncrawl/interfaces/implementations/spark/CommonCrawlSparkReducer.java)
* **SQS Queue Reader:** You'll need to implement the `QueueMessageReader` interface.
  * [SQS Queue Reader Docs](https://www.letsdata.io/docs/sdk-interface/#sqsQueueReaderInterfaceRequirementDetailsContainer)
  * [QueueMessageReader - Example Interface Implementation](https://github.com/lets-data/letsdata-common-crawl/blob/main/src/main/java/com/letsdata/commoncrawl/interfaces/implementations/sqsqueuereader/CommonCrawlQueueReader.java)
* **Sagemaker Vectors Interface:** You'll need to implement the `SagemakerVectorsInterface` interface.
  * [Sagemaker Vectors Interface Docs](https://www.letsdata.io/docs/sdk-interface/#sagemakerVectorsInterfaceRequirementDetailsContainer)
  * [SagemakerVectorsInterface - Example Interface Implementation](https://github.com/lets-data/letsdata-common-crawl/blob/main/src/main/java/com/letsdata/commoncrawl/interfaces/implementations/sagemaker/CommonCrawlSagemakerReader.java)
* **Kinesis Stream Reader:** You'll need to implement the `KinesisRecordReader` interface.
  * [Kinesis Stream Reader Interface Docs](https://www.letsdata.io/docs/sdk-interface/#sagemakerVectorsInterfaceRequirementDetailsContainer)
  * [KinesisRecordReader - Example Interface Implementation](https://github.com/lets-data/letsdata-common-crawl/blob/main/src/main/java/com/letsdata/commoncrawl/interfaces/implementations/kinesisstreamreader/CommonCrawlStreamReader.java)
* **DynamoDB Streams Reader:** You'll need to implement the `DynamoDBStreamsRecordReader` interface.
  * [DynamoDB Streams Reader Interface Docs](https://www.letsdata.io/docs/sdk-interface/#dynamoDBStreamReaderRequirementDetailsContainer)
  * [DynamoDBStreamsRecordReader - Example Interface Implementation](https://github.com/lets-data/letsdata-common-crawl/blob/main/src/main/java/com/letsdata/commoncrawl/interfaces/implementations/dynamodbstreams/CommonCrawlDDBStreamReader.java)
* **DynamoDB Table Reader:** You'll need to implement the `DynamoDBTableItemReader` interface.
  * [DynamoDB Table Reader Interface Docs](https://www.letsdata.io/docs/sdk-interface/#dynamoDBTableReaderRequirementDetailsContainer)
  * [DynamoDBTableItemReader - Example Interface Implementation](https://github.com/lets-data/letsdata-common-crawl/blob/main/src/main/java/com/letsdata/commoncrawl/interfaces/implementations/dynamodb/CommonCrawlDDBItemReader.java)
* **S3 - Single File State Machine Reader:** You'll need to implement the `SingleFileStateMachineParser` interface (to parse individual records from file and maintain a state machine) & `SingleFileStateMachineReader` interface (to output a composite doc from the parsed records)
    * [Single File State Machine Reader Interface Docs](https://www.letsdata.io/docs/sdk-interface/#dynamoDBTableReaderRequirementDetailsContainer)
    * [SingleFileStateMachineParser - Example Interface Implementation](https://github.com/lets-data/letsdata-common-crawl/blob/main/src/main/java/com/letsdata/commoncrawl/interfaces/implementations/parser/WARCFileParser.java)
    * [SingleFileStateMachineReader - Example Interface Implementation](https://github.com/lets-data/letsdata-common-crawl/blob/main/src/main/java/com/letsdata/commoncrawl/interfaces/implementations/reader/CommonCrawlWARCSingleFileStateMachineReader.java)
* **S3 - Multiple File State Machine Reader:** You'll need to implement either the `SingleFileParser` or `SingleFileStateMachineParser` interface for each file. If the records in the file are a single record type and do not follow a state machine, use the `SingleFileParser` interface. If there are multiple record types in the file that follow a state machine, use the `SingleFileStateMachineParser` interface. You'll also need to implement `MultipleFileStateMachineReader` interface - this will combine the records returned by the individual file parsers and produce a Composite document. It will also maintain the state machine across files - you'll be adding logic to get the next records from each file and combining them into the result doc.
    * [Multiple File State Machine Reader Interface Docs](https://www.letsdata.io/docs/sdk-interface/#dynamoDBTableReaderRequirementDetailsContainer)
    * [SingleFileParser - Example Interface Implementation](https://github.com/lets-data/letsdata-examples-target-uri-extractor/blob/main/src/main/java/com/letsdata/example/TargetUriExtractor.java)
    * [SingleFileStateMachineParser - Example Interface Implementation](https://github.com/lets-data/letsdata-common-crawl/blob/main/src/main/java/com/letsdata/commoncrawl/interfaces/implementations/parser/WARCFileParser.java)
    * [MultipleFileStateMachineReader - Example Interface Implementation](https://github.com/lets-data/letsdata-common-crawl/blob/main/src/main/java/com/letsdata/commoncrawl/interfaces/implementations/reader/CommonCrawlReader.java)

## How to build and import this package for dev environments
* Assuming maven is installed and in the PATH - run the command `mvn clean compile assembly:single package` - This produces 3 artifacts: a single jar, a single jar with dependencies and a sources jar
* Install the maven file in the local maven repo
```
mvn -e install:install-file -Dfile=target/letsdata-data-interface-1.0-SNAPSHOT-jar-with-dependencies.jar -Dsources=target/letsdata-data-interface-1.0-SNAPSHOT-sources.jar -DgroupId=com.resonance.letsdata -DartifactId=letsdata-data-interface -Dpackaging=jar -Dversion=1.0-SNAPSHOT
```
* Now import this in your project with the following maven dependency
```
<dependency>
    <groupId>com.resonance.letsdata</groupId>
    <artifactId>letsdata-data-interface</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```
* See the [quickstart](https://www.letsdata.io/docs/#quickstart) for additional details or follow a [step by step detailed example](https://www.letsdata.io/docs/examples/).
## References:
* **LetsData:** LetsData links for learning about datasets, creating dataset configurations, access grants, examples and the sdk docs.
  * **Datasets:** https://www.letsdata.io/docs/datasets/
  * **Read Connectors:** https://www.letsdata.io/docs/read-connectors/
  * **Write Connectors:** https://www.letsdata.io/docs/write-connectors/
  * **Compute Engine:** https://www.letsdata.io/docs/compute-engine/
  * **Access Grants:** https://www.letsdata.io/docs/access-grants/
  * **Examples:** https://www.letsdata.io/docs/examples/
  * **SDK Interface:** https://www.letsdata.io/docs/sdk-interface/
