package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

public class CrawlTaskBuilder {
    private final Clock clock;
    private final PageParserFactory parserFactory;
    private String url;
    private Instant deadline;
    private int depth;
    private ConcurrentMap<String, Integer> counts;
    private Set<String> visitedUrls;
    private List<Pattern> ignoredUrls;

    public class CrawlTask extends RecursiveAction {
        private final String url;
        private final Instant deadline;
        private final int depth;
        private final ConcurrentMap<String, Integer> counts;
        private final Set<String> visitedUrls;
        private final List<Pattern> ignoredUrls;

        private CrawlTask(
                String url,
                Instant deadline,
                int depth,
                ConcurrentMap<String, Integer> counts,
                Set<String> visitedUrls,
                List<Pattern> ignoredUrls) {
            this.url = url;
            this.deadline = deadline;
            this.depth = depth;
            this.counts = counts;
            this.visitedUrls = visitedUrls;
            this.ignoredUrls = ignoredUrls;
        }

        public void compute() {
            if (depth == 0 || clock.instant().isAfter(deadline)) {
                return;
            }
            for (Pattern pattern : ignoredUrls) {
                if (pattern.matcher(url).matches()) {
                    return;
                }
            }
            if (!visitedUrls.add(url)) {
                return;
            }
            PageParser.Result result = parserFactory.get(url).parse();

            for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
                String word = e.getKey();
                Integer count = e.getValue();
                counts.compute(word, (k, v) -> v == null ? count : v + count);
            }

            List<CrawlTask> tasks = new ArrayList<>(result.getLinks().size());
            CrawlTaskBuilder builder = new CrawlTaskBuilder(clock, parserFactory)
                    .setDeadline(deadline)
                    .setDepth(depth - 1)
                    .setWordCounts(counts)
                    .setVisitedUrls(visitedUrls)
                    .setIgnoredUrls(ignoredUrls);

            result.getLinks().forEach(link ->
                    tasks.add(builder.setUrl(link).build())
            );

            invokeAll(tasks);
        }
    }

    public CrawlTaskBuilder (Clock clock, PageParserFactory parserFactory) {
        this.clock = clock;
        this.parserFactory = parserFactory;
        this.url = "";
        this.deadline = Instant.now();
        this.depth = 0;
        this.counts = new ConcurrentHashMap<>();
        this.visitedUrls = Collections.synchronizedSet(new HashSet<>());
        this.ignoredUrls = new ArrayList<>();
    }

    public CrawlTaskBuilder setUrl (String url) {
        this.url = url;
        return this;
    }

    public CrawlTaskBuilder setDeadline (Instant deadline) {
        this.deadline = deadline;
        return this;
    }

    public CrawlTaskBuilder setDepth(int depth) {
        this.depth = depth;
        return this;
    }

    public CrawlTaskBuilder setVisitedUrls (Set<String> visitedUrls) {
        this.visitedUrls = visitedUrls;
        return this;
    }

    public CrawlTaskBuilder setWordCounts (ConcurrentMap<String, Integer> counts) {
        this.counts = counts;
        return this;
    }

    public CrawlTaskBuilder setIgnoredUrls (List<Pattern> ignoredUrls) {
        this.ignoredUrls = ignoredUrls;
        return this;
    }

    public CrawlTask build() {
        return new CrawlTask(url, deadline, depth, counts, visitedUrls, ignoredUrls);
    }
}