package com.resonance.letsdata.data.readers.interfaces.sagemaker;

import com.resonance.letsdata.data.documents.interfaces.DocumentInterface;

import java.util.Map;

public interface SagemakerVectorsInterface {
    /**
     * The document that is read from the read destination might have some fields that need vectorization while others are probably identifiers that do not need vectorization.
     * This interface is where you tell on the parts of the documents that need vectorization.
     *
     * For example, the following document for a html page might need vectorization on the text fields but can skip fields such as id, url etc.
     *      {
     *          "id": "318792f5-8bcb-4a77-b362-087520efb49c",
     * 	        "url": "www.cnn.com",
     * 	        "title": "Breaking News, Latest News and Videos | CNN",
     * 	        "description": "View the latest news and breaking news today for U.S., world, weather, entertainment, politics and health at CNN.com.",
     * 	        "keywords": "cnn news, daily news, breaking news, news today, current events",
     * 	        "docText": "Article: US inflation means families are spending more than two years ago. The typical American household spent more in July than they did two years ago to buy the same goods and services, according to recent surveys. That figure underscores the cumulative impact high inflation has had on consumer finances — even as price growth has cooled considerably in recent months."
     *      }
     *
     * The extract function will return only the fields that need vectorization with friendly names that can be used to identify different vectors. Here is an example output:
     *      {
     *          "PageTitle": "Breaking News, Latest News and Videos | CNN",
     * 	        "PageDescription": "View the latest news and breaking news today for U.S., world, weather, entertainment, politics and health at CNN.com.",
     * 	        "PageKeywords": "cnn news, daily news, breaking news, news today, current events",
     * 	        "PageText": "Article: US inflation means families are spending more than two years ago. The typical American household spent more in July than they did two years ago to buy the same goods and services, according to recent surveys. That figure underscores the cumulative impact high inflation has had on consumer finances — even as price growth has cooled considerably in recent months."
     *      }
     *
     * @param documentInterface
     * @return
     */
    Map<String /*friendlyName*/, String /*content*/> extractDocumentElementsForVectorization(DocumentInterface documentInterface);

