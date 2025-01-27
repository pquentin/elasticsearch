/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.mapper;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.bytes.BytesReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Fork of {@link org.apache.lucene.document.Document} with additional functionality.
 */
public class LuceneDocument implements Iterable<IndexableField> {

    private final LuceneDocument parent;
    private final String path;
    private final String prefix;
    private final List<IndexableField> fields;
    private Map<Object, IndexableField> keyedFields;
    /**
     * A sorted map of the serialized values of dimension fields that will be used
     * for generating the _tsid field. The map will be used by {@link TimeSeriesIdFieldMapper}
     * to build the _tsid field for the document.
     */
    private SortedMap<BytesRef, BytesReference> dimensionBytes;

    LuceneDocument(String path, LuceneDocument parent) {
        fields = new ArrayList<>();
        this.path = path;
        this.prefix = path.isEmpty() ? "" : path + ".";
        this.parent = parent;
    }

    public LuceneDocument() {
        this("", null);
    }

    /**
     * Return the path associated with this document.
     */
    public String getPath() {
        return path;
    }

    /**
     * Return a prefix that all fields in this document should have.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Return the parent document, or null if this is the root document.
     */
    public LuceneDocument getParent() {
        return parent;
    }

    @Override
    public Iterator<IndexableField> iterator() {
        return fields.iterator();
    }

    public List<IndexableField> getFields() {
        return fields;
    }

    public void addAll(List<? extends IndexableField> fields) {
        this.fields.addAll(fields);
    }

    public void add(IndexableField field) {
        // either a meta fields or starts with the prefix
        assert field.name().startsWith("_") || field.name().startsWith(prefix) : field.name() + " " + prefix;
        fields.add(field);
    }

    /**
     * Add fields so that they can later be fetched using {@link #getByKey(Object)}.
     */
    public void addWithKey(Object key, IndexableField field) {
        if (keyedFields == null) {
            keyedFields = new HashMap<>();
        } else if (keyedFields.containsKey(key)) {
            throw new IllegalStateException("Only one field can be stored per key");
        }
        keyedFields.put(key, field);
        add(field);
    }

    /**
     * Get back fields that have been previously added with {@link #addWithKey(Object, IndexableField)}.
     */
    public IndexableField getByKey(Object key) {
        return keyedFields == null ? null : keyedFields.get(key);
    }

    /**
     * Add the serialized byte reference for a dimension field. This will be used by {@link TimeSeriesIdFieldMapper}
     * to build the _tsid field for the document.
     */
    public void addDimensionBytes(String fieldName, BytesReference tsidBytes) {
        BytesRef fieldNameBytes = new BytesRef(fieldName);
        if (dimensionBytes == null) {
            // It is a {@link TreeMap} so that it is order by field name.
            dimensionBytes = new TreeMap<>();
        } else if (dimensionBytes.containsKey(fieldNameBytes)) {
            throw new IllegalArgumentException("Dimension field [" + fieldName + "] cannot be a multi-valued field.");
        }
        dimensionBytes.put(fieldNameBytes, tsidBytes);
    }

    public SortedMap<BytesRef, BytesReference> getDimensionBytes() {
        if (dimensionBytes == null) {
            return Collections.emptySortedMap();
        }
        return dimensionBytes;
    }

    public IndexableField[] getFields(String name) {
        List<IndexableField> f = new ArrayList<>();
        for (IndexableField field : fields) {
            if (field.name().equals(name)) {
                f.add(field);
            }
        }
        return f.toArray(new IndexableField[f.size()]);
    }

    public IndexableField getField(String name) {
        for (IndexableField field : fields) {
            if (field.name().equals(name)) {
                return field;
            }
        }
        return null;
    }

    public String get(String name) {
        for (IndexableField f : fields) {
            if (f.name().equals(name) && f.stringValue() != null) {
                return f.stringValue();
            }
        }
        return null;
    }

    public BytesRef getBinaryValue(String name) {
        for (IndexableField f : fields) {
            if (f.name().equals(name) && f.binaryValue() != null) {
                return f.binaryValue();
            }
        }
        return null;
    }

    public Number getNumericValue(String name) {
        for (IndexableField f : fields) {
            if (f.name().equals(name) && f.numericValue() != null) {
                return f.numericValue();
            }
        }
        return null;
    }

}
