package pl.cgielda.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

public class PnlCalculator {

    public static class Result {
        /** Realized profit/loss (FIFO cost basis per paper), ignoring unsold holdings. */
        public final double realizedPnl;
        /**
         * Cash actually received minus cash actually spent, counting only the
         * matched (closed) portion of trades - shares still held are excluded.
         * Each sale is matched against the cheapest available lot(s) of the
         * same paper bought up to that point, regardless of purchase order.
         */
        public final double netCashFlow;
        /** Cost basis (at purchase) of shares still held, i.e. money currently tied up. */
        public final double openCostBasis;

        Result(double realizedPnl, double netCashFlow, double openCostBasis) {
            this.realizedPnl = realizedPnl;
            this.netCashFlow = netCashFlow;
            this.openCostBasis = openCostBasis;
        }
    }

    private static class Lot {
        double quantity;
        final double unitCost;
        final long seq;

        Lot(double quantity, double unitCost, long seq) {
            this.quantity = quantity;
            this.unitCost = unitCost;
            this.seq = seq;
        }
    }

    private static final Comparator<Lot> OLDEST_FIRST = new Comparator<Lot>() {
        @Override
        public int compare(Lot a, Lot b) {
            return Long.valueOf(a.seq).compareTo(b.seq);
        }
    };

    private static final Comparator<Lot> CHEAPEST_FIRST = new Comparator<Lot>() {
        @Override
        public int compare(Lot a, Lot b) {
            return Double.compare(a.unitCost, b.unitCost);
        }
    };

    private static class MatchResult {
        double matchedDelta;
        double openCostBasis;
    }

    /**
     * Buys and sells of the "same" paper must match even if the name was
     * typed/stored with different casing or surrounding whitespace at
     * different times (e.g. data saved before uppercase input was enforced).
     */
    private static String normalizeName(String name) {
        return name == null ? "" : name.trim().toUpperCase(Locale.US);
    }

    /**
     * Runs FIFO-style lot matching over chronologically-ordered transactions,
     * where lotOrder decides which available lot of the same paper a sale
     * picks first (oldest, cheapest, ...). A sale can only ever match lots
     * bought earlier in time, regardless of lotOrder.
     */
    private static MatchResult runMatching(List<Transaction> chronological, Comparator<Lot> lotOrder) {
        Map<String, PriorityQueue<Lot>> lotsByName = new HashMap<String, PriorityQueue<Lot>>();
        double delta = 0;
        long seqCounter = 0;

        for (Transaction t : chronological) {
            if (t.quantity <= 0) {
                continue;
            }

            String key = normalizeName(t.name);
            PriorityQueue<Lot> lots = lotsByName.get(key);
            if (lots == null) {
                lots = new PriorityQueue<Lot>(11, lotOrder);
                lotsByName.put(key, lots);
            }

            if (t.type == Transaction.TYPE_BUY) {
                lots.add(new Lot(t.quantity, t.amount / t.quantity, seqCounter++));
            } else {
                double sellQty = t.quantity;
                double unitProceeds = t.amount / t.quantity;
                while (sellQty > 0.0000001 && !lots.isEmpty()) {
                    Lot lot = lots.peek();
                    double matched = Math.min(sellQty, lot.quantity);
                    delta += matched * (unitProceeds - lot.unitCost);
                    lot.quantity -= matched;
                    sellQty -= matched;
                    if (lot.quantity <= 0.0000001) {
                        lots.poll();
                    }
                }
                if (sellQty > 0.0000001) {
                    // Sold more than was ever recorded as bought (e.g. missing
                    // history) - count the unmatched part at full proceeds.
                    delta += sellQty * unitProceeds;
                }
            }
        }

        double openCostBasis = 0;
        for (PriorityQueue<Lot> lots : lotsByName.values()) {
            for (Lot lot : lots) {
                openCostBasis += lot.quantity * lot.unitCost;
            }
        }

        MatchResult result = new MatchResult();
        result.matchedDelta = delta;
        result.openCostBasis = openCostBasis;
        return result;
    }

    /**
     * Order of the input list does not matter for display (the user can freely
     * reorder transactions) - transactions are always processed in chronological
     * (timestamp) order for matching, since a sale can only be matched against
     * shares bought earlier.
     */
    public static Result compute(List<Transaction> transactions) {
        List<Transaction> chronological = new ArrayList<Transaction>(transactions);
        Collections.sort(chronological, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction a, Transaction b) {
                return Long.valueOf(a.timestamp).compareTo(b.timestamp);
            }
        });

        MatchResult fifo = runMatching(chronological, OLDEST_FIRST);
        MatchResult cheapest = runMatching(chronological, CHEAPEST_FIRST);

        return new Result(fifo.matchedDelta, cheapest.matchedDelta, fifo.openCostBasis);
    }
}