    /**
     * LetsData will vectorize each of the contents returned by the extract document. These vectors are then run by this interface so that:
     *      * The original document can be updated with these vectorizations if needed
     *      or
     *      * a new vector document is created that can be stored in the vector database. This might mean adding back the columns such as ids, url etc to identify what the vector index is for.
     *
     * Continuing the example from the extract function, the arguments to the function are as follows:
     *      documentInterface:
     *      {
     *          "id": "318792f5-8bcb-4a77-b362-087520efb49c",
     * 	        "url": "www.cnn.com",
     * 	        "title": "Breaking News, Latest News and Videos | CNN",
     * 	        "description": "View the latest news and breaking news today for U.S., world, weather, entertainment, politics and health at CNN.com.",
     * 	        "keywords": "cnn news, daily news, breaking news, news today, current events",
     * 	        "docText": "Article: US inflation means families are spending more than two years ago. The typical American household spent more in July than they did two years ago to buy the same goods and services, according to recent surveys. That figure underscores the cumulative impact high inflation has had on consumer finances — even as price growth has cooled considerably in recent months."
     *      }
     *
     *      vectorsMap:
     *      {
     *          "PageTitle": [58.7, 45.6, 59.1, 75.1] ,
     * 	        "PageDescription": [27.6, 12, 9.8, 19, 96, 1.2, 11.3, 9.8, 29.8, 22.3, 22.4, 82.9, 27.5, 12.4, 11.5, 80.3, 30.4],
     * 	        "PageKeywords": [31.8, 48.4, 2.4, 98.3, 28.4, 55.5, 15.3, 79.3, 81, 12, 56.4, 56, 13.7, 46.4, 2.9, 46.5, 65.2, 70.7, 13.1, 59.8, 63.8, 22.4, 15.2]
     * 	        "PageText": [96.3, 79.8, 18.2, 37.8, 85.6, 83.6, 68.3, 54.5, 95.9, 64, 32.6, 36.4, 68, 93.1, 40.2, 61, 98.2, 52.7, 26.2, 31.1, 26.9, 35.2, 16.8, 25.3, 97.7, 81.6, 87.9, 21.5, 72.4, 12.1, 27.4, 83.9, 81.9, 60.1, 5.5]
     *      }
     *
     * The function could return either the of the FEATURE or VECTOR docs, which will be stored in the write destination. How you decide to implement this is upto you.
     *
     * * FEATURE Doc: We define FEATURE doc as the original document with the vectors added to the doc. See the following example where the vectors are added to the original document interface.
     *      {
     *          "id": "318792f5-8bcb-4a77-b362-087520efb49c",
     *          "url": "www.cnn.com",
     *          "title": "Breaking News, Latest News and Videos | CNN",
     *          "description": "View the latest news and breaking news today for U.S., world, weather, entertainment, politics and health at CNN.com.",
     *          "keywords": "cnn news, daily news, breaking news, news today, current events",
     *          "docText": "Article: US inflation means families are spending more than two years ago. The typical American household spent more in July than they did two years ago to buy the same goods and services, according to recent surveys. That figure underscores the cumulative impact high inflation has had on consumer finances — even as price growth has cooled considerably in recent months."
     *          "vectors": {
     *              "PageTitle": [58.7, 45.6, 59.1, 75.1] ,
     *              "PageDescription": [27.6, 12, 9.8, 19, 96, 1.2, 11.3, 9.8, 29.8, 22.3, 22.4, 82.9, 27.5, 12.4, 11.5, 80.3, 30.4],
     *              "PageKeywords": [31.8, 48.4, 2.4, 98.3, 28.4, 55.5, 15.3, 79.3, 81, 12, 56.4, 56, 13.7, 46.4, 2.9, 46.5, 65.2, 70.7, 13.1, 59.8, 63.8, 22.4, 15.2]
     *              "PageText": [96.3, 79.8, 18.2, 37.8, 85.6, 83.6, 68.3, 54.5, 95.9, 64, 32.6, 36.4, 68, 93.1, 40.2, 61, 98.2, 52.7, 26.2, 31.1, 26.9, 35.2, 16.8, 25.3, 97.7, 81.6, 87.9, 21.5, 72.4, 12.1, 27.4, 83.9, 81.9, 60.1, 5.5]
     *          }
     *      }
     *
     * * VECTOR Doc: We define VECTOR doc as a new document that has the vectors, id and the write destination specific keys etc. You could include any additional fields that are required.
     *      {
     *          "id": "318792f5-8bcb-4a77-b362-087520efb49c",
     *          "url": "www.cnn.com",
     *          "PageTitle": [58.7, 45.6, 59.1, 75.1] ,
     *          "PageDescription": [27.6, 12, 9.8, 19, 96, 1.2, 11.3, 9.8, 29.8, 22.3, 22.4, 82.9, 27.5, 12.4, 11.5, 80.3, 30.4],
     *          "PageKeywords": [31.8, 48.4, 2.4, 98.3, 28.4, 55.5, 15.3, 79.3, 81, 12, 56.4, 56, 13.7, 46.4, 2.9, 46.5, 65.2, 70.7, 13.1, 59.8, 63.8, 22.4, 15.2]
     *	        "PageText": [96.3, 79.8, 18.2, 37.8, 85.6, 83.6, 68.3, 54.5, 95.9, 64, 32.6, 36.4, 68, 93.1, 40.2, 61, 98.2, 52.7, 26.2, 31.1, 26.9, 35.2, 16.8, 25.3, 97.7, 81.6, 87.9, 21.5, 72.4, 12.1, 27.4, 83.9, 81.9, 60.1, 5.5]
     *      }
     *
     * @param documentInterface
     * @param vectorsMap
     * @return
     */
    DocumentInterface constructVectorDoc(DocumentInterface documentInterface, Map<String /*friendlyName*/, Double[] /*Vector*/> vectorsMap);
}
