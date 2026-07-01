package pl.cgielda.app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Human-editable semicolon-separated export/import format for backup/transfer. */
public class TransactionCsv {

    private static final String HEADER = "TYP;NAZWA;ILOSC;CENA;KURS;PROWIZJA%;DATA";
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static SimpleDateFormat dateFormat() {
        return new SimpleDateFormat(DATE_PATTERN, Locale.US);
    }

    public static String export(List<Transaction> transactions) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\n');
        SimpleDateFormat df = dateFormat();
        for (Transaction t : transactions) {
            sb.append(t.type).append(';')
                    .append(t.name).append(';')
                    .append(num(t.quantity)).append(';')
                    .append(num(t.price)).append(';')
                    .append(num(t.rate)).append(';')
                    .append(num(t.commissionPercent)).append(';')
                    .append(df.format(new java.util.Date(t.timestamp)))
                    .append('\n');
        }
        return sb.toString();
    }

    private static String num(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    public static class ParseResult {
        public final List<Transaction> transactions;
        public final int skippedLines;

        ParseResult(List<Transaction> transactions, int skippedLines) {
            this.transactions = transactions;
            this.skippedLines = skippedLines;
        }
    }

    public static ParseResult parse(String text) {
        List<Transaction> result = new ArrayList<Transaction>();
        int skipped = 0;
        if (text == null) {
            return new ParseResult(result, 0);
        }
        SimpleDateFormat df = dateFormat();
        String[] lines = text.split("\r?\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.length() == 0) {
                continue;
            }
            String[] parts = line.split(";");
            if (parts.length < 6) {
                skipped++;
                continue;
            }
            String typeToken = parts[0].trim().toUpperCase(Locale.US);
            if (!typeToken.equals(String.valueOf(Transaction.TYPE_BUY))
                    && !typeToken.equals(String.valueOf(Transaction.TYPE_SELL))) {
                // Likely the header row or a malformed line - skip silently.
                continue;
            }
            try {
                char type = typeToken.charAt(0);
                String name = parts[1].trim().toUpperCase(Locale.US);
                double quantity = Double.parseDouble(parts[2].trim().replace(',', '.'));
                double price = Double.parseDouble(parts[3].trim().replace(',', '.'));
                double rate = Double.parseDouble(parts[4].trim().replace(',', '.'));
                double commission = Double.parseDouble(parts[5].trim().replace(',', '.'));
                long timestamp;
                if (parts.length >= 7 && parts[6].trim().length() > 0) {
                    try {
                        timestamp = df.parse(parts[6].trim()).getTime();
                    } catch (ParseException pe) {
                        timestamp = System.currentTimeMillis();
                    }
                } else {
                    timestamp = System.currentTimeMillis();
                }
                if (name.length() == 0 || quantity <= 0 || price <= 0) {
                    skipped++;
                    continue;
                }
                result.add(new Transaction(type, name, quantity, price, rate, commission, timestamp));
            } catch (Exception e) {
                skipped++;
            }
        }
        return new ParseResult(result, skipped);
    }
}
