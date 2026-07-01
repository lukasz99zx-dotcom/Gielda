package pl.cgielda.app;

public class Transaction {
    public static final char TYPE_BUY = 'K';
    public static final char TYPE_SELL = 'S';
    private static final char SEP = (char) 30;

    public char type;
    public String name;
    public double quantity;
    public double price;
    public double rate;
    public double commissionPercent;
    public double amount;
    public long timestamp;

    public Transaction(char type, String name, double quantity, double price,
                        double rate, double commissionPercent, long timestamp) {
        this.type = type;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.rate = rate;
        this.commissionPercent = commissionPercent;
        this.timestamp = timestamp;
        this.amount = computeAmount();
    }

    private double computeAmount() {
        double base = quantity * price * rate;
        double commission = base * (commissionPercent / 100.0);
        if (type == TYPE_BUY) {
            return base + commission;
        } else {
            return base - commission;
        }
    }

    public String toLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(SEP);
        sb.append(escape(name)).append(SEP);
        sb.append(quantity).append(SEP);
        sb.append(price).append(SEP);
        sb.append(rate).append(SEP);
        sb.append(commissionPercent).append(SEP);
        sb.append(timestamp);
        return sb.toString();
    }

    public static Transaction fromLine(String line) {
        String[] parts = line.split(String.valueOf(SEP), -1);
        if (parts.length < 7) {
            return null;
        }
        try {
            char type = parts[0].charAt(0);
            String name = unescape(parts[1]);
            double quantity = Double.parseDouble(parts[2]);
            double price = Double.parseDouble(parts[3]);
            double rate = Double.parseDouble(parts[4]);
            double commissionPercent = Double.parseDouble(parts[5]);
            long timestamp = Long.parseLong(parts[6]);
            return new Transaction(type, name, quantity, price, rate, commissionPercent, timestamp);
        } catch (Exception e) {
            return null;
        }
    }

    private static String escape(String s) {
        return s.replace(SEP, ' ').replace('\n', ' ');
    }

    private static String unescape(String s) {
        return s;
    }
}
