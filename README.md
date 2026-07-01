# cGiełda

Prosta aplikacja na Androida do liczenia wyceny rachunku giełdowego na podstawie
ręcznie wprowadzanych transakcji kupna/sprzedaży.

## Funkcje

- Kompaktowy formularz dodawania transakcji (nazwa, ilość, cena w jednej linii),
  z opcjonalnym kursem (domyślnie `1`) i prowizją banku (domyślnie `0.39%`)
  schowanymi pod ikonką ▾ w tym samym wierszu.
- Pole nazwy podpowiada wcześniej wpisane nazwy papierów (unikalne, jak
  select-box) i wymusza WIELKIE LITERY zarówno przy wpisywaniu, jak i na
  liście.
- Lista transakcji na ekranie głównym (najnowsze na górze), każdy wiersz w formie
  karty pokazuje: typ (**K** zielony / **S** czerwony), nazwę papieru, ilość,
  cenę i wyliczoną kwotę transakcji (z uwzględnieniem prowizji). Kwoty
  formatowane są z odstępem tysięcy (np. `20 000,00 zł`).
  - **Stuknięcie** w wiersz otwiera kompaktową edycję transakcji (z podglądem
    kwoty prowizji na żywo) albo jej usunięcie (czytelny czerwony przycisk 🗑 Usuń).
  - **Przesunięcie w prawo** pokazuje potwierdzenie usunięcia (też czerwony przycisk z ikoną).
  - **Przytrzymanie** wiersza pozwala przeciągnięciem zmienić jego kolejność na liście.
- Eksport/import transakcji jako tekst (przyciski ↑/↓ w nagłówku): eksport
  kopiuje listę do schowka (i pokazuje ją do ręcznego skopiowania), import
  wkleja tekst i odtwarza z niego listę transakcji — wygodne do przeniesienia
  danych między urządzeniami albo ręcznej kopii zapasowej.
- Pasek podsumowania przyklejony do dołu ekranu, trzy liczby:
  - **Zrealizowany wynik** (duża liczba) — faktyczny zysk/strata licząc każdą
    sprzedaż jako dopasowaną do najtańszego dostępnego w danym momencie zakupu.
  - **Saldo gotówkowe** — to samo co zrealizowany wynik, ale liczone jako
    różnica kwot (uzyskana ze sprzedaży minus wydana na zakup) tylko dla
    dopasowanych (rozliczonych) transakcji — nie uwzględnia kosztu akcji
    wciąż trzymanych w portfelu.
  - **Zainwestowano obecnie** (mała czcionka) — ile pieniędzy tkwi w akcjach
    wciąż trzymanych w portfelu (jeszcze niesprzedanych).
- Dane zapisywane lokalnie na urządzeniu, przeżywają zamknięcie aplikacji
  i (dzięki stabilnemu podpisowi APK, patrz niżej) również aktualizację appki.

Kwota transakcji liczona jest jako `ilość × cena × kurs`, powiększona o prowizję
przy kupnie, pomniejszona o prowizję przy sprzedaży.

**Zrealizowany wynik** liczony jest osobno dla każdego papieru: każda sprzedaż
jest dopasowywana do **najtańszego** jeszcze nierozliczonego zakupu tego
samego papieru spośród wszystkich zakupów zaksięgowanych do tego momentu
(niezależnie od kolejności, w jakiej zostały wpisane) — a zysk/strata to
różnica między kwotą uzyskaną ze sprzedaży a kosztem zakupu dopasowanych
akcji. Np. przy zakupach po 220/230/240 zł i sprzedaży po 222 zł, sprzedaż
zawsze dopasowywana jest do zakupu po 220 zł (dając zysk), niezależnie od tego,
w jakiej kolejności te zakupy zostały dodane. Akcje, które wciąż są w
portfelu, nie są liczone jako strata — liczy się tylko to, co faktycznie
zostało zrealizowane. Nadal obowiązuje reguła, że sprzedaż może dopasować się
tylko do zakupów wprowadzonych *wcześniej w czasie* (na podstawie rzeczywistej
chronologii, nie kolejności na liście, którą możesz swobodnie zmieniać
przeciąganiem) — nie można "sprzedać" akcji z przyszłego zakupu.

