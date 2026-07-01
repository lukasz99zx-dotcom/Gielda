package pl.cgielda.app;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PnlCalculator {

    public static class Result {
        /** Realized profit/loss (FIFO cost basis per paper), ignoring unsold holdings. */
        public final double realizedPnl;
        /** Sum of sell amounts minus sum of buy amounts across all transactions. */
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

    /**
     * Order of the input list does not matter (the user can freely reorder
     * transactions for display purposes) - transactions are always processed
     * in chronological (timestamp) order for FIFO matching to be meaningful.
     */
    public static Result compute(List<Transaction> transactions) {
        List<Transaction> chronological = new ArrayList<Transaction>(transactions);
        Collections.sort(chronological, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction a, Transaction b) {
                return Long.valueOf(a.timestamp).compareTo(b.timestamp);
            }
        });

        Map<String, Deque<Lot>> lotsByName = new HashMap<String, Deque<Lot>>();
        double realized = 0;
        double netCashFlow = 0;

        for (Transaction t : chronological) {
            if (t.quantity <= 0) {
                continue;
            }

            netCashFlow += (t.type == Transaction.TYPE_SELL) ? t.amount : -t.amount;

            Deque<Lot> lots = lotsByName.get(t.name);
            if (lots == null) {
                lots = new ArrayDeque<Lot>();
                lotsByName.put(t.name, lots);
            }

            if (t.type == Transaction.TYPE_BUY) {
                lots.addLast(new Lot(t.quantity, t.amount / t.quantity));
            } else {
                double sellQty = t.quantity;
                double unitProceeds = t.amount / t.quantity;
                while (sellQty > 0.0000001 && !lots.isEmpty()) {
                    Lot lot = lots.peekFirst();
                    double matched = Math.min(sellQty, lot.quantity);
                    realized += matched * (unitProceeds - lot.unitCost);
                    lot.quantity -= matched;
                    sellQty -= matched;
                    if (lot.quantity <= 0.0000001) {
                        lots.pollFirst();
                    }
                }
                if (sellQty > 0.0000001) {
                    // Sold more than was ever recorded as bought (e.g. missing
                    // history) - count the unmatched part at full proceeds.
                    realized += sellQty * unitProceeds;
                }
            }
        }

        double openCostBasis = 0;
        for (Deque<Lot> lots : lotsByName.values()) {
            for (Lot lot : lots) {
                openCostBasis += lot.quantity * lot.unitCost;
            }
        }

        return new Result(realized, netCashFlow, openCostBasis);
    }
}
