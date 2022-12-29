package com.resonance.letsdata.data.readers.interfaces;

import com.resonance.letsdata.data.documents.interfaces.DocumentInterface;
import com.resonance.letsdata.data.readers.model.SingleFileReaderState;

import java.util.Map;

public interface SystemFileReader {
    DocumentInterface nextRecord(boolean peek) throws Exception;
    SingleFileReaderState getState();
    Map<String, Long> getOffset();
    Map<String, String> getFileTypeFileNameMap();
    Map<String, String> getLastRecordTypeMap();
}