### Format eksportu/importu

Eksport tworzy tekst rozdzielany średnikami, jedna transakcja na linię:

```
TYP;NAZWA;ILOSC;CENA;KURS;PROWIZJA%;DATA
K;ABC;100;12.50;1;0.39;2026-07-01 10:15:00
```

Import akceptuje ten sam format (nagłówek jest opcjonalny/pomijany), zamienia
całą obecną listę transakcji na tę wklejoną — po potwierdzeniu w dialogu.

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

Kolejne wersje możesz instalować **na wierzch** poprzedniej (Android zaktualizuje
appkę i zachowa zapisane transakcje) — nie trzeba już odinstalowywać starej wersji.

Wymaga Androida 5.0+ (`minSdkVersion 21`), celuje w `targetSdkVersion 29`.

## Budowanie ze źródeł (lokalnie)

```
./build.sh
```

Wynikiem jest podpisany `dist/cGielda-1.0.apk`. Wymaga JDK 17+, `curl`, `git` i
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
  ARSCLib) do złożenia `AndroidManifest.xml`, `resources.arsc`, ikony i
  finalnego APK bez potrzeby `aapt`/`aapt2`,
- `jarsigner` z JDK do podpisania APK (schemat podpisu v1/JAR),
- `tools/IconGen.java` — mały programik rysujący ikonę aplikacji (Java2D,
  bez zewnętrznych zależności) na etapie builda.

Interfejs jest budowany w 100% programistycznie w Javie (bez plików `res/layout`),
więc aplikacja nie potrzebuje AndroidX/Material — kolory, zaokrąglone rogi kart
i przyciski rysowane są bezpośrednio przez `GradientDrawable` w kodzie.

### Stabilny podpis APK (dlaczego aktualizacja już działa)

Wcześniej `build.sh` przy braku pliku keystore generował nowy, losowy klucz
podpisujący za każdym razem, gdy budował od zera — a że katalog roboczy CI nie
jest niczym trwałym między przebiegami, **każdy build z GitHub Actions miał
inny podpis**. Android traktuje różnie podpisane APK jako różne aplikacje i
odmawia instalacji "na wierzch" istniejącej (`app not installed - conflicts
with an existing package`) — stąd konieczność odinstalowania starej wersji.

Naprawione: `signing/cgielda-release.keystore` jest teraz zacommitowany do
repo i `build.sh` zawsze go używa (nigdy nie generuje nowego). Każdy build -
lokalny czy z CI - ma odtąd ten sam podpis, więc kolejne wersje instalują się
jako aktualizacja z zachowaniem danych. `versionCode` jest też automatycznie
zwiększany (liczba commitów w repo), żeby Android poprawnie rozpoznawał nowsze
wersje.

Uwaga: to jest podpis *deweloperski* (self-signed, hasło w repo) — wystarczający
do spójnych aktualizacji własnej instalacji, ale nie należy traktować go jako
sekretu chroniącego przed podszywaniem się pod aplikację.

## Struktura

```
app/
  AndroidManifest.xml
  src/pl/cgielda/app/
    MainActivity.java       - ekran główny, formularz, lista, pasek podsumowania, dialogi edycji/importu/eksportu
    Transaction.java        - model transakcji i logika wyliczania kwoty transakcji
    PnlCalculator.java      - wynik FIFO, saldo gotówkowe, kapitał obecnie zainwestowany
    TransactionAdapter.java - karty wierszy listy, gest przesunięcia do usunięcia, formatowanie liczb
    TransactionCsv.java     - eksport/import transakcji jako tekst
    Colors.java             - wspólna paleta kolorów
tools/IconGen.java           - generator ikony aplikacji (uruchamiany przez build.sh)
signing/cgielda-release.keystore - stabilny klucz podpisujący (patrz sekcja wyżej)
build.sh                    - reprodukowalny skrypt budowania i podpisywania APK
.github/workflows/build-apk.yml - CI: buduje APK i publikuje go (artefakt + release "latest")
```
