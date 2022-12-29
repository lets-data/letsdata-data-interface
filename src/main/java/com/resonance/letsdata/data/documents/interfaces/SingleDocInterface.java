package com.resonance.letsdata.data.documents.interfaces;

/**
 *  Single method interface for a Single Document (as opposed to a Composite Document)
 *  Single Documents should be used for each individual record in the file i.e. there should be 1-1 mapping between a file record and its corresponding java Single Doc Interface implementation.
 *  Composite Documents on the other hand are documents created by Single File State Machine reader and Multiple File State machine readers where they output a document from multiple single document inputs.
 *
 *  For example, the following multi file state machine reader setup shows how different parsers output different documents
 *  +-----------------------+
 *  | metadata_file1.gz     |   METADATALOG reader (Single File reader)
 *  | &lt;metadata_rec1&gt; |  ----------------------------------------> MetadataSingleDoc_rec1-----+
 *  | &lt;metadata_rec2&gt; |  ----------------------------------------> MetadataSingleDoc_rec2 ----|---+
 *  | &lt;metadata_rec3&gt; |  ----------------------------------------> MetadataSingleDoc_rec3 ----|---|---+
 *  +-----------------------+                                                                       |   |   |  Multiple File State Machine reader
 *                                                                                                  |   |   | --------------------------------------> CompositeDoc_rec3
 *                                                                                                  |   |   |
 *  +-------------------+                                                                           |   |   |  Multiple File State Machine reader
 *  | data_file1.gz     |  DATALOG reader (Single File reader)                                      | ---------------------------------------------> CompositeDoc_rec1
 *  |                   |                                                                           |   |   |
 *  | &lt;data_rec1&gt; |  --------------------------------------------> DataSingleDoc_rec1---------+   |   |  Multiple File State Machine reader
 *  | &lt;data_rec2&gt; |  --------------------------------------------> DataSingleDoc_rec2-------------+------------------------------------------> CompositeDoc_rec2
 *  |                   |                                                                                   |
 *  | &lt;data_rec3&gt; |  --------------------------------------------> DataSingleDoc_rec3-----------------+
 *  +-------------------+
 *
 *  Callouts in the example above:
 *      * a MetadataSingleDoc implementation is created from the SingleDocInterface for the  metadata records (metadata_rec) from METADATALOG file
 *      * a DataSingleDoc implementation is created from the SingleDocInterface for the data records (data_rec) from DATALOG file
 *      * a CompositeDoc implementation is created from the CompositeDocInterface for the record created from the combination of data and metadata documents
 */
public interface SingleDocInterface extends DocumentInterface {
    /**
     * Interface should return true for SingleDocInterface implementations (maybe some future SingleDoc implementations can return false)
     * @return - true for SingleDocInterface implementations
     */
    boolean isSingleDoc();
}
