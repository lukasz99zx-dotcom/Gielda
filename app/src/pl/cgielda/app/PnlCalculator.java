package pl.cgielda.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class PnlCalculator {

    public static class Result {
        /** Realized profit/loss (cheapest-lot-first cost basis per paper), ignoring unsold holdings. */
        public final double realizedPnl;
        /**
         * Cash actually received minus cash actually spent, counting only the
         * matched (closed) portion of trades - shares still held are excluded,
         * so this tracks realizedPnl by construction.
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

        Lot(double quantity, double unitCost) {
            this.quantity = quantity;
            this.unitCost = unitCost;
        }
    }

    private static final Comparator<Lot> CHEAPEST_FIRST = new Comparator<Lot>() {
        @Override
        public int compare(Lot a, Lot b) {
            return Double.compare(a.unitCost, b.unitCost);
        }
    };

    /**
     * Order of the input list does not matter for display (the user can freely
     * reorder transactions) - transactions are always processed in chronological
     * (timestamp) order, since a sale can only be matched against shares bought
     * earlier. Within the shares available at that point, a sale is matched
     * against the CHEAPEST lot(s) first (regardless of purchase order), so the
     * realized result reflects the most favorable, price-based matching rather
     * than an arbitrary entry order.
     */
    public static Result compute(List<Transaction> transactions) {
        List<Transaction> chronological = new ArrayList<Transaction>(transactions);
        Collections.sort(chronological, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction a, Transaction b) {
                return Long.valueOf(a.timestamp).compareTo(b.timestamp);
            }
        });

        Map<String, PriorityQueue<Lot>> lotsByName = new HashMap<String, PriorityQueue<Lot>>();
        double realized = 0;
        double netCashFlow = 0;

        for (Transaction t : chronological) {
            if (t.quantity <= 0) {
                continue;
            }

            PriorityQueue<Lot> lots = lotsByName.get(t.name);
            if (lots == null) {
                lots = new PriorityQueue<Lot>(11, CHEAPEST_FIRST);
                lotsByName.put(t.name, lots);
            }

            if (t.type == Transaction.TYPE_BUY) {
                lots.add(new Lot(t.quantity, t.amount / t.quantity));
            } else {
                double sellQty = t.quantity;
                double unitProceeds = t.amount / t.quantity;
                while (sellQty > 0.0000001 && !lots.isEmpty()) {
                    Lot lot = lots.peek();
                    double matched = Math.min(sellQty, lot.quantity);
                    double matchedReceived = matched * unitProceeds;
                    double matchedSpent = matched * lot.unitCost;
                    realized += matchedReceived - matchedSpent;
                    netCashFlow += matchedReceived - matchedSpent;
                    lot.quantity -= matched;
                    sellQty -= matched;
                    if (lot.quantity <= 0.0000001) {
                        lots.poll();
                    }
                }
                if (sellQty > 0.0000001) {
                    // Sold more than was ever recorded as bought (e.g. missing
                    // history) - count the unmatched part at full proceeds.
                    realized += sellQty * unitProceeds;
                    netCashFlow += sellQty * unitProceeds;
                }
            }
        }

        double openCostBasis = 0;
        for (PriorityQueue<Lot> lots : lotsByName.values()) {
            for (Lot lot : lots) {
                openCostBasis += lot.quantity * lot.unitCost;
            }
        }

        return new Result(realized, netCashFlow, openCostBasis);
    }
}
