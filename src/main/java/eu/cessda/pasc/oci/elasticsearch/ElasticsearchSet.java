/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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
import lombok.EqualsAndHashCode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An implementation of the set interface that supports iterating over an Elasticsearch scroll.
 *
 * @param <T> the type to decode the JSON into
 */
@EqualsAndHashCode(callSuper = true)
public class ElasticsearchSet<T> extends AbstractSet<T> {

    private final SearchRequestBuilder searchRequestBuilder;
    private final Client client;
    private final ObjectReader objectReader;

    ElasticsearchSet(SearchRequestBuilder searchRequestBuilder, Client client, ObjectReader objectReader) {
        this.searchRequestBuilder = searchRequestBuilder;
        this.client = client;
        this.objectReader = objectReader;
    }

    @Override
    public int size() {
        return (int) searchRequestBuilder.get().getHits().getTotalHits();
    }

    /**
     * {@inheritDoc}
     *
     * @throws java.io.UncheckedIOException if an IO error occurs when decoding the JSON
     */
    @Override
    public Iterator<T> iterator() {
        return new ElasticsearchIterator();
    }

    /**
     * An iterator that iterates over an Elasticsearch scroll and decodes the resulting JSON
     */
    private class ElasticsearchIterator implements Iterator<T> {

        private final TimeValue timeout = new TimeValue(Duration.ofSeconds(60).toMillis());
        private SearchResponse response = searchRequestBuilder.setSize(1000).setScroll(timeout).get();
        private int currentIndex = 0;

        @Override
        public boolean hasNext() {
            if (currentIndex >= response.getHits().getHits().length) {
                // Reached the end of the current scroll, collect the next scroll
                response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeout).get();
                currentIndex = 0;
            }
            return response.getHits().getHits().length > 0;
        }

        /**
         * {@inheritDoc}
         *
         * @throws UncheckedIOException if an IO error occurs when decoding the JSON
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
