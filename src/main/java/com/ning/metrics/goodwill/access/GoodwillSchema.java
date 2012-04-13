/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.metrics.goodwill.access;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Describe a Schema in Goodwill.
 * This is basically a union of a Schema and extra metadata for the Sink.
 *
 * @see com.ning.metrics.serialization.schema.Schema
 */
public class GoodwillSchema
{
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String name;
    private String sinkAddInfo;
    private final HashMap<Short, GoodwillSchemaField> thriftItems = new HashMap<Short, GoodwillSchemaField>();

    public static final String JSON_THRIFT_TYPE_NAME = "name";
    public static final String JSON_THRIFT_TYPE_SCHEMA = "schema";
    public static final String JSON_THRIFT_TYPE_SINK_ADD_INFO = "sinkAddInfo";

    /**
     * Jackson constructor
     * <p/>
     * {
     * "sinkAddInfo": null,
     * "name": "hello",
     * "schema": [
     * {
     * "name": "my hello attribute",
     * "type": "string",
     * "position": 1,
     * "description": "awesome attribute",
     * "sql": {
     * "type": "nvarchar",
     * "length": null,
     * "scale": null,
     * "precision": null
     * }
     * },
     * {
     * "name": "dsfdfsfds",
     * "type": "bool",
     * "position": 2,
     * "description": "dfsfdsfds",
     * "sql": {
     * "type": "boolean",
     * "length": null,
     * "scale": null,
     * "precision": null
     * }
     * },
     * {
     * "name": "wer",
     * "type": "double",
     * "position": 3,
     * "description": "wer",
     * "sql": {
     * "type": "numeric",
     * "length": null,
     * "scale": 12,
     * "precision": 42
     * }
     * }
     * ]
     * }
     *
     * @param name        Schema name
     * @param items       List of fields
     * @param sinkAddInfo extra information for the Sink
     */
    @JsonCreator
    public GoodwillSchema(
        @JsonProperty(JSON_THRIFT_TYPE_NAME) final String name,
        @JsonProperty(JSON_THRIFT_TYPE_SCHEMA) final List<GoodwillSchemaField> items,
        @JsonProperty(JSON_THRIFT_TYPE_SINK_ADD_INFO) final String sinkAddInfo
    )
    {
        this(name, items);
        setSinkAddInfo(sinkAddInfo);
    }

    /**
     * Manual constructor, typically used by Goodwill stores.
     *
     * @param name  Schema name
     * @param items List of fields
     */
    public GoodwillSchema(final String name, final List<GoodwillSchemaField> items)
    {
        this.name = name;
        for (final GoodwillSchemaField field : items) {
            addThriftField(field);
        }
    }

    public static GoodwillSchema decode(final String thriftJson) throws IOException
    {
        return mapper.readValue(thriftJson, GoodwillSchema.class);
    }

    @JsonValue
    @SuppressWarnings({"unchecked"})
    public ImmutableMap toMap()
    {
        return new ImmutableMap.Builder()
            .put(JSON_THRIFT_TYPE_NAME, getName())
            .put(JSON_THRIFT_TYPE_SCHEMA, getSchema())
            .put(JSON_THRIFT_TYPE_SINK_ADD_INFO, sinkAddInfo == null ? "" : sinkAddInfo)
            .build();
    }

    /**
     * Add a field in the Thrift. The code does not enforce sanity w.r.t. field positions.
     *
     * @param goodwillSchemaField field to add
     */
    public void addThriftField(final GoodwillSchemaField goodwillSchemaField)
    {
        thriftItems.put(goodwillSchemaField.getId(), goodwillSchemaField);
    }

    public String getName()
    {
        return name;
    }

    /**
     * Get the schema as a collection of fields.
     * We guarantee the ordering by field id.
     *
     * @return the sorted collection of fields
     */
    public ArrayList<GoodwillSchemaField> getSchema()
    {
        final ArrayList<GoodwillSchemaField> items = new ArrayList<GoodwillSchemaField>(thriftItems.values());

        Collections.sort(items, new Comparator<GoodwillSchemaField>()
        {
            @Override
            public int compare(final GoodwillSchemaField left, final GoodwillSchemaField right)
            {
                return Short.valueOf(left.getId()).compareTo(right.getId());
            }
        });

        return items;
    }

    public void setSinkAddInfo(final String sinkAddInfo)
    {
        this.sinkAddInfo = sinkAddInfo;
    }

    /**
     * Given a position, return the field at that position.
     *
     * @param i position in the Thrift (start with 1)
     * @return the GoodwillSchemaField object
     */
    public GoodwillSchemaField getFieldByPosition(final short i)
    {
        return thriftItems.get(i);
    }

    /**
     * Given a name, return the field matching the name.
     *
     * @param name GoodwillSchemaField name
     * @return the GoodwillSchemaField object
     */
    public GoodwillSchemaField getFieldByName(final String name)
    {
        for (final GoodwillSchemaField field : thriftItems.values()) {
            if (field.getName().equals(name)) {
                return field;
            }
        }

        return null;
    }

    @Override
    public String toString()
    {
        try {
            return mapper.writeValueAsString(this);
        }
        catch (JsonGenerationException e) {
            return "GoodwillSchema{" +
                JSON_THRIFT_TYPE_NAME + "='" + getName() + '\'' +
                ", thriftItems=" + getSchema() +
                '}';
        }
        catch (IOException e) {
            return "GoodwillSchema{" +
                JSON_THRIFT_TYPE_NAME + "='" + getName() + '\'' +
                ", thriftItems=" + getSchema() +
                '}';
        }
    }

    public byte[] toJSONBytes() throws IOException
    {
        return mapper.writeValueAsBytes(this);
    }

    /**
     * @deprecated Use 'toJSONBytes()' instead
     */
    @Deprecated
    public ByteArrayOutputStream toJSON() throws IOException
    {
        // silly, but 
        final byte[] bytes = toJSONBytes();
        final ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);
        out.write(bytes);
        return out;
    }
}