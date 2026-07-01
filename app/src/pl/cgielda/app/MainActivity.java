package pl.cgielda.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "cgielda_prefs";
    private static final String KEY_TRANSACTIONS = "transactions";

    private final List<Transaction> transactions = new ArrayList<Transaction>();
    private TransactionAdapter adapter;
    private TextView summaryView;

    private EditText nameInput;
    private EditText qtyInput;
    private EditText priceInput;
    private EditText rateInput;
    private EditText commissionInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Color.WHITE);

        root.addView(buildHeader());
        root.addView(buildForm());
        root.addView(buildListHeader());
        root.addView(buildList());
        root.addView(buildSummaryBar());

        setContentView(root);

        loadTransactions();
        refreshList();
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private View buildHeader() {
        TextView title = new TextView(this);
        title.setText("cGiełda");
        title.setTextSize(22);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setBackgroundColor(Color.parseColor("#1565C0"));
        title.setPadding(dp(16), dp(16), dp(16), dp(16));
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return title;
    }

    private EditText makeInput(String hint, int inputType) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(12), dp(4), dp(12), dp(4));
        et.setLayoutParams(lp);
        return et;
    }

    private View buildForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(0, dp(8), 0, dp(8));
        form.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        nameInput = makeInput("Nazwa papieru", InputType.TYPE_CLASS_TEXT);
        qtyInput = makeInput("Ilość akcji", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        priceInput = makeInput("Cena akcji", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        rateInput = makeInput("Kurs (domyślnie 1)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        commissionInput = makeInput("Prowizja % (domyślnie 0.39)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        form.addView(nameInput);
        form.addView(qtyInput);
        form.addView(priceInput);
        form.addView(rateInput);
        form.addView(commissionInput);

        LinearLayout buttonsRow = new LinearLayout(this);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonsRow.setPadding(dp(12), dp(8), dp(12), dp(4));
        buttonsRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button buyButton = new Button(this);
        buyButton.setText("Kupno");
        buyButton.setTextColor(Color.WHITE);
        buyButton.setBackgroundColor(Color.parseColor("#2E7D32"));
        LinearLayout.LayoutParams buyLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        buyLp.setMargins(0, 0, dp(6), 0);
        buyButton.setLayoutParams(buyLp);
        buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTransaction(Transaction.TYPE_BUY);
            }
        });

        Button sellButton = new Button(this);
        sellButton.setText("Sprzedaż");
        sellButton.setTextColor(Color.WHITE);
        sellButton.setBackgroundColor(Color.parseColor("#C62828"));
        LinearLayout.LayoutParams sellLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        sellLp.setMargins(dp(6), 0, 0, 0);
        sellButton.setLayoutParams(sellLp);
        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTransaction(Transaction.TYPE_SELL);
            }
        });

        buttonsRow.addView(buyButton);
        buttonsRow.addView(sellButton);
        form.addView(buttonsRow);

        return form;
    }

    private View buildListHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.parseColor("#E0E0E0"));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        row.addView(headerCell("Typ", 1));
        row.addView(headerCell("Nazwa", 3));
        row.addView(headerCell("Ilość", 2));
        row.addView(headerCell("Cena", 2));
        row.addView(headerCell("Kwota", 3));
        return row;
    }

    private TextView headerCell(String text, int weight) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#212121"));
        tv.setPadding(dp(4), dp(6), dp(4), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        tv.setLayoutParams(lp);
        return tv;
    }

    private ListView listView;

    private View buildList() {
        listView = new ListView(this);
        listView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        adapter = new TransactionAdapter(this, transactions);
        listView.setAdapter(adapter);
        return listView;
    }

    private View buildSummaryBar() {
        summaryView = new TextView(this);
        summaryView.setPadding(dp(16), dp(16), dp(16), dp(16));
        summaryView.setTextSize(20);
        summaryView.setTypeface(summaryView.getTypeface(), Typeface.BOLD);
        summaryView.setGravity(Gravity.CENTER);
        summaryView.setBackgroundColor(Color.parseColor("#EEEEEE"));
        summaryView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return summaryView;
    }

    private void addTransaction(char type) {
        String name = nameInput.getText().toString().trim();
        String qtyStr = qtyInput.getText().toString().trim();
        String priceStr = priceInput.getText().toString().trim();
        String rateStr = rateInput.getText().toString().trim();
        String commissionStr = commissionInput.getText().toString().trim();

        if (name.length() == 0) {
            Toast.makeText(this, "Podaj nazwę papieru", Toast.LENGTH_SHORT).show();
            return;
        }

        double qty;
        double price;
        try {
            qty = Double.parseDouble(qtyStr.replace(',', '.'));
            price = Double.parseDouble(priceStr.replace(',', '.'));
        } catch (Exception e) {
            Toast.makeText(this, "Podaj poprawną ilość i cenę", Toast.LENGTH_SHORT).show();
            return;
        }
        if (qty <= 0 || price <= 0) {
            Toast.makeText(this, "Ilość i cena muszą być większe od zera", Toast.LENGTH_SHORT).show();
            return;
        }

        double rate = 1.0;
        if (rateStr.length() > 0) {
            try {
                rate = Double.parseDouble(rateStr.replace(',', '.'));
            } catch (Exception e) {
                Toast.makeText(this, "Nieprawidłowy kurs", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        double commission = 0.39;
        if (commissionStr.length() > 0) {
            try {
                commission = Double.parseDouble(commissionStr.replace(',', '.'));
            } catch (Exception e) {
                Toast.makeText(this, "Nieprawidłowa prowizja", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Transaction t = new Transaction(type, name, qty, price, rate, commission, System.currentTimeMillis());
        transactions.add(0, t);
        saveTransactions();
        refreshList();

        nameInput.setText("");
        qtyInput.setText("");
        priceInput.setText("");
        rateInput.setText("");
        commissionInput.setText("");
        nameInput.requestFocus();
    }

    private void refreshList() {
        adapter.notifyDataSetChanged();
        double total = 0;
        for (Transaction t : transactions) {
            if (t.type == Transaction.TYPE_SELL) {
                total += t.amount;
            } else {
                total -= t.amount;
            }
        }
        String sign = total >= 0 ? "+" : "-";
        String text = sign + TransactionAdapter.formatMoney(Math.abs(total)) + " zł";
        summaryView.setText(text);
        summaryView.setTextColor(total >= 0 ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
    }

    private void saveTransactions() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < transactions.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(transactions.get(i).toLine());
        }
        prefs.edit().putString(KEY_TRANSACTIONS, sb.toString()).commit();
    }

    private void loadTransactions() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(KEY_TRANSACTIONS, "");
        transactions.clear();
        if (data.length() == 0) {
            return;
        }
        String[] lines = data.split("\n");
        List<Transaction> loaded = new ArrayList<Transaction>();
        for (String line : lines) {
            Transaction t = Transaction.fromLine(line);
            if (t != null) {
                loaded.add(t);
            }
        }
        transactions.addAll(loaded);
    }
}
