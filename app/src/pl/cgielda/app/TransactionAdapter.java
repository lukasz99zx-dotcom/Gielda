package pl.cgielda.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends BaseAdapter {

    private final Context context;
    private final List<Transaction> items;

    public TransactionAdapter(Context context, List<Transaction> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private int dp(int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private TextView cell(String text, int weight, int color, boolean bold, int align) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setPadding(dp(4), dp(8), dp(4), dp(8));
        tv.setGravity(align);
        if (bold) {
            tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        tv.setLayoutParams(lp);
        return tv;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Transaction t = items.get(position);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (position % 2 == 0) {
            row.setBackgroundColor(Color.parseColor("#F5F5F5"));
        } else {
            row.setBackgroundColor(Color.WHITE);
        }

        boolean buy = t.type == Transaction.TYPE_BUY;
        int typeColor = buy ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828");
        String typeLabel = buy ? "K" : "S";

        row.addView(cell(typeLabel, 1, typeColor, true, Gravity.CENTER));
        row.addView(cell(t.name, 3, Color.parseColor("#212121"), false, Gravity.LEFT | Gravity.CENTER_VERTICAL));
        row.addView(cell(formatNumber(t.quantity), 2, Color.parseColor("#424242"), false, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        row.addView(cell(formatMoney(t.price), 2, Color.parseColor("#424242"), false, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        row.addView(cell(formatMoney(t.amount), 3, typeColor, true, Gravity.RIGHT | Gravity.CENTER_VERTICAL));

        return row;
    }

    static String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.getDefault(), "%.0f", value);
        }
        return String.format(Locale.getDefault(), "%.4f", value);
    }

    static String formatMoney(double value) {
        return String.format(Locale.getDefault(), "%.2f", value);
    }
}
