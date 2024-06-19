/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cessda.pasc.oci.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.ClearScrollRequest;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import lombok.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An implementation of the set interface that supports iterating over an Elasticsearch scroll.
 * This is an unmodifiable collection.
 *
 * @param <T> the type to decode the JSON into.
 */
@SuppressWarnings("java:S2160") // The super class will handle equality comparisons
public class ElasticsearchSet<T> extends AbstractSet<T> {

    /**
     * Scroll timeout
     */
    private static final Time SCROLL_TIMEOUT = new Time.Builder().time("1m").build();
    private final SearchRequest searchRequest;
    private final Class<T> clazz;
    private final ElasticsearchClient client;

    /**
     * Constructs a new Elasticsearch Set that will contain the results of the given search query.
     * @param searchRequestBuilder the search request to execute, the builder will become unusable afterwards.
     * @param client the Elasticsearch Client to use.
     * @param clazz the class to deserialize to.
     */
    ElasticsearchSet(SearchRequest.Builder searchRequestBuilder, ElasticsearchClient client, Class<T> clazz) {
        this.searchRequest = searchRequestBuilder.scroll(SCROLL_TIMEOUT).build();
        this.client = client;
        this.clazz = clazz;
    }

    /**
     * {@inheritDoc}
     *
     * @throws java.io.UncheckedIOException if an IO error occurs when decoding the JSON.
     */
    @Override
    public @NonNull Iterator<T> iterator() {
        try {
            return new ElasticsearchIterator();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws java.io.UncheckedIOException if an IO error occurs when accessing Elasticsearch.
     */
    @Override
    public int size() {
        try {
            var countRequest = new CountRequest.Builder().index(searchRequest.index()).query(searchRequest.query()).build();
            long totalHits = client.count(countRequest).count();
            return totalHits < Integer.MAX_VALUE ? (int) totalHits : Integer.MAX_VALUE;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * An iterator that iterates over an Elasticsearch scroll and decodes the resulting JSON.
     */
    private class ElasticsearchIterator implements Iterator<T> {

        private ResponseBody<T> response;
        private int currentIndex;

        private ElasticsearchIterator() throws IOException {
            response = client.search(searchRequest, clazz);
            currentIndex = 0;
        }

        /**
         * {@inheritDoc}
         *
         * @throws UncheckedIOException if an IO error occurs when accessing Elasticsearch.
         */
        @Override
        public boolean hasNext() {
            if (currentIndex >= response.hits().hits().size()) {
                // Reached the end of the current scroll, collect the next scroll if available.
                try {
                    var scrollRequest = new ScrollRequest.Builder().scrollId(response.scrollId()).scroll(SCROLL_TIMEOUT).build();
                    response = client.scroll(scrollRequest, clazz);
                    currentIndex = 0; // Only reset the index once an update has been retrieved.
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            var hasNext = !response.hits().hits().isEmpty();
            if (!hasNext) {
                // If no more results are available, clear the scroll context
                var clearScrollRequest = new ClearScrollRequest.Builder()
                    .scrollId(response.scrollId())
                    .build();

                try {
                    client.clearScroll(clearScrollRequest);
                } catch (IOException e) {
                    // ignored - clearing the scroll is for tidyness reasons
                }
            }
            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("End of scroll reached");
            }
            return response.hits().hits().get(currentIndex++).source();
        }
    }
}
