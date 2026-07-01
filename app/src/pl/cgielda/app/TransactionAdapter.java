package pl.cgielda.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends BaseAdapter {

    public interface RowActionListener {
        void onRowTapped(int position);
        void onRowSwipedForDelete(int position);
    }

    private final Context context;
    private final List<Transaction> items;
    private final RowActionListener listener;

    public TransactionAdapter(Context context, List<Transaction> items, RowActionListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
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

    private TextView cell(String text, float weight, int color, boolean bold, int align) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(14);
        tv.setPadding(dp(4), 0, dp(4), 0);
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Transaction t = items.get(position);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(dp(10), dp(4), dp(10), dp(4));
        card.setLayoutParams(cardLp);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Colors.SURFACE);
        bg.setCornerRadius(dp(14));
        card.setBackground(bg);
        card.setTranslationX(0f);

        boolean buy = t.type == Transaction.TYPE_BUY;
        int typeColor = buy ? Colors.GREEN : Colors.RED;
        String typeLabel = buy ? "K" : "S";

        LinearLayout badge = new LinearLayout(context);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        badgeBg.setColor(colorWithAlpha(typeColor, 0x22));
        badge.setBackground(badgeBg);
        badge.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(dp(30), dp(30));
        badgeLp.setMargins(0, 0, dp(10), 0);
        badge.setLayoutParams(badgeLp);
        TextView typeView = new TextView(context);
        typeView.setText(typeLabel);
        typeView.setTextColor(typeColor);
        typeView.setTypeface(typeView.getTypeface(), Typeface.BOLD);
        typeView.setTextSize(14);
        badge.addView(typeView);
        card.addView(badge);

        String displayName = t.name == null ? "" : t.name.toUpperCase(Locale.getDefault());
        card.addView(cell(displayName, 3f, Colors.TEXT_PRIMARY, true, Gravity.LEFT | Gravity.CENTER_VERTICAL));
        card.addView(cell(formatNumber(t.quantity), 2f, Colors.TEXT_SECONDARY, false, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        card.addView(cell(formatMoney(t.price), 2f, Colors.TEXT_SECONDARY, false, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        card.addView(cell(formatMoney(t.amount), 3f, typeColor, true, Gravity.RIGHT | Gravity.CENTER_VERTICAL));

        card.setOnTouchListener(new SwipeToDeleteListener(position));
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRowTapped(position);
                }
            }
        });

        return card;
    }

    private int colorWithAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private final class SwipeToDeleteListener implements View.OnTouchListener {
        private final int position;
        private float startX;
        private float startY;
        private boolean swiping;
        private final int touchSlop;

        SwipeToDeleteListener(int position) {
            this.position = position;
            this.touchSlop = dp(8);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getRawX();
                    startY = event.getRawY();
                    swiping = false;
                    return false;
                case MotionEvent.ACTION_MOVE: {
                    float dx = event.getRawX() - startX;
                    float dy = event.getRawY() - startY;
                    if (!swiping) {
                        if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 2 && dx > 0) {
                            swiping = true;
                        } else {
                            return false;
                        }
                    }
                    v.setTranslationX(Math.max(0, dx));
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (swiping) {
                        float dx = event.getRawX() - startX;
                        swiping = false;
                        if (dx > v.getWidth() * 0.35f) {
                            v.setTranslationX(0f);
                            if (listener != null) {
                                listener.onRowSwipedForDelete(position);
                            }
                        } else {
                            v.animate().translationX(0f).setDuration(150).start();
                        }
                        return true;
                    }
                    return false;
                }
                default:
                    return false;
            }
        }
    }

    private static final DecimalFormatSymbols DISPLAY_SYMBOLS = new DecimalFormatSymbols(Locale.US);
    static {
        DISPLAY_SYMBOLS.setGroupingSeparator(' ');
        DISPLAY_SYMBOLS.setDecimalSeparator('.');
    }
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00", DISPLAY_SYMBOLS);
    private static final DecimalFormat WHOLE_NUMBER_FORMAT = new DecimalFormat("#,##0", DISPLAY_SYMBOLS);
    private static final DecimalFormat DECIMAL_NUMBER_FORMAT = new DecimalFormat("#,##0.####", DISPLAY_SYMBOLS);

    /** For display only (thousands-separated) - not meant to be parsed back. */
    static String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return WHOLE_NUMBER_FORMAT.format(value);
        }
        return DECIMAL_NUMBER_FORMAT.format(value);
    }

    /** For display only (thousands-separated) - not meant to be parsed back. */
    static String formatMoney(double value) {
        return MONEY_FORMAT.format(value);
    }

    /** Plain (no grouping), safe to pre-fill into an editable field and re-parse. */
    static String plainNumber(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.4f", value);
    }

    /** Plain (no grouping), safe to pre-fill into an editable field and re-parse. */
    static String plainMoney(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
