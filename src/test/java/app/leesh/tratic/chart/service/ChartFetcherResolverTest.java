package app.leesh.tratic.chart.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import app.leesh.tratic.chart.domain.Chart;
import app.leesh.tratic.chart.domain.Market;

class ChartFetcherResolverTest {

    @Test
    void resolve_returnsFetcherForMarket() {
        ChartFetcher upbit = new StubFetcher(Market.UPBIT);
        ChartFetcher binance = new StubFetcher(Market.BINANCE);
        ChartFetcherResolver resolver = new ChartFetcherResolver(List.of(upbit, binance));

        ChartFetcher resolved = resolver.resolve(Market.BINANCE);

        assertSame(binance, resolved);
    }

    @Test
    void constructor_throwsWhenDuplicateMarketFetcherExists() {
        ChartFetcher first = new StubFetcher(Market.UPBIT);
        ChartFetcher second = new StubFetcher(Market.UPBIT);

        assertThrows(IllegalArgumentException.class, () -> new ChartFetcherResolver(List.of(first, second)));
    }

    @Test
    void resolve_throwsWhenMarketNotSupported() {
        ChartFetcherResolver resolver = new ChartFetcherResolver(List.of(new StubFetcher(Market.UPBIT)));

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(Market.BINANCE));
    }

    private static final class StubFetcher implements ChartFetcher {
        private final Market market;

        private StubFetcher(Market market) {
            this.market = market;
        }

        @Override
        public Chart fetch(ChartFetchRequest req) {
            throw new UnsupportedOperationException("not used in resolver test");
        }

        @Override
        public Market market() {
            return market;
        }
    }
}
