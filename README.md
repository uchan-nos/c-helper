# C-Helper
C-Helper は、C言語初学者向けの静的解析ツールです。
ソースコードから、初学者が犯しやすい問題点を発見し指摘します。

## インストール
C-Helper は Eclipse プラグインです。
Eclipseの入手からC-Helperをインストールするまでの手順を説明します。

### All in One Eclipse の準備
Eclipse は公式サイトや有志の手によって、様々なパッケージが提供されています。
ここでは、初学者にとって最も手軽と思われる All in One Eclipse の導入方法を説明します。

All in One Eclipse は Windows 向けしか配布されていませんので、
他の OS をお使いの人は Eclipse の公式サイトなどから、対応している Eclipse をダウンロードしてください。

1. ダウンロード  
All in One Eclipse は MergeDoc プロジェクトが配布しているパッケージで、
その名の通り便利なプラグインが最初から同梱されています。
日本語化もされています。

    ダウンロードは [Pleiades - Eclipse プラグイン日本語化プラグイン](mergedoc.sourceforge.jp) からできます。
この文書の執筆時は "Eclipse 4.2 Juno Pleiades All in One" が最新でした。
最新版をダウンロードしましょう。

    All in One Eclipse は開発対象に応じて Platform, Ultimate, Java, C/C++ などの種類が用意されています。
さらに、それぞれの種類に Full Edition または Standard Edition があります。
付属しているプラグイン、ソフトウェアが違います。
C 言語の開発をするには Ultimate または C/C++ の Full Edition を選択してください。

2. インストール  
All in One Eclipse のインストールはとても簡単です。
ダウンロードした zip ファイルを適当な場所に解凍するだけです。
pleiades-e4.2-cpp-32bit-jre\_20121123.zip のようなファイルがダウンロードされるはずですので、
これを適当なディレクトリに解凍します。

### C-Helper の準備
C-Helper は Eclipse のプラグインであり、現状で JAR ファイルの形で配布されています。

1. ダウンロード  
[uchan\_nos/c-helper/plugins](https://github.com/uchan-nos/c-helper/tree/master/plugins)
から JAR ファイルをダウンロードできます。
com.github.uchan\_nos.c\_helper\_1.0.0.201302181105.jar のようなファイル名をクリックすると
Raw, History のようなボタンがあります。
Raw をクリックすればファイルをダウンロードできます。

2. インストール  
ダウンロードしたファイルを Eclipse の dropins ディレクトリに配置します。
All in One Eclipse ならば pleiades/eclipse/dropins ディレクトリに先ほどの JAR ファイルを入れます。
以上で C-Helper の準備は完了です。

### 使用方法
![C-Helper icon](https://github.com/uchan-nos/c-helper/raw/master/icons/analysis.png)
が C-Helper を起動するアイコンです。
何らかの C 言語プログラムのソースコードをエディタで開いている状態でこのアイコンをクリックすれば、
即座に解析が行われて「問題」ビューに検出した問題が一覧表示されます。
