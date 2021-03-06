package datawave.query.util;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import datawave.data.MetadataCardinalityCounts;
import datawave.data.type.LcNoDiacriticsType;
import datawave.data.type.Type;
import datawave.marking.MarkingFunctions;
import datawave.query.model.QueryModel;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.commons.lang.time.DateUtils;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class MockMetadataHelper extends MetadataHelper {
    protected final Metadata metadata = new Metadata();
    private Set<String> indexOnlyFields = new HashSet<>();
    private Set<String> expansionFields = new HashSet<>();
    private Set<String> contentFields = new HashSet<>();
    private Set<String> riFields = new HashSet<>();
    private Multimap<String,String> fieldsToDatatype = HashMultimap.create();
    protected Multimap<String,Type<?>> dataTypes = HashMultimap.create();
    protected Map<String,Map<String,MetadataCardinalityCounts>> termCounts = new HashMap<>();
    protected Map<String,QueryModel> models = new HashMap<>();
    protected Map<Map.Entry<String,String>,Map<String,Long>> cardinalityByDataTypeForFieldAndDate = Maps.newHashMap();
    
    Function<Type<?>,String> function = new Function<Type<?>,String>() {
        @Override
        @Nullable
        public String apply(@Nullable Type<?> input) {
            return input.getClass().getName();
        }
    };
    
    public void addContentFields(Collection<String> fields) {
        this.contentFields.addAll(fields);
    }
    
    public void addDataTypes(Collection<String> dataTypes) {
        getMetadata().datatypes.addAll(dataTypes);
    }
    
    public void addExpansionFields(Collection<String> fields) {
        this.expansionFields.addAll(fields);
    }
    
    public void addField(String field, String dt) {
        getMetadata().allFields.add(field);
        try {
            this.dataTypes.put(field, Class.forName(dt).asSubclass(Type.class).newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            // TODO: Auto-generated catch block;
            e.printStackTrace();
        }
    }
    
    public void addFields(Collection<String> fields) {
        getMetadata().allFields.addAll(fields);
        for (String field : fields) {
            this.dataTypes.put(field, new LcNoDiacriticsType());
        }
    }
    
    public void addFields(Multimap<String,Type<?>> fields) {
        getMetadata().allFields.addAll(fields.keys());
        for (Map.Entry<String,Type<?>> field : fields.entries()) {
            this.dataTypes.put(field.getKey(), field.getValue());
        }
    }
    
    public void addFieldsToDatatypes(Multimap<String,String> fieldsToDatatype) {
        getMetadata().allFields.addAll(fieldsToDatatype.keySet());
        for (Map.Entry<String,String> field : fieldsToDatatype.entries()) {
            try {
                this.dataTypes.put(field.getKey(), Class.forName(field.getValue()).asSubclass(Type.class).newInstance());
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                this.fieldsToDatatype.putAll(fieldsToDatatype);
            }
        }
    }
    
    public void addNormalizers(String field, Collection<Type<?>> normalizerSet) {
        this.dataTypes.putAll(field, normalizerSet);
    }
    
    public void addTermFrequencyFields(Collection<String> fields) {
        getMetadata().termFrequencyFields.addAll(fields);
    }
    
    @Override
    public Metadata getMetadata() {
        return metadata;
    }
    
    @Override
    public Metadata getMetadata(Set<String> ingestTypeFilter) throws TableNotFoundException, ExecutionException, MarkingFunctions.Exception {
        // TODO: filter this?
        return metadata;
    }
    
    @Override
    public Set<String> getAllFields(Set<String> ingestTypeFilter) throws TableNotFoundException {
        if (ingestTypeFilter == null || ingestTypeFilter.isEmpty()) {
            return Collections.unmodifiableSet(getMetadata().getAllFields());
        }
        Set<String> fields = new HashSet<>();
        for (Map.Entry<String,String> entry : fieldsToDatatype.entries()) {
            if (ingestTypeFilter.contains(entry.getValue())) {
                fields.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(fields);
    }
    
    @Override
    public Set<String> getIndexOnlyFields(Set<String> ingestTypeFilter) throws TableNotFoundException {
        return indexOnlyFields;
    }
    
    @Override
    public QueryModel getQueryModel(String modelTableName, String modelName, Collection<String> unevaluatedFields) throws TableNotFoundException {
        return models.get(modelName);
    }
    
    @Override
    public boolean isIndexed(String fieldName, Set<String> ingestTypeFilter) throws TableNotFoundException {
        // TODO: should try to observe the ingestTypeFilter as well
        return getMetadata().indexedFields.contains(fieldName);
    }
    
    @Override
    public boolean isReverseIndexed(String fieldName, Set<String> ingestTypeFilter) {
        return this.riFields.contains(fieldName);
    }
    
    @Override
    public Map<String,Map<String,MetadataCardinalityCounts>> getTermCounts() throws InstantiationException, IllegalAccessException, TableNotFoundException {
        return termCounts;
    }
    
    @Override
    public Map<String,Map<String,MetadataCardinalityCounts>> getTermCountsWithRootAuths() throws InstantiationException, IllegalAccessException,
                    TableNotFoundException, AccumuloSecurityException, AccumuloException {
        return termCounts;
    }
    
    @Override
    public Set<String> getAllNormalized() throws InstantiationException, IllegalAccessException, TableNotFoundException {
        return getMetadata().getNormalizedFields();
    }
    
    @Override
    public Set<Type<?>> getAllDatatypes() throws InstantiationException, IllegalAccessException, TableNotFoundException {
        return Sets.newHashSet(dataTypes.values());
    }
    
    @Override
    public Set<Type<?>> getDatatypesForField(String fieldName) throws InstantiationException, IllegalAccessException, TableNotFoundException {
        return getDatatypesForField(fieldName, null);
    }
    
    @Override
    public Set<Type<?>> getDatatypesForField(String fieldName, Set<String> ingestTypeFilter) throws InstantiationException, IllegalAccessException,
                    TableNotFoundException {
        // TODO: filter these?
        return new HashSet<>(dataTypes.get(fieldName));
    }
    
    @Override
    public TypeMetadata getTypeMetadata(Set<String> ingestTypeFilter) throws TableNotFoundException {
        TypeMetadata typeMetadata = new TypeMetadata();
        for (String fieldName : dataTypes.keySet()) {
            try {
                typeMetadata.put(fieldName, "test", Iterables.transform(getDatatypesForField(fieldName), function).iterator().next());
            } catch (InstantiationException | IllegalAccessException e) {
                // TODO: fix autogenerated block
                e.printStackTrace();
            }
        }
        return typeMetadata;
    }
    
    @Override
    public Multimap<String,Type<?>> getFieldsToDatatypes(Set<String> ingestTypeFilter) throws InstantiationException, IllegalAccessException,
                    TableNotFoundException {
        Multimap<String,Type<?>> multimap = ArrayListMultimap.create();
        for (String field : dataTypes.keySet()) {
            multimap.putAll(field, getDatatypesForField(field, ingestTypeFilter));
        }
        return multimap;
    }
    
    @Override
    public Set<String> getFieldsForDatatype(Class<? extends Type<?>> datawaveType, Set<String> ingestTypeFilter) throws TableNotFoundException {
        Set<String> fields = new HashSet<>();
        for (String field : dataTypes.keySet()) {
            for (Type<?> type : dataTypes.get(field)) {
                if (datawaveType.isInstance(type)) {
                    fields.add(field);
                    break;
                }
            }
        }
        return fields;
    }
    
    @Override
    public Set<String> getTermFrequencyFields(Set<String> ingestTypeFilter) throws TableNotFoundException {
        return getMetadata().getTermFrequencyFields();
    }
    
    @Override
    public Set<String> getIndexedFields(Set<String> ingestTypeFilter) throws TableNotFoundException {
        return getMetadata().getIndexedFields();
    }
    
    @Override
    public Set<String> getExpansionFields(Set<String> ingestTypeFilter) throws TableNotFoundException {
        return this.expansionFields;
    }
    
    @Override
    public Set<String> getContentFields(Set<String> ingestTypeFilter) throws TableNotFoundException {
        return this.contentFields;
    }
    
    @Override
    public long getCardinalityForField(String fieldName, String datatype, Date begin, Date end) throws TableNotFoundException {
        throw new UnsupportedOperationException("not imeplemented in MockMetadataHelper");
    }
    
    @Override
    public Set<String> getDatatypes(Set<String> ingestTypeFilter) throws TableNotFoundException {
        if (ingestTypeFilter == null || ingestTypeFilter.isEmpty()) {
            return getMetadata().getDatatypes();
        } else {
            Set<String> types = new HashSet<>(getMetadata().getDatatypes());
            types.retainAll(ingestTypeFilter);
            return types;
        }
    }
    
    @Override
    public Long getCountsByFieldForDays(String fieldName, Date begin, Date end, Set<String> ingestTypeFilter) {
        Preconditions.checkNotNull(fieldName);
        Preconditions.checkNotNull(begin);
        Preconditions.checkNotNull(end);
        Preconditions.checkArgument(begin.before(end));
        Preconditions.checkNotNull(ingestTypeFilter);
        
        Date truncatedBegin = DateUtils.truncate(begin, Calendar.DATE);
        Date truncatedEnd = DateUtils.truncate(end, Calendar.DATE);
        
        if (truncatedEnd.getTime() != end.getTime()) {
            // If we don't have the same time for both, we actually tuncated the end,
            // and we want to bump the date range to include the entire end.
            truncatedEnd = new Date(truncatedEnd.getTime() + 86400000);
        }
        
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(truncatedBegin);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        long sum = 0l;
        while (cal.getTime().before(truncatedEnd)) {
            Date curDate = cal.getTime();
            String desiredDate = sdf.format(curDate);
            
            sum += getCountsByFieldInDayWithTypes(fieldName, desiredDate, ingestTypeFilter);
            cal.add(Calendar.DATE, 1);
        }
        
        return sum;
    }
    
    @Override
    public Long getCountsByFieldInDayWithTypes(String fieldName, String date, final Set<String> datatypes) {
        Preconditions.checkNotNull(fieldName);
        Preconditions.checkNotNull(date);
        Preconditions.checkNotNull(datatypes);
        
        Map<String,Long> countsByType = this.cardinalityByDataTypeForFieldAndDate.get(Maps.immutableEntry(fieldName, date));
        
        if (null == countsByType) {
            return 0L;
        }
        
        Iterable<Map.Entry<String,Long>> filteredByType = Iterables.filter(countsByType.entrySet(), new Predicate<Map.Entry<String,Long>>() {
            @Override
            public boolean apply(Map.Entry<String,Long> input) {
                return datatypes.contains(input.getKey());
            }
        });
        
        long sum = 0;
        for (Map.Entry<String,Long> entry : filteredByType) {
            sum += entry.getValue();
        }
        
        return sum;
    }
    
    @Override
    protected Multimap<String,String> loadAllFields() throws TableNotFoundException {
        return HashMultimap.create();
    }
    
    @Override
    protected Multimap<String,String> loadIndexOnlyFields() throws TableNotFoundException {
        return super.loadIndexOnlyFields();
    }
    
    @Override
    protected Multimap<String,String> loadTermFrequencyFields() throws TableNotFoundException {
        return super.loadTermFrequencyFields();
    }
    
    public void setCardinalities(Map<Map.Entry<String,String>,Map<String,Long>> cardinalities) {
        this.cardinalityByDataTypeForFieldAndDate = cardinalities;
    }
    
    public void setDataTypes(Multimap<String,Type<?>> dataTypes) {
        this.dataTypes = dataTypes;
    }
    
    public void setIndexedFields(Set<String> indexedFields) {
        getMetadata().indexedFields = indexedFields;
    }
    
    public void setIndexOnlyFields(Set<String> indexOnlyFields) {
        this.indexOnlyFields = indexOnlyFields;
    }
    
    public void setNormalizedFields(Set<String> normalizedFields) {
        getMetadata().normalizedFields = normalizedFields;
    }
    
    public void setQueryModel(String modelName, QueryModel model) {
        models.put(modelName, model);
    }
    
    public void setReverseIndexFields(Set<String> riFields) {
        this.riFields.addAll(riFields);
    }
    
    public void setTermCounts(Map<String,Map<String,MetadataCardinalityCounts>> counts) {
        this.termCounts = counts;
    }
    
}
