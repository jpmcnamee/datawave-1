package datawave.ingest.mapreduce;

import datawave.data.hash.HashUID;
import datawave.data.hash.UID;
import datawave.data.hash.UIDBuilder;
import datawave.ingest.data.RawRecordContainer;
import datawave.ingest.data.Type;
import datawave.ingest.data.TypeRegistry;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.hadoop.io.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

/**
 * A POJO implementation of the {@link datawave.ingest.data.RawRecordContainer}.
 */
public class SimpleRawRecord implements RawRecordContainer, Writable {
    
    private UIDBuilder<UID> uidBuilder = HashUID.builder();
    
    private Map<String,String> securityMarkings = new TreeMap<>();
    private UID id = uidBuilder.newId();
    private Type dataType;
    private long date = 0;
    private Collection<String> errors = Collections.emptyList();
    private Collection<String> altIds = Collections.emptyList();
    private String rawFileName = "";
    private long rawRecordNumber = 0;
    private long rawRecordTimestamp = 0;
    private byte[] rawData = new byte[0];
    private Object auxData;
    private ColumnVisibility visibility;
    
    @Override
    public Map<String,String> getSecurityMarkings() {
        return securityMarkings;
    }
    
    @Override
    public void setSecurityMarkings(Map<String,String> securityMarkings) {
        this.securityMarkings = securityMarkings;
    }
    
    @Override
    public void addSecurityMarking(String domain, String marking) {
        securityMarkings.put(domain, marking);
    }
    
    @Override
    public boolean hasSecurityMarking(String domain, String marking) {
        return marking.equals(securityMarkings.get(domain));
    }
    
    @Override
    public UID getId() {
        return id;
    }
    
    @Override
    public void setId(UID id) {
        this.id = id;
    }
    
    @Override
    public void generateId(String extra) {
        setId(uidBuilder.newId(rawData, getTimeForUID(), extra));
    }
    
    @Override
    public Type getDataType() {
        return dataType;
    }
    
    @Override
    public void setDataType(Type dataType) {
        this.dataType = dataType;
    }
    
    @Override
    public long getDate() {
        return date;
    }
    
    @Override
    public void setDate(long date) {
        this.date = date;
    }
    
    @Override
    public Collection<String> getErrors() {
        return errors;
    }
    
    @Override
    public void setErrors(Collection<String> errors) {
        this.errors = errors;
    }
    
    @Override
    public void addError(String error) {
        this.errors.add(error);
    }
    
    @Override
    public void removeError(String error) {
        this.errors.remove(error);
    }
    
    @Override
    public boolean hasError(String error) {
        return this.errors.contains(error);
    }
    
    @Override
    public boolean fatalError() {
        return false;
    }
    
    @Override
    public boolean ignorableError() {
        return false;
    }
    
    @Override
    public void clearErrors() {
        this.errors.clear();
    }
    
    @Override
    public Collection<String> getAltIds() {
        return altIds;
    }
    
    @Override
    public void setAltIds(Collection<String> altIds) {
        this.altIds = altIds;
    }
    
    @Override
    public void addAltId(String altId) {
        altIds.add(altId);
    }
    
    @Override
    public boolean hasAltId(String altId) {
        return altIds.contains(altId);
    }
    
    @Override
    public String getRawFileName() {
        return rawFileName;
    }
    
    @Override
    public void setRawFileName(String rawFileName) {
        this.rawFileName = rawFileName;
    }
    
    @Override
    public long getRawRecordNumber() {
        return rawRecordNumber;
    }
    
    @Override
    public void setRawRecordNumber(long rawRecordNumber) {
        this.rawRecordNumber = rawRecordNumber;
    }
    
    @Override
    public long getRawFileTimestamp() {
        return rawRecordTimestamp;
    }
    
    @Override
    public void setRawFileTimestamp(long rawRecordTimestamp) {
        this.rawRecordTimestamp = rawRecordTimestamp;
    }
    
    @Override
    public byte[] getRawData() {
        return rawData;
    }
    
    @Override
    public void setRawData(byte[] rawData) {
        this.rawData = rawData;
    }
    
    @Override
    public Object getAuxData() {
        return auxData;
    }
    
    @Override
    public void setAuxData(Object auxData) {
        this.auxData = auxData;
    }
    
    @Override
    public RawRecordContainer copy() {
        throw new UnsupportedOperationException("Copy not yet implemented on " + this.getClass());
    }
    
    @Override
    public long getDataOutputSize() {
        return 0;
    }
    
    @Override
    public void write(DataOutput dataOutput) throws IOException {
        TestWritableUtil.writeMap(securityMarkings, dataOutput);
        
        id.write(dataOutput);
        dataOutput.writeUTF(dataType.typeName());
        dataOutput.writeLong(date);
        
        TestWritableUtil.writeCollection(errors, dataOutput);
        TestWritableUtil.writeCollection(altIds, dataOutput);
        
        dataOutput.writeUTF(rawFileName);
        dataOutput.writeLong(rawRecordNumber);
        dataOutput.writeLong(rawRecordTimestamp);
        
        dataOutput.writeInt(rawData.length);
        dataOutput.write(rawData);
        
        // skipping auxData and visibility for now
    }
    
    @Override
    public void readFields(DataInput dataInput) throws IOException {
        securityMarkings = TestWritableUtil.readMap(dataInput);
        
        id = uidBuilder.newId();
        id.readFields(dataInput);
        
        dataType = TypeRegistry.getType(dataInput.readUTF());
        date = dataInput.readLong();
        
        errors = TestWritableUtil.readCollection(dataInput);
        altIds = TestWritableUtil.readCollection(dataInput);
        
        rawFileName = dataInput.readUTF();
        rawRecordNumber = dataInput.readLong();
        rawRecordTimestamp = dataInput.readLong();
        
        int len = dataInput.readInt();
        rawData = new byte[len];
        dataInput.readFully(rawData);
    }
    
    @Override
    public ColumnVisibility getVisibility() {
        return visibility;
    }
    
    @Override
    public void setVisibility(ColumnVisibility visibility) {
        this.visibility = visibility;
    }
    
    @Override
    public Date getTimeForUID() {
        return new Date(rawRecordTimestamp);
    }
    
    @Override
    public boolean isRequiresMasking() {
        return false;
    }
    
    @Override
    public void clear() {
        
    }
}
