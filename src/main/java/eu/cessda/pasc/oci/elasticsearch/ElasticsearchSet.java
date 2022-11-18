/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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

import com.fasterxml.jackson.databind.ObjectReader;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.core.TimeValue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.client.RequestOptions.DEFAULT;

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
    private static final TimeValue timeout = new TimeValue(60, TimeUnit.SECONDS);
    private final SearchRequest searchRequest;
    private final ObjectReader objectReader;
    private final RestHighLevelClient client;

    /**
     * Constructs a new Elasticsearch Set that will contain the results of the given search query.
     * @param searchRequest the search request to execute.
     * @param client the Elasticsearch Client to use.
     * @param objectReader the object reader to use to deserialize the JSON.
     */
    ElasticsearchSet(SearchRequest searchRequest, RestHighLevelClient client, ObjectReader objectReader) {
        this.searchRequest = searchRequest;
        this.client = client;
        this.objectReader = objectReader;
    }

    /**
     * {@inheritDoc}
     *
     * @throws java.io.UncheckedIOException if an IO error occurs when decoding the JSON.
     */
    @Override
    public Iterator<T> iterator() {
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
            var countRequest = new CountRequest(searchRequest.indices(), searchRequest.source().query());
            long totalHits = client.count(countRequest, DEFAULT).getCount();
            return totalHits < Integer.MAX_VALUE ? (int) totalHits : Integer.MAX_VALUE;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * An iterator that iterates over an Elasticsearch scroll and decodes the resulting JSON.
     */
    private class ElasticsearchIterator implements Iterator<T> {

        private SearchResponse response;
        private int currentIndex;

        private ElasticsearchIterator() throws IOException {
            response = client.search(searchRequest.scroll(timeout), DEFAULT);
            currentIndex = 0;
        }

        /**
         * {@inheritDoc}
         *
         * @throws UncheckedIOException if an IO error occurs when accessing Elasticsearch.
         */
        @Override
        public boolean hasNext() {
            if (currentIndex >= response.getHits().getHits().length) {
                // Reached the end of the current scroll, collect the next scroll if available.
                try {
                    response = client.scroll(new SearchScrollRequest(response.getScrollId()).scroll(timeout), DEFAULT);
                    currentIndex = 0; // Only reset the index once an update has been retrieved.
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            var hasNext = response.getHits().getHits().length > 0;
            if (!hasNext) {
                // If no more results are available, clear the scroll context
                var clearScrollRequest = new ClearScrollRequest();
                clearScrollRequest.addScrollId(response.getScrollId());
                client.clearScrollAsync(clearScrollRequest, DEFAULT, new ActionListener<>() {
                    @Override
                    public void onResponse(ClearScrollResponse clearScrollResponse) {
                        // ignored
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // ignored
                    }
                });
            }
            return hasNext;
        }

        /**
         * {@inheritDoc}
         *
         * @throws UncheckedIOException if an IO error occurs when decoding the JSON.
         */
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("End of scroll reached");
            }
            try {
                return objectReader.readValue(response.getHits().getHits()[currentIndex++].getSourceRef().streamInput());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
