package pl.cgielda.app;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PnlCalculator {

    private static class Lot {
        double quantity;
        final double unitCost;

        Lot(double quantity, double unitCost) {
            this.quantity = quantity;
            this.unitCost = unitCost;
        }
    }

    /**
     * Realized profit/loss (FIFO cost basis per paper), ignoring the cost of
     * currently held (unsold) shares. transactionsNewestFirst is expected to
     * be ordered newest-first, as stored by MainActivity.
     */
    public static double realizedProfitLoss(List<Transaction> transactionsNewestFirst) {
        Map<String, Deque<Lot>> lotsByName = new HashMap<String, Deque<Lot>>();
        double realized = 0;

        for (int i = transactionsNewestFirst.size() - 1; i >= 0; i--) {
            Transaction t = transactionsNewestFirst.get(i);
            if (t.quantity <= 0) {
                continue;
            }
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
        return realized;
    }
}
