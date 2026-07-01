# cGiełda

Prosta aplikacja na Androida do liczenia wyceny rachunku giełdowego na podstawie
ręcznie wprowadzanych transakcji kupna/sprzedaży.

## Funkcje

- Dodawanie transakcji: nazwa papieru, ilość akcji, cena akcji, opcjonalny kurs
  (domyślnie `1`) i opcjonalna prowizja banku (domyślnie `0.39%`).
- Kompaktowy formularz dodawania transakcji (nazwa, ilość, cena w jednej linii),
  pola kurs/prowizja domyślnie ukryte pod przełącznikiem "Kurs / prowizja (pokaż)".
- Lista transakcji na ekranie głównym (najnowsze na górze), każdy wiersz pokazuje:
  typ (**K** zielony / **S** czerwony), nazwę papieru, ilość, cenę i wyliczoną
  kwotę transakcji (z uwzględnieniem prowizji).
- Pasek podsumowania przyklejony do dołu ekranu: **faktyczny zrealizowany
  zysk/strata** (nie suma wszystkich przepływów), na zielono gdy dodatni,
  na czerwono gdy ujemny.
- Dane zapisywane lokalnie na urządzeniu (przeżywają zamknięcie aplikacji).

Kwota transakcji liczona jest jako `ilość × cena × kurs`, powiększona o prowizję
przy kupnie, pomniejszona o prowizję przy sprzedaży.

Wynik na dole ekranu to **zrealizowany zysk/strata**, liczony metodą FIFO
osobno dla każdego papieru: każda sprzedaż jest dopasowywana do najstarszych
jeszcze nierozliczonych zakupów tego samego papieru, a zysk/strata to różnica
między kwotą uzyskaną ze sprzedaży a kosztem zakupu dopasowanych akcji. Akcje,
które wciąż są w portfelu (kupione, ale jeszcze nie sprzedane), nie są liczone
jako strata — liczy się tylko to, co faktycznie zostało zrealizowane.

## Instalacja

APK budowany jest automatycznie przez GitHub Actions przy każdym pushu
(`.github/workflows/build-apk.yml`):

- **Release "latest"** — zakładka *Releases* w repo zawsze ma najnowszy
  `cGielda-1.0.apk` gotowy do pobrania jednym kliknięciem.
- **Artefakt buildu** — każdy przebieg w zakładce *Actions* ma artefakt
  `cGielda-apk` do pobrania (spakowany w zip).

1. Pobierz plik `.apk` na telefon z Androidem.
2. Otwórz go (np. z Pobranych plików) i zezwól na instalację z tego źródła,
   jeśli system o to zapyta.
3. Ponieważ APK jest podpisany własnym (self-signed) certyfikatem deweloperskim,
   a nie certyfikatem ze Sklepu Play, Android pokaże ostrzeżenie o
   "nieznanym źródle" — to normalne przy instalacji poza Sklepem Play.

Wymaga Androida 5.0+ (`minSdkVersion 21`), celuje w `targetSdkVersion 29`.

## Budowanie ze źródeł (lokalnie)

```
./build.sh
```

Wynikiem jest podpisany `dist/cGielda-1.0.apk`. Wymaga JDK 17+, `curl` i
dostępu do `repo.maven.apache.org` oraz `github.com` — dokładnie tego samego
używa workflow w GitHub Actions.

### Dlaczego nie ma tu `build.gradle` / Android Studio?

Standardowa ścieżka budowania (Android Gradle Plugin + AndroidX) wymaga pobrania
zależności z `dl.google.com` / `maven.google.com`. W niektórych środowiskach
sieciowych (m.in. tym, w którym powstał ten projekt) te domeny są zablokowane
politykami sieciowymi, przez co standardowy build Gradle nie może się nawet
zsynchronizować.

Zamiast tego `build.sh` buduje APK z wyłącznie publicznie dostępnych, otwartych
narzędzi z Maven Central i GitHuba:

- `com.google.android:android` (Maven Central) jako `android.jar` do kompilacji,
- kompilator `dx` (konwersja `.class` → `.dex`) zbudowany z oryginalnego
  źródła AOSP (`aosp-mirror/platform_dalvik`),
- [REAndroid/APKEditor](https://github.com/REAndroid/APKEditor) (oparty o
  ARSCLib) do złożenia `AndroidManifest.xml`, `resources.arsc` i finalnego APK
  bez potrzeby `aapt`/`aapt2`,
- `jarsigner` z JDK do podpisania APK (schemat podpisu v1/JAR).

Interfejs jest budowany w 100% programistycznie w Javie (bez plików `res/layout`),
więc aplikacja nie potrzebuje żadnych zasobów graficznych ani AndroidX/Material —
stąd też brak własnej ikony aplikacji (używana jest domyślna ikona systemowa).

## Struktura

```
app/
  AndroidManifest.xml
  src/pl/cgielda/app/
    MainActivity.java       - ekran główny, formularz, lista, pasek podsumowania
    Transaction.java        - model transakcji i logika wyliczania kwoty transakcji
    PnlCalculator.java      - liczenie zrealizowanego zysku/straty metodą FIFO
    TransactionAdapter.java - renderowanie wiersza listy
build.sh                    - reprodukowalny skrypt budowania i podpisywania APK
.github/workflows/build-apk.yml - CI: buduje APK i publikuje go (artefakt + release "latest")
```
