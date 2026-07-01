package pl.cgielda.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity implements TransactionAdapter.RowActionListener {

    private static final String PREFS_NAME = "cgielda_prefs";
    private static final String KEY_TRANSACTIONS = "transactions";
    private static final InputFilter[] UPPERCASE_FILTERS = {new InputFilter.AllCaps()};

    private final List<Transaction> transactions = new ArrayList<Transaction>();
    private TransactionAdapter adapter;
    private ListView listView;
    private int dragFromPosition = -1;

    private TextView summaryPrimary;
    private TextView summarySecondary;
    private TextView summaryInvested;

    private AutoCompleteTextView nameInput;
    private EditText qtyInput;
    private EditText priceInput;
    private EditText rateInput;
    private EditText commissionInput;

    private LinearLayout advancedRow;
    private TextView advancedToggle;
    private boolean advancedVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Colors.BACKGROUND);

        root.addView(buildHeader());
        root.addView(buildFormCard());
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

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    // ---------- Header ----------

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(20), dp(18), dp(20));
        header.setBackgroundColor(Colors.PRIMARY);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView logo = new TextView(this);
        logo.setText("G");
        logo.setTextColor(Colors.PRIMARY);
        logo.setTypeface(logo.getTypeface(), Typeface.BOLD);
        logo.setTextSize(18);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(rounded(Color.WHITE, 10));
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        logoLp.setMargins(0, 0, dp(12), 0);
        logo.setLayoutParams(logoLp);
        header.addView(logo);

        TextView title = new TextView(this);
        title.setText("cGiełda");
        title.setTextSize(21);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        header.addView(title);

        LinearLayout spacer = new LinearLayout(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(spacer);

        TextView exportButton = headerIconButton("↑");
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExportDialog();
            }
        });
        header.addView(exportButton);

        TextView importButton = headerIconButton("↓");
        LinearLayout.LayoutParams importLp = (LinearLayout.LayoutParams) importButton.getLayoutParams();
        importLp.setMargins(dp(8), 0, 0, 0);
        header.addView(importButton);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImportDialog();
            }
        });

        return header;
    }

    private TextView headerIconButton(String glyph) {
        TextView btn = new TextView(this);
        btn.setText(glyph);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(btn.getTypeface(), Typeface.BOLD);
        btn.setTextSize(18);
        btn.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colorWithAlpha(Color.WHITE, 0x33));
        bg.setShape(GradientDrawable.OVAL);
        btn.setBackground(bg);
        btn.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        return btn;
    }

    private int colorWithAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    // ---------- Add-transaction form ----------

    private EditText makeCompactInput(String hint, int inputType, float weight) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(Colors.TEXT_MUTED);
        et.setInputType(inputType);
        et.setSingleLine(true);
        et.setTextSize(14);
        et.setTextColor(Colors.TEXT_PRIMARY);
        et.setPadding(dp(10), dp(8), dp(10), dp(8));
        et.setBackground(fieldBackground());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        et.setLayoutParams(lp);
        return et;
    }

    private AutoCompleteTextView makeNameInput(float weight) {
        AutoCompleteTextView et = new AutoCompleteTextView(this);
        et.setHint("Nazwa");
        et.setHintTextColor(Colors.TEXT_MUTED);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setFilters(UPPERCASE_FILTERS);
        et.setSingleLine(true);
        et.setTextSize(14);
        et.setTextColor(Colors.TEXT_PRIMARY);
        et.setPadding(dp(10), dp(8), dp(10), dp(8));
        et.setBackground(fieldBackground());
        et.setThreshold(1);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        et.setLayoutParams(lp);
        return et;
    }

    private GradientDrawable fieldBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Colors.BACKGROUND);
        d.setCornerRadius(dp(10));
        return d;
    }

    private View buildFormCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(10));
        card.setBackground(rounded(Colors.SURFACE, 16));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(dp(10), dp(10), dp(10), dp(6));
        card.setLayoutParams(cardLp);

        LinearLayout mainRow = new LinearLayout(this);
        mainRow.setOrientation(LinearLayout.HORIZONTAL);
        mainRow.setGravity(Gravity.CENTER_VERTICAL);
        mainRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        nameInput = makeNameInput(2f);
        qtyInput = makeCompactInput("Ilość", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 1f);
        priceInput = makeCompactInput("Cena", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 1f);

        mainRow.addView(nameInput);
        mainRow.addView(qtyInput);
        mainRow.addView(priceInput);

        advancedToggle = new TextView(this);
        advancedToggle.setTextColor(Colors.ACCENT);
        advancedToggle.setTextSize(16);
        advancedToggle.setTypeface(advancedToggle.getTypeface(), Typeface.BOLD);
        advancedToggle.setGravity(Gravity.CENTER);
        advancedToggle.setBackground(rounded(Colors.BACKGROUND, 10));
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        toggleLp.setMargins(dp(4), dp(3), dp(3), dp(3));
        advancedToggle.setLayoutParams(toggleLp);
        advancedToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAdvanced();
            }
        });
        mainRow.addView(advancedToggle);
        card.addView(mainRow);

        advancedRow = new LinearLayout(this);
        advancedRow.setOrientation(LinearLayout.HORIZONTAL);
        advancedRow.setVisibility(View.GONE);
        advancedRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        rateInput = makeCompactInput("Kurs (domyślnie 1)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 1f);
        commissionInput = makeCompactInput("Prowizja % (domyślnie 0.39)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, 1f);
        advancedRow.addView(rateInput);
        advancedRow.addView(commissionInput);
        card.addView(advancedRow);
        setAdvancedToggleText();

        LinearLayout buttonsRow = new LinearLayout(this);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonsRow.setPadding(0, dp(8), 0, dp(2));
        buttonsRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button buyButton = flatButton("Kupno", Colors.GREEN);
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

        Button sellButton = flatButton("Sprzedaż", Colors.RED);
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
        card.addView(buttonsRow);

        return card;
    }

    private Button flatButton(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        b.setBackground(rounded(color, 12));
        b.setPadding(0, dp(10), 0, dp(10));
        return b;
    }

    private void toggleAdvanced() {
        advancedVisible = !advancedVisible;
        advancedRow.setVisibility(advancedVisible ? View.VISIBLE : View.GONE);
        setAdvancedToggleText();
    }

    private void setAdvancedToggleText() {
        advancedToggle.setText(advancedVisible ? "▴" : "▾");
    }

    private void updateNameSuggestions() {
        if (nameInput == null) {
            return;
        }
        Set<String> unique = new LinkedHashSet<String>();
        for (Transaction t : transactions) {
            if (t.name != null && t.name.length() > 0) {
                unique.add(t.name.toUpperCase(Locale.getDefault()));
            }
        }
        ArrayAdapter<String> namesAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_dropdown_item_1line, new ArrayList<String>(unique));
        nameInput.setAdapter(namesAdapter);
    }

    // ---------- Transaction list ----------

    private View buildListHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(22), dp(6), dp(18), dp(4));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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
        tv.setTextColor(Colors.TEXT_MUTED);
        tv.setTextSize(11);
        tv.setPadding(dp(4), dp(2), dp(4), dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View buildList() {
        listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        adapter = new TransactionAdapter(this, transactions, this);
        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                dragFromPosition = position;
                ClipData data = ClipData.newPlainText("reorder", "");
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
                view.startDrag(data, shadow, null, 0);
                return true;
            }
        });

        listView.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        return true;
                    case DragEvent.ACTION_DRAG_LOCATION: {
                        int target = listView.pointToPosition((int) event.getX(), (int) event.getY());
                        if (target != AdapterView.INVALID_POSITION
                                && dragFromPosition != -1 && target != dragFromPosition) {
                            Transaction moved = transactions.remove(dragFromPosition);
                            transactions.add(target, moved);
                            dragFromPosition = target;
                            adapter.notifyDataSetChanged();
                        }
                        return true;
                    }
                    case DragEvent.ACTION_DROP:
                        return true;
                    case DragEvent.ACTION_DRAG_ENDED:
                        dragFromPosition = -1;
                        saveTransactions();
                        refreshList();
                        return true;
                    default:
                        return true;
                }
            }
        });

        return listView;
    }

    @Override
    public void onRowTapped(int position) {
        showEditDialog(position);
    }

    @Override
    public void onRowSwipedForDelete(int position) {
        showDeleteConfirmDialog(position);
    }

    private void styleDestructiveButton(Button b) {
        if (b == null) {
            return;
        }
        b.setText("🗑  Usuń");
        b.setTextColor(Colors.RED);
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        b.setAllCaps(false);
    }

    private void showDeleteConfirmDialog(final int position) {
        if (position < 0 || position >= transactions.size()) {
            return;
        }
        Transaction t = transactions.get(position);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Usunąć transakcję?")
                .setMessage(t.name + " - " + TransactionAdapter.formatMoney(t.amount) + " zł")
                .setPositiveButton("Usuń", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (position >= 0 && position < transactions.size()) {
                            transactions.remove(position);
                            saveTransactions();
                            refreshList();
                        }
                    }
                })
                .setNegativeButton("Anuluj", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        refreshList();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        refreshList();
                    }
                })
                .show();
        styleDestructiveButton(dialog.getButton(DialogInterface.BUTTON_POSITIVE));
    }

    private void showEditDialog(final int position) {
        if (position < 0 || position >= transactions.size()) {
            return;
        }
        final Transaction existing = transactions.get(position);

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        dialogLayout.setPadding(pad, dp(6), pad, 0);

        final AutoCompleteTextView editName = makeNameInput(1f);
        editName.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        editName.setText(existing.name);
        dialogLayout.addView(editName);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        final EditText editQty = dialogField("Ilość", TransactionAdapter.plainNumber(existing.quantity), 1f);
        final EditText editPrice = dialogField("Cena", TransactionAdapter.plainMoney(existing.price), 1f);
        row1.addView(editQty);
        row1.addView(editPrice);
        dialogLayout.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        final EditText editRate = dialogField("Kurs", TransactionAdapter.plainMoney(existing.rate), 1f);
        final EditText editCommission = dialogField("Prowizja %", TransactionAdapter.plainMoney(existing.commissionPercent), 1f);
        row2.addView(editRate);
        row2.addView(editCommission);
        dialogLayout.addView(row2);

        final TextView commissionInfo = new TextView(this);
        commissionInfo.setTextColor(Colors.TEXT_MUTED);
        commissionInfo.setTextSize(12);
        commissionInfo.setPadding(0, dp(4), 0, 0);
        dialogLayout.addView(commissionInfo);

        TextWatcher recompute = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateCommissionPreview(commissionInfo, editQty, editPrice, editRate, editCommission);
            }
        };
        editQty.addTextChangedListener(recompute);
        editPrice.addTextChangedListener(recompute);
        editRate.addTextChangedListener(recompute);
        editCommission.addTextChangedListener(recompute);
        updateCommissionPreview(commissionInfo, editQty, editPrice, editRate, editCommission);

        LinearLayout typeRow = new LinearLayout(this);
        typeRow.setOrientation(LinearLayout.HORIZONTAL);
        typeRow.setPadding(0, dp(10), 0, dp(4));
        final Button buyToggle = flatButton("Kupno", existing.type == Transaction.TYPE_BUY ? Colors.GREEN : Colors.TEXT_MUTED);
        final Button sellToggle = flatButton("Sprzedaż", existing.type == Transaction.TYPE_SELL ? Colors.RED : Colors.TEXT_MUTED);
        final char[] selectedType = {existing.type};
        LinearLayout.LayoutParams buyLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        buyLp.setMargins(0, 0, dp(6), 0);
        buyToggle.setLayoutParams(buyLp);
        LinearLayout.LayoutParams sellLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        sellLp.setMargins(dp(6), 0, 0, 0);
        sellToggle.setLayoutParams(sellLp);
        buyToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedType[0] = Transaction.TYPE_BUY;
                buyToggle.setBackground(rounded(Colors.GREEN, 12));
                sellToggle.setBackground(rounded(Colors.TEXT_MUTED, 12));
            }
        });
        sellToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedType[0] = Transaction.TYPE_SELL;
                sellToggle.setBackground(rounded(Colors.RED, 12));
                buyToggle.setBackground(rounded(Colors.TEXT_MUTED, 12));
            }
        });
        typeRow.addView(buyToggle);
        typeRow.addView(sellToggle);
        dialogLayout.addView(typeRow);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edytuj transakcję")
                .setView(dialogLayout)
                .setPositiveButton("Zapisz", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveEditedTransaction(position, existing, editName, editQty, editPrice,
                                editRate, editCommission, selectedType[0]);
                    }
                })
                .setNeutralButton("Usuń", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (position >= 0 && position < transactions.size()) {
                            transactions.remove(position);
                            saveTransactions();
                            refreshList();
                        }
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();
        styleDestructiveButton(dialog.getButton(DialogInterface.BUTTON_NEUTRAL));
    }

    private double parseOrZero(EditText field) {
        try {
            return Double.parseDouble(field.getText().toString().trim().replace(',', '.'));
        } catch (Exception e) {
            return 0;
        }
    }

    private void updateCommissionPreview(TextView commissionInfo, EditText editQty, EditText editPrice,
                                          EditText editRate, EditText editCommission) {
        double qty = parseOrZero(editQty);
        double price = parseOrZero(editPrice);
        double rate = parseOrZero(editRate);
        double commissionPct = parseOrZero(editCommission);
        double base = qty * price * rate;
        double commissionAmount = base * (commissionPct / 100.0);
        commissionInfo.setText("Prowizja: " + TransactionAdapter.formatMoney(commissionAmount) + " zł");
    }

    private EditText dialogField(String hint, String value, float weight) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(value);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        et.setLayoutParams(lp);
        return et;
    }

    private void saveEditedTransaction(int position, Transaction existing, EditText editName,
                                        EditText editQty, EditText editPrice, EditText editRate,
                                        EditText editCommission, char type) {
        String name = editName.getText().toString().trim().toUpperCase(Locale.getDefault());
        if (name.length() == 0) {
            Toast.makeText(this, "Podaj nazwę papieru", Toast.LENGTH_SHORT).show();
            return;
        }
        double qty;
        double price;
        double rate;
        double commission;
        try {
            qty = Double.parseDouble(editQty.getText().toString().trim().replace(',', '.'));
            price = Double.parseDouble(editPrice.getText().toString().trim().replace(',', '.'));
            rate = Double.parseDouble(editRate.getText().toString().trim().replace(',', '.'));
            commission = Double.parseDouble(editCommission.getText().toString().trim().replace(',', '.'));
        } catch (Exception e) {
            Toast.makeText(this, "Sprawdź wprowadzone wartości liczbowe", Toast.LENGTH_SHORT).show();
            return;
        }
        if (qty <= 0 || price <= 0) {
            Toast.makeText(this, "Ilość i cena muszą być większe od zera", Toast.LENGTH_SHORT).show();
            return;
        }
        Transaction updated = new Transaction(type, name, qty, price, rate, commission, existing.timestamp);
        if (position >= 0 && position < transactions.size()) {
            transactions.set(position, updated);
            saveTransactions();
            refreshList();
        }
    }

    // ---------- Export / Import ----------

    private void showExportDialog() {
        if (transactions.isEmpty()) {
            Toast.makeText(this, "Brak transakcji do wyeksportowania", Toast.LENGTH_SHORT).show();
            return;
        }
        String csv = TransactionCsv.export(transactions);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("cGiełda export", csv));
        }

        EditText textView = new EditText(this);
        textView.setText(csv);
        textView.setTextIsSelectable(true);
        textView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        textView.setTextSize(12);
        textView.setPadding(dp(12), dp(12), dp(12), dp(12));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(textView);

        new AlertDialog.Builder(this)
                .setTitle("Eksport (skopiowano do schowka)")
                .setView(scroll)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showImportDialog() {
        final EditText input = new EditText(this);
        input.setHint("Wklej tu wyeksportowany tekst");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(6);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
            CharSequence clip = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (clip != null && clip.toString().contains(";")) {
                input.setText(clip.toString());
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Importuj transakcje")
                .setView(input)
                .setPositiveButton("Importuj", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleImport(input.getText().toString());
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void handleImport(String text) {
        TransactionCsv.ParseResult result = TransactionCsv.parse(text);
        if (result.transactions.isEmpty()) {
            Toast.makeText(this, "Nie znaleziono żadnych transakcji w tekście", Toast.LENGTH_SHORT).show();
            return;
        }
        String message = "Znaleziono " + result.transactions.size() + " transakcji."
                + (result.skippedLines > 0 ? " Pominięto " + result.skippedLines + " nieprawidłowych linii." : "")
                + "\n\nTo zastąpi obecną listę (" + transactions.size() + " pozycji). Kontynuować?";
        new AlertDialog.Builder(this)
                .setTitle("Potwierdź import")
                .setMessage(message)
                .setPositiveButton("Zastąp listę", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        transactions.clear();
                        transactions.addAll(result.transactions);
                        saveTransactions();
                        refreshList();
                        Toast.makeText(MainActivity.this, "Zaimportowano " + result.transactions.size()
                                + " transakcji", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    // ---------- Summary bar ----------

    private View buildSummaryBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(dp(20), dp(16), dp(20), dp(16));
        bar.setBackground(topRounded(Colors.SURFACE, 20));
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView primaryLabel = new TextView(this);
        primaryLabel.setText("Zrealizowany wynik");
        primaryLabel.setTextSize(11);
        primaryLabel.setTextColor(Colors.TEXT_MUTED);
        bar.addView(primaryLabel);

        summaryPrimary = new TextView(this);
        summaryPrimary.setTextSize(26);
        summaryPrimary.setTypeface(summaryPrimary.getTypeface(), Typeface.BOLD);
        bar.addView(summaryPrimary);

        summarySecondary = new TextView(this);
        summarySecondary.setTextSize(13);
        summarySecondary.setTypeface(summarySecondary.getTypeface(), Typeface.BOLD);
        summarySecondary.setPadding(0, dp(4), 0, 0);
        bar.addView(summarySecondary);

        summaryInvested = new TextView(this);
        summaryInvested.setTextSize(11);
        summaryInvested.setTextColor(Colors.TEXT_MUTED);
        summaryInvested.setPadding(0, dp(2), 0, 0);
        bar.addView(summaryInvested);

        return bar;
    }

    private GradientDrawable topRounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        float r = dp(radiusDp);
        d.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        return d;
    }

    // ---------- Add transaction ----------

    private void addTransaction(char type) {
        String name = nameInput.getText().toString().trim().toUpperCase(Locale.getDefault());
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
        updateNameSuggestions();
        PnlCalculator.Result result = PnlCalculator.compute(transactions);

        summaryPrimary.setText(signedMoney(result.realizedPnl));
        summaryPrimary.setTextColor(result.realizedPnl >= 0 ? Colors.GREEN : Colors.RED);

        summarySecondary.setText("Saldo gotówkowe: " + signedMoney(result.netCashFlow));
        summarySecondary.setTextColor(result.netCashFlow >= 0 ? Colors.GREEN : Colors.RED);

        summaryInvested.setText("Zainwestowano obecnie: "
                + TransactionAdapter.formatMoney(result.openCostBasis) + " zł");
    }

    private String signedMoney(double value) {
        String sign = value >= 0 ? "+" : "-";
        return sign + TransactionAdapter.formatMoney(Math.abs(value)) + " zł";
    }

    // ---------- Persistence ----------

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
        boolean nameNeedsMigration = false;
        for (String line : lines) {
            Transaction t = Transaction.fromLine(line);
            if (t != null) {
                if (t.name != null) {
                    String upper = t.name.trim().toUpperCase(Locale.getDefault());
                    if (!upper.equals(t.name)) {
                        t.name = upper;
                        nameNeedsMigration = true;
                    }
                }
                loaded.add(t);
            }
        }
        transactions.addAll(loaded);
        if (nameNeedsMigration) {
            // Older data saved before names were forced to uppercase - persist
            // the normalized form so matching (PnlCalculator) stays correct.
            saveTransactions();
        }
    }
}
