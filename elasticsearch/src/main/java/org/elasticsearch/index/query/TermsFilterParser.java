/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.query;

import com.google.common.collect.Lists;

import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilterCachingPolicy;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.lucene.BytesRefs;
import org.elasticsearch.common.lucene.HashedBytesRef;
import org.elasticsearch.common.lucene.search.AndFilter;
import org.elasticsearch.common.lucene.search.OrFilter;
import org.elasticsearch.common.lucene.search.Queries;
import org.elasticsearch.common.lucene.search.XBooleanFilter;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.indices.cache.filter.terms.TermsLookup;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class TermsFilterParser implements FilterParser {

    public static final String NAME = "terms";
    private Client client;

    public static final String EXECUTION_KEY = "execution";
    public static final String EXECUTION_VALUE_PLAIN = "plain";
    public static final String EXECUTION_VALUE_FIELDDATA = "fielddata";
    public static final String EXECUTION_VALUE_BOOL = "bool";
    public static final String EXECUTION_VALUE_BOOL_NOCACHE = "bool_nocache";
    public static final String EXECUTION_VALUE_AND = "and";
    public static final String EXECUTION_VALUE_AND_NOCACHE = "and_nocache";
    public static final String EXECUTION_VALUE_OR = "or";
    public static final String EXECUTION_VALUE_OR_NOCACHE = "or_nocache";

    @Inject
    public TermsFilterParser() {
    }

    @Override
    public String[] names() {
        return new String[]{NAME, "in"};
    }

    @Inject(optional = true)
    public void setClient(Client client) {
        this.client = client;
    }

    @Override
    public Filter parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        MapperService.SmartNameFieldMappers smartNameFieldMappers;
        FilterCachingPolicy cache = parseContext.autoFilterCachePolicy();
        String filterName = null;
        String currentFieldName = null;

        String lookupIndex = parseContext.index().name();
        String lookupType = null;
        String lookupId = null;
        String lookupPath = null;
        String lookupRouting = null;

        HashedBytesRef cacheKey = null;
        XContentParser.Token token;
        String execution = EXECUTION_VALUE_PLAIN;
        List<Object> terms = Lists.newArrayList();
        String fieldName = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_ARRAY) {
                if  (fieldName != null) {
                    throw new QueryParsingException(parseContext.index(), "[terms] filter does not support multiple fields");
                }
                fieldName = currentFieldName;

                while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                    Object value = parser.objectBytes();
                    if (value == null) {
                        throw new QueryParsingException(parseContext.index(), "No value specified for terms filter");
                    }
                    terms.add(value);
                }
            } else if (token == XContentParser.Token.START_OBJECT) {
                fieldName = currentFieldName;
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if (token.isValue()) {
                        if ("index".equals(currentFieldName)) {
                            lookupIndex = parser.text();
                        } else if ("type".equals(currentFieldName)) {
                            lookupType = parser.text();
                        } else if ("id".equals(currentFieldName)) {
                            lookupId = parser.text();
                        } else if ("path".equals(currentFieldName)) {
                            lookupPath = parser.text();
                        } else if ("routing".equals(currentFieldName)) {
                            lookupRouting = parser.textOrNull();
                        } else {
                            throw new QueryParsingException(parseContext.index(), "[terms] filter does not support [" + currentFieldName + "] within lookup element");
                        }
                    }
                }
                if (lookupType == null) {
                    throw new QueryParsingException(parseContext.index(), "[terms] filter lookup element requires specifying the type");
                }
                if (lookupId == null) {
                    throw new QueryParsingException(parseContext.index(), "[terms] filter lookup element requires specifying the id");
                }
                if (lookupPath == null) {
                    throw new QueryParsingException(parseContext.index(), "[terms] filter lookup element requires specifying the path");
                }
            } else if (token.isValue()) {
                if (EXECUTION_KEY.equals(currentFieldName)) {
                    execution = parser.text();
                } else if ("_name".equals(currentFieldName)) {
                    filterName = parser.text();
                } else if ("_cache".equals(currentFieldName)) {
                    cache = parseContext.parseFilterCachePolicy();
                } else if ("_cache_key".equals(currentFieldName) || "_cacheKey".equals(currentFieldName)) {
                    cacheKey = new HashedBytesRef(parser.text());
                } else {
                    throw new QueryParsingException(parseContext.index(), "[terms] filter does not support [" + currentFieldName + "]");
                }
            }
        }

        if (fieldName == null) {
            throw new QueryParsingException(parseContext.index(), "terms filter requires a field name, followed by array of terms");
        }

        FieldMapper<?> fieldMapper = null;
        smartNameFieldMappers = parseContext.smartFieldMappers(fieldName);
        if (smartNameFieldMappers != null) {
            if (smartNameFieldMappers.hasMapper()) {
                fieldMapper = smartNameFieldMappers.mapper();
                fieldName = fieldMapper.names().indexName();
            }
        }

        if (lookupId != null) {
            final TermsLookup lookup = new TermsLookup(lookupIndex, lookupType, lookupId, lookupRouting, lookupPath, parseContext);
            final GetResponse getResponse = client.get(new GetRequest(lookup.getIndex(), lookup.getType(), lookup.getId()).preference("_local").routing(lookup.getRouting())).actionGet();
            if (getResponse.isExists()) {
                List<Object> values = XContentMapValues.extractRawValues(lookup.getPath(), getResponse.getSourceAsMap());
                terms.addAll(values);
            }
        }

        if (terms.isEmpty()) {
            return Queries.MATCH_NO_FILTER;
        }
        
            Filter filter;
            if (EXECUTION_VALUE_PLAIN.equals(execution)) {
                if (fieldMapper != null) {
                    filter = fieldMapper.termsFilter(terms, parseContext);
                } else {
                    BytesRef[] filterValues = new BytesRef[terms.size()];
                    for (int i = 0; i < filterValues.length; i++) {
                        filterValues[i] = BytesRefs.toBytesRef(terms.get(i));
                    }
                    filter = new TermsFilter(fieldName, filterValues);
                }
            } else if (EXECUTION_VALUE_FIELDDATA.equals(execution)) {
                // if there are no mappings, then nothing has been indexing yet against this shard, so we can return
                // no match (but not cached!), since the FieldDataTermsFilter relies on a mapping...
                if (fieldMapper == null) {
                    return Queries.MATCH_NO_FILTER;
                }
    
                filter = fieldMapper.fieldDataTermsFilter(terms, parseContext);
            } else if (EXECUTION_VALUE_BOOL.equals(execution)) {
                XBooleanFilter boolFiler = new XBooleanFilter();
                if (fieldMapper != null) {
                    for (Object term : terms) {
                        boolFiler.add(parseContext.cacheFilter(fieldMapper.termFilter(term, parseContext), null, parseContext.autoFilterCachePolicy()), BooleanClause.Occur.SHOULD);
                    }
                } else {
                    for (Object term : terms) {
                        boolFiler.add(parseContext.cacheFilter(new TermFilter(new Term(fieldName, BytesRefs.toBytesRef(term))), null, parseContext.autoFilterCachePolicy()), BooleanClause.Occur.SHOULD);
                    }
                }
                filter = boolFiler;
            } else if (EXECUTION_VALUE_BOOL_NOCACHE.equals(execution)) {
                XBooleanFilter boolFiler = new XBooleanFilter();
                if (fieldMapper != null) {
                    for (Object term : terms) {
                        boolFiler.add(fieldMapper.termFilter(term, parseContext), BooleanClause.Occur.SHOULD);
                    }
                } else {
                    for (Object term : terms) {
                        boolFiler.add(new TermFilter(new Term(fieldName, BytesRefs.toBytesRef(term))), BooleanClause.Occur.SHOULD);
                    }
                }
                filter = boolFiler;
            } else if (EXECUTION_VALUE_AND.equals(execution)) {
                List<Filter> filters = Lists.newArrayList();
                if (fieldMapper != null) {
                    for (Object term : terms) {
                        filters.add(parseContext.cacheFilter(fieldMapper.termFilter(term, parseContext), null, parseContext.autoFilterCachePolicy()));
                    }
                } else {
                    for (Object term : terms) {
                        filters.add(parseContext.cacheFilter(new TermFilter(new Term(fieldName, BytesRefs.toBytesRef(term))), null, parseContext.autoFilterCachePolicy()));
                    }
                }
                filter = new AndFilter(filters);
            } else if (EXECUTION_VALUE_AND_NOCACHE.equals(execution)) {
                List<Filter> filters = Lists.newArrayList();
                if (fieldMapper != null) {
                    for (Object term : terms) {
                        filters.add(fieldMapper.termFilter(term, parseContext));
                    }
                } else {
                    for (Object term : terms) {
                        filters.add(new TermFilter(new Term(fieldName, BytesRefs.toBytesRef(term))));
                    }
                }
                filter = new AndFilter(filters);
            } else if (EXECUTION_VALUE_OR.equals(execution)) {
                List<Filter> filters = Lists.newArrayList();
                if (fieldMapper != null) {
                    for (Object term : terms) {
                        filters.add(parseContext.cacheFilter(fieldMapper.termFilter(term, parseContext), null, parseContext.autoFilterCachePolicy()));
                    }
                } else {
                    for (Object term : terms) {
                        filters.add(parseContext.cacheFilter(new TermFilter(new Term(fieldName, BytesRefs.toBytesRef(term))), null, parseContext.autoFilterCachePolicy()));
                    }
                }
                filter = new OrFilter(filters);
            } else if (EXECUTION_VALUE_OR_NOCACHE.equals(execution)) {
                List<Filter> filters = Lists.newArrayList();
                if (fieldMapper != null) {
                    for (Object term : terms) {
                        filters.add(fieldMapper.termFilter(term, parseContext));
                    }
                } else {
                    for (Object term : terms) {
                        filters.add(new TermFilter(new Term(fieldName, BytesRefs.toBytesRef(term))));
                    }
                }
                filter = new OrFilter(filters);
            } else {
                throw new QueryParsingException(parseContext.index(), "terms filter execution value [" + execution + "] not supported");
            }
    
            if (cache != null) {
                filter = parseContext.cacheFilter(filter, cacheKey, cache);
            }
    
            if (filterName != null) {
                parseContext.addNamedFilter(filterName, filter);
            }
            return filter;
    }
}
