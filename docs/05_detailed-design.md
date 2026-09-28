# Knitting 詳細設計書

## 1. 文書情報・目的

### 文書情報

| 項目 | 内容 |
|---|---|
| 対象システム | Knitting |
| 文書種別 | 詳細設計書 |
| 文書状態 | 詳細設計01〜13 統合確定版 |
| 作成日 | 2026年9月11日 |
| 実装状況 | 本書作成時点では機能実装前 |
| 技術前提 | Java 25 / Spring Boot 4.0.8 / Maven / Eclipse / Spring MVC / Thymeleaf / HTML / CSS / 必要最小限のJavaScript / Spring Data JPA / Spring Security / MySQL Community 8.4 LTS / Flyway |

### 本書の目的

本書は、Knittingの詳細設計01〜13で確定した内容を、実装時の正式な参照資料として1つのMarkdownファイルへ統合したものである。単独でURL、クラス責務、永続化、認証・認可、画像、自動保存、例外、入力検証、画面動作、環境設定およびテストの設計を確認できる状態を目的とする。

同一事項に差異がある場合は後続の詳細設計を優先し、先行資料の「後続で決定」は後続資料の確定内容へ置き換えた。新規仕様は追加していない。

### 詳細設計01〜13との対応

| 詳細設計 | 統合先 | 正式テーマ |
|---|---|---|
| 01 | 第2章 | 全体方針・技術構成 |
| 02 | 第3章 | URL・ルーティング設計 |
| 03 | 第4章 | DB・JPA設計 |
| 04 | 第5章 | 認証・認可設計 |
| 05 | 第6章 | 画像保存設計 |
| 06 | 第7章 | 自動保存・非同期更新設計 |
| 07 | 第8章 | 共通例外・エラー処理設計 |
| 08 | 第9章 | Form・Validation設計 |
| 09 | 第10章 | Controller・Service設計 |
| 10 | 第11章 | 計算ロジック・毛糸対応基準設計 |
| 11 | 第12章 | Thymeleaf・JavaScript・画面動作設計 |
| 12 | 第13章 | 環境設定・application設定設計 |
| 13 | 第14章 | テスト設計 |

> 添付ファイル名と本文番号が食い違う2資料は、本文の正式番号を採用した。詳細設計05は画像保存設計、詳細設計06は自動保存・非同期更新設計である。

---

## 2. 全体方針・技術構成

### 技術構成

| 区分 | 採用技術・方式 |
|---|---|
| Java | Java 25 |
| フレームワーク | Spring Boot 4.0.8 |
| Web | Spring MVC |
| テンプレート | Thymeleafによるサーバーサイドレンダリング |
| フロントエンド | HTML / CSS / 必要最小限のJavaScript |
| 永続化 | Spring Data JPA / Hibernate |
| 認証・認可 | Spring Security |
| データベース | MySQL Community 8.4 LTS系列 |
| マイグレーション | Flyway |
| ビルド | Maven |
| 開発環境・管理 | Eclipse / Git / GitHub |

SPAや別フロントエンドアプリは採用しない。基本処理経路は次のとおりとする。

```text
Thymeleaf
↓
Form
↓
Controller
↓
Service
↓
Repository
↓
Entity
↓
MySQL
```

表示用・層間データに必要な場合のみDTOを使用する。Entityを画面入力Formとして直接使用しない。Service interface、独自DAO層、機械的なEntity別DTO、Mapper / MapStruct等は導入しない。

### レイヤーと責務

| レイヤー | 主な責務 |
|---|---|
| Controller | HTTP受付、Form受取、Principal取得、Service呼出、遷移・Model・レスポンス制御 |
| Service | 業務ルール、所有者確認、Entity生成・更新、Repository呼出、トランザクション、日時制御 |
| Repository | Spring Data JPAによる検索・永続化・ロック |
| Entity | MySQL永続データのJava表現 |
| Form | Web入力の受取、型変換、Bean Validation |
| DTO | 必要な結果・表示データだけを受け渡す |
| Thymeleaf / JavaScript | サーバー描画、補助的な画面操作、非同期保存。正式な業務検証はサーバー側で行う |

パッケージはpackage by layerを基本とし、
`controller`、`service`、`repository`、`entity`、`domain`、`form`、
必要時の`dto`、`security`、`config`、`exception`を使用する。詳細なクラス配置は第10章を正とする。

### 共通実装原則

- ControllerからRepositoryを直接呼ばず、Serviceを介する。
- Service層を原則的なトランザクション境界とする。個別指定は第10章を正とする。
- 画面入力はFormとBean Validationで正式に検証し、業務状態を要する検証はServiceで行う。DB制約でも整合性を保護する。
- 共通化できる例外処理は一元管理し、入力・業務・システムエラーを区別する。
- 日時は`Asia/Tokyo`基準とし、ServiceへDIした`Clock`から取得する。
- Spring Bootのログ機構を使用し、`System.out.println`を運用ログに使わない。
- password、password hash、Remember-Me token/key、Session ID、CSRF token等をログへ出力しない。
- 秘密情報をJavaソースへ直書きせず、環境設定と分離し、Git/GitHubへ登録しない。

---

## 3. URL・ルーティング設計

### 共通方針

- URLは小文字を基本とし、複数語はハイフンで区切る。日本語および`.html`は使用しない。
- 作品は`/works`を基点とし、個別作品は`/works/{id}`で表す。
- GETは画面表示・検索・計算等、サーバー状態を変更しない処理に使用する。
- POSTは登録・更新・削除・ログイン・ログアウト等、状態を変更する処理に使用する。
- 通常の登録・明示保存・削除成功後はPRG（Post → Redirect → Get）を使用する。
- 作品ルートではPathのIDだけを信用せず、ログインユーザーの所有者条件を必ず適用する。

### ルート一覧

| 画面・区分 | HTTP | URL / エンドポイント | 利用条件・処理 |
|---|---|---|---|
| S-01 トップ | GET | `/` | 公開。認証状態で表示を切替 |
| S-02 ユーザー登録 | GET | `/register` | 未ログイン向け表示 |
| S-02 ユーザー登録 | POST | `/register` | 成功後`/login?registered` |
| S-03 ログイン | GET | `/login` | 未ログイン向け表示 |
| S-03 ログイン | POST | `/login` | Spring Securityが処理 |
| S-04 ゲージ調整 | GET | `/gauge-adjustment` | 公開。6入力項目をquery parameterとして計算 |
| S-05 2本取り・引き揃え | GET | `/yarn-combination` | 公開。計算要求は`calculate=true` |
| S-06 簡易カウンター | GET | `/counter` | 公開。操作はJavaScript + localStorage |
| S-07 対応表 | GET | `/yarn-guide` | 公開 |
| S-08 作品登録 | GET | `/works/new` | 認証必須。初期表示statusは`IN_PROGRESS` |
| S-08 作品登録 | POST | `/works` | 認証必須。選択statusを保存。成功後`/works/{id}` |
| S-09 制作中一覧 | GET | `/works/in-progress` | 認証必須。`category`、`page`を任意指定 |
| S-10 完成一覧 | GET | `/works/completed` | 認証必須。`category`、`page`を任意指定 |
| S-11 作品詳細 | GET | `/works/{id}` | 認証必須・本人作品のみ。任意`from`を受け付ける |
| S-11 作品画像 | GET | `/works/{id}/image` | 認証必須・本人作品のみ |
| S-11 基本情報保存 | POST | `/works/{id}/basic-info` | multipart、明示保存 |
| S-11 カウンター保存 | POST | `/works/{id}/counter` | 非同期自動保存 |
| S-11 制作メモ保存 | POST | `/works/{id}/memo` | 非同期自動保存 |
| S-11 ステータス保存 | POST | `/works/{id}/status` | 非同期自動保存 |
| S-11 完成日保存 | POST | `/works/{id}/completion-date` | 非同期自動保存 |
| S-11 作品削除 | POST | `/works/{id}/delete` | 成功後`/` |
| 共通 ログアウト | POST | `/logout` | Spring Securityが処理。成功後`/` |

### 計算・一覧のquery parameter

- S-04は`specifiedStitchGauge`、`measuredStitchGauge`、`originalStitchCount`、`specifiedRowGauge`、`measuredRowGauge`、`originalRowCount`を使用する。6項目すべて未入力なら初期表示とし、`calculate=true`は使用しない。
- S-05は`mode`および選択モードの重量・長さ項目を使用し、初期表示と計算要求の区別に`calculate=true`を使用する。
- S-09 / S-10の`page`はURL上1始まり、未指定時は1。`category`は任意で、未指定は「すべて」。不正category、`page < 1`、範囲外pageは400とする。

### S-11の遷移元保持

- S-09からは`/works/{id}?from=in-progress`、S-10からは`/works/{id}?from=completed`で遷移する。
- 有効な`from`がある場合、S-11内でstatusを変更しても「一覧へ戻る」とナビゲーション現在地は遷移元一覧を維持する。
- `from`なしの場合は現在の作品statusから戻り先を決める。不正な`from`は`from`なしとして扱う。
- 基本情報保存後のPRGでも有効な`from`を引き継ぐ。
- 自動保存APIへ`from`を送らない。DB、Session、Cookie、Referer、`history.back()`へ依存しない。

### 認証に伴う遷移

- 未認証で通常の保護画面へアクセスした場合は`/login?required`へ移動し、元URLへ自動復帰しない。
- ログイン成功後は`/`、失敗時は`/login?error`、登録完了後は`/login?registered`とする。
- ログイン済みで`GET /login`または`GET /register`へアクセスした場合は`/`へ移動する。

---

## 4. DB・JPA設計

### 採用技術

#### データベース

- MySQL Community Editionを使用する。
- MySQLは **8.4 LTS系列**を採用する。
- パッチバージョンは設計上固定しない。
- 環境構築時点で利用可能な最新の8.4.xを使用する。

#### ORM / 永続化

- Spring Data JPAを使用する。
- JPA実装はSpring Bootが提供するHibernateを使用する。
- RepositoryはSpring Data JPAの `JpaRepository` を利用する。
- 独自DAO層は設けない。

#### DBマイグレーション

- Flywayを採用する。
- DB構造の作成・変更はFlywayのmigration SQLで管理する。
- HibernateによるDBの自動生成・自動更新は使用しない。

---

### 文字コード・照合順序

#### 文字コード

MySQLの文字コードは以下とする。

```text
utf8mb4
```

日本語および絵文字を含むUnicode文字列を保存可能とする。

#### 基本照合順序

基本照合順序は以下とする。

```text
utf8mb4_0900_ai_ci
```

文字列比較では原則として大文字・小文字を区別しない。

#### username

`username` は、入力時の大文字・小文字を保持して保存する。

例：

```text
入力・保存値: Mame123
```

ただし、ログインおよび一意性判定では大文字・小文字を区別しない。

以下は同一usernameとして扱う。

```text
Mame123
mame123
MAME123
```

DB側では、case-insensitiveな照合順序とUNIQUE制約を組み合わせて、大文字・小文字だけが異なるusernameの重複登録を防ぐ。

Java側でも `existsByUsernameIgnoreCase()` 等を使用して登録前確認を行う。

#### email

emailは保存前にJava側で小文字へ正規化する。

例：

```text
入力: Mame@Example.com
保存: mame@example.com
```

大文字・小文字の表記を保持する要件はないため、DB上の表記を小文字に統一する。

DB側ではUNIQUE制約によって重複登録を防ぐ。

---

### Entity構成

Entityは以下の2クラスとする。

- `User`
- `Work`

対応テーブルは以下とする。

| Entity | DBテーブル |
|---|---|
| `User` | `users` |
| `Work` | `works` |

---

### Entity間の関連

`User` と `Work` は1対多の関係である。

ただしJPA Entityでは、**Work → User の単方向関連**とする。

#### Work側

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

#### User側

`User` Entityには `List<Work>` を持たせない。

理由：

- ログインユーザーの作品はRepositoryから条件検索する。
- 双方向関連を持たせる必要性がない。
- 不要なコレクション管理や遅延ロードの複雑化を避ける。
- Entityを必要以上に密結合にしない。

---

### Java型とDB型

#### User

| Javaフィールド | Java型 | DBカラム | DB型 |
|---|---|---|---|
| `id` | `Long` | `id` | `BIGINT` |
| `email` | `String` | `email` | `VARCHAR(255)` |
| `username` | `String` | `username` | `VARCHAR(20)` |
| `passwordHash` | `String` | `password_hash` | `VARCHAR(255)` |
| `createdAt` | `LocalDateTime` | `created_at` | `DATETIME` |

#### Work

| Javaフィールド | Java型 | DBカラム | DB型 |
|---|---|---|---|
| `id` | `Long` | `id` | `BIGINT` |
| `user` | `User` | `user_id` | `BIGINT` |
| `name` | `String` | `name` | `VARCHAR(50)` |
| `photoReference` | `String` | `photo_reference` | `VARCHAR(500)` |
| `category` | `WorkCategory` | `category` | `VARCHAR(20)` |
| `status` | `WorkStatus` | `status` | `VARCHAR(20)` |
| `stitchCount` | `int` | `stitch_count` | `INT` |
| `rowCount` | `int` | `row_count` | `INT` |
| `memo` | `String` | `memo` | `VARCHAR(1000)` |
| `completionDate` | `LocalDate` | `completion_date` | `DATE` |
| `lastUpdatedAt` | `LocalDateTime` | `last_updated_at` | `DATETIME` |

---

### Enum設計

#### WorkCategory

Javaでは `WorkCategory` enumを使用する。

```text
HAT
TOP
BOTTOM
SCARF
BAG
TOY
SMALL_GOODS
OTHER
```

表示上の意味は以下とする。

| 値 | 表示 |
|---|---|
| `HAT` | 帽子 |
| `TOP` | 上衣 |
| `BOTTOM` | 下衣 |
| `SCARF` | マフラー |
| `BAG` | バッグ |
| `TOY` | ぬいぐるみ |
| `SMALL_GOODS` | 小物 |
| `OTHER` | その他 |

#### WorkStatus

Javaでは `WorkStatus` enumを使用する。

```text
IN_PROGRESS
COMPLETED
```

表示上の意味は以下とする。

| 値 | 表示 |
|---|---|
| `IN_PROGRESS` | 制作中 |
| `COMPLETED` | 完成 |

#### JPAマッピング

Enumは以下の方針とする。

```java
@Enumerated(EnumType.STRING)
```

`EnumType.ORDINAL` は使用しない。

DB側ではMySQL ENUM型を使用せず、`VARCHAR(20)` とCHECK制約を組み合わせる。

---

### ID採番

`users.id` および `works.id` はMySQLの `AUTO_INCREMENT` を使用する。

JPA側では以下とする。

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

EntityのID型は、永続化前に `null` を表現できる `Long` を使用する。

---

### 初期値・DEFAULTの管理方針

業務的な意味を持つ初期値はJava側で明示的に設定する。

単純で固定的な安全値についてはDB DEFAULTも併用する。

#### Java側で管理するもの

- `User.createdAt`
- `Work.lastUpdatedAt`
- `Work.status`
- `Work.completionDate`

新規作品登録画面の初期表示ではJava側で `status = IN_PROGRESS` を設定する。登録時はユーザーが選択したstatusを保存し、status欠落はValidationエラーとする。`IN_PROGRESS`なら`completionDate = null`、`COMPLETED`なら`completionDate = LocalDate.now(clock)`とする。

`status` にはDB DEFAULTを設定しない。

#### Java＋DB DEFAULTを併用するもの

以下はJava側でも初期化し、DB側にもDEFAULTを持たせる。

```text
stitch_count = 0
row_count = 0
memo = ''
```

#### NULLの扱い

`photo_reference` は画像未登録時に `NULL` とする。

空文字 `''` を「画像なし」として使用しない。

`memo` はNULLを使用せず、未入力時は空文字 `''` とする。

---

### 日時管理

#### 基本方針

日時はJavaアプリケーション側で管理する。

MySQLの以下の自動設定には依存しない。

```text
DEFAULT CURRENT_TIMESTAMP
ON UPDATE CURRENT_TIMESTAMP
```

#### タイムゾーン

アプリケーションの日時基準は `Asia/Tokyo` とする。

#### Clock

Java標準の `java.time.Clock` をSpring Beanとして登録する。

概念例：

```java
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Tokyo"));
    }
}
```

Serviceへ `Clock` をDIし、以下のように使用する。

```java
LocalDateTime.now(clock)
LocalDate.now(clock)
```

業務ロジック内で `LocalDateTime.now()` や `LocalDate.now()` を直接使用しない。

これによりテスト時に固定Clockへ差し替え可能とする。

#### last_updated_at更新対象

`last_updated_at` は以下の変更時に更新する。

- 目数変更
- 段数変更
- メモ変更
- 作品名変更
- 写真変更
- 写真削除
- カテゴリー変更
- ステータス変更

以下では更新しない。

- 作品詳細画面を表示しただけの場合
- 完成日のみを手動変更した場合

---

### Repository設計

#### 基本方針

以下のRepositoryを作成する。

```text
UserRepository
WorkRepository
```

両方とも `JpaRepository<対象Entity, Long>` を継承する。

独自DAO層は作成しない。

単純な検索はSpring Data JPAの派生クエリを使用する。

派生クエリで可読性が悪くなる場合や複雑な条件が必要な場合は `@Query` を使用する。

Specificationは初期版では使用しない。

---

### UserRepository

主なメソッド候補は以下とする。

```java
Optional<User> findByUsernameIgnoreCase(String username);

boolean existsByUsernameIgnoreCase(String username);

boolean existsByEmail(String email);
```

用途：

- usernameによるログイン対象ユーザー取得
- username重複確認
- email重複確認

emailはJava側で小文字正規化した上で扱う。

---

### WorkRepository

#### 所有者を含む作品取得

作品詳細や更新では、IDのみではなく所有ユーザーも検索条件に含める。

```java
Optional<Work> findByIdAndUser(Long id, User user);
```

他ユーザーの作品を取得しないための防御として使用する。

最終的な認可ルールはServiceおよびSpring Security側でも管理する。

#### 一覧取得

基本形：

```java
Page<Work> findByUserAndStatus(
    User user,
    WorkStatus status,
    Pageable pageable
);
```

カテゴリー指定時：

```java
Page<Work> findByUserAndStatusAndCategory(
    User user,
    WorkStatus status,
    WorkCategory category,
    Pageable pageable
);
```

カテゴリー指定なしは「すべて」を意味する。

Serviceでカテゴリーの有無を判定し、使用するRepositoryメソッドを切り替える。

#### Pageable

並び順はRepositoryメソッド名に埋め込まず、`Pageable` で指定する。

一覧の戻り値は `Page<Work>` とする。

#### ログイン後トップの最新4件

トップ専用の `findTop4...` メソッドは作成しない。

一覧と同じ `findByUserAndStatus()` を再利用し、以下のPageableを渡す。

```text
page = 0
size = 4
```

---

### usersテーブル制約

#### id

```text
BIGINT NOT NULL AUTO_INCREMENT
PRIMARY KEY
```

#### email

```text
VARCHAR(255) NOT NULL
UNIQUE
```

UNIQUE制約名：

```text
uq_users_email
```

メール形式の判定はDB CHECKでは行わず、Bean Validation等のJava側で行う。

#### username

```text
VARCHAR(20) NOT NULL
UNIQUE
```

UNIQUE制約名：

```text
uq_users_username
```

4～20文字・半角英数字という入力仕様はJava側で検証する。

DBでは、最大20文字、NOT NULL、UNIQUEを保証する。

#### password_hash

```text
VARCHAR(255) NOT NULL
```

UNIQUE制約およびCHECK制約は設定しない。

平文パスワードを保存しないことはSpring Security側の責務とする。

#### created_at

```text
DATETIME NOT NULL
```

DB DEFAULTは使用せず、JavaのClockで設定する。

---

### worksテーブル制約

#### id

```text
BIGINT NOT NULL AUTO_INCREMENT
PRIMARY KEY
```

#### user_id

```text
BIGINT NOT NULL
FOREIGN KEY REFERENCES users(id)
```

外部キー制約名：

```text
fk_works_user
```

初期版にはユーザー退会機能がないため、`ON DELETE CASCADE` は設定しない。

現時点ではMySQL標準のRESTRICT相当の挙動とし、ユーザー削除時に関連作品を自動削除しない。

将来退会機能を実装する際に削除方針を再検討する。

#### name

```text
VARCHAR(50) NOT NULL
```

空文字・空白のみ等の入力チェックはJava側で行う。

#### photo_reference

```text
VARCHAR(500) NULL
```

画像未登録は `NULL` に統一する。

#### category

```text
VARCHAR(20) NOT NULL
```

CHECK制約名：

```text
chk_works_category
```

許可値：

```text
HAT
TOP
BOTTOM
SCARF
BAG
TOY
SMALL_GOODS
OTHER
```

#### status

```text
VARCHAR(20) NOT NULL
```

CHECK制約名：

```text
chk_works_status
```

許可値：

```text
IN_PROGRESS
COMPLETED
```

DB DEFAULTは設定しない。

#### stitch_count

```text
INT NOT NULL DEFAULT 0
```

CHECK制約名：

```text
chk_works_stitch_count
```

条件：

```text
0 <= stitch_count <= 9999
```

#### row_count

```text
INT NOT NULL DEFAULT 0
```

CHECK制約名：

```text
chk_works_row_count
```

条件：

```text
0 <= row_count <= 9999
```

#### memo

```text
VARCHAR(1000) NOT NULL DEFAULT ''
```

メモなしはNULLではなく空文字とする。

#### completion_date

```text
DATE NULL
```

statusとの整合性をCHECK制約で保証する。

CHECK制約名：

```text
chk_works_status_completion_date
```

条件：

```text
status = IN_PROGRESS
→ completion_date IS NULL

status = COMPLETED
→ completion_date IS NOT NULL
```

完成日が未来日でないことはDB CHECKではなく、JavaのServiceおよびClockを使用して検証する。

#### last_updated_at

```text
DATETIME NOT NULL
```

DBの自動更新は使用せず、Serviceで更新する。

---

### worksテーブルのインデックス

初期版では、以下の2本のみ作成する。

#### 制作中一覧・トップ用

```text
idx_works_user_status_updated
```

構成：

```text
(user_id, status, last_updated_at DESC, id DESC)
```

用途：

- 制作中作品一覧
- ログイン後トップの最新4件

並び順：

```text
last_updated_at DESC
id DESC
```

同一更新日時の場合はID降順を第2ソート条件とし、ページネーション時の並び順を安定させる。

#### 完成一覧用

```text
idx_works_user_status_completed
```

構成：

```text
(user_id, status, completion_date DESC, id DESC)
```

用途：

- 完成作品一覧

並び順：

```text
completion_date DESC
id DESC
```

完成日はDATE型であり、同日に複数作品が完成する可能性があるため、ID降順を第2ソート条件とする。

#### 初期版で作成しないインデックス

以下は初期版では作成しない。

```text
user_id単独
status単独
category単独
categoryを含む複合インデックス
```

理由：

- `user_id` は既存の複合インデックスの先頭に含まれる。
- statusは2値であり、単独では選択性が低い。
- category検索も必ずログインユーザー・statusと組み合わせる。
- 1ユーザーあたりの作品数は非常に大規模になる想定ではない。
- 不要なインデックスによるINSERT・UPDATEコスト増加を避ける。

将来実測で性能問題が確認された場合は、`EXPLAIN` 等で実行計画を確認した上でFlyway migrationにより追加する。

---

### Flywayによるスキーマ管理

#### 基本方針

DBスキーマの正式な構造・変更履歴はFlywayを正とする。

初期構造は以下のmigrationにまとめる。

```text
src/main/resources/db/migration/
└── V1__create_initial_schema.sql
```

V1には以下を含める。

- `users` テーブル作成
- `works` テーブル作成
- PRIMARY KEY
- FOREIGN KEY
- UNIQUE
- CHECK
- DEFAULT
- 初期インデックス
- 必要な文字コード・照合順序設定

#### 将来の変更

DB構造を変更する場合は、適用済みのV1を書き換えず、新しいmigrationを追加する。

例：

```text
V1__create_initial_schema.sql
V2__add_xxx_to_works.sql
V3__add_xxx_index.sql
```

FlywayがDBごとの適用履歴を管理する。

ユーザー登録や作品登録などの通常データ更新はFlywayの対象ではない。

Flywayが管理するのは、開発者によるDB構造・制約・インデックス等の変更である。

---

### Hibernate ddl-auto

HibernateにはDB構造を作成・変更させない。

設定は以下とする。

```properties
spring.jpa.hibernate.ddl-auto=validate
```

役割：

```text
Flyway
→ DB構造を作成・変更する

Hibernate
→ EntityとDB構造の整合性を起動時に確認する
```

`ddl-auto=update`、`create`、`create-drop` は使用しない。

---

### schema.sql

Spring Bootの `schema.sql` によるDBスキーマ作成は使用しない。

DBスキーマ管理方式をFlywayへ一本化する。

---

### JPA EntityとFlywayの責務分担

#### Entity側に記述するもの

JPA Entityには、JavaとDBの対応を理解するために必要なマッピング情報を記述する。

- `@Entity`
- `@Table(name = "...")`
- `@Column(name = "...", nullable = ..., length = ...)`
- Java型
- Entity間関連
- `@ManyToOne`
- `@JoinColumn`
- `@Enumerated(EnumType.STRING)`
- IDマッピング

#### Flyway側を正とするもの

以下の物理DB定義はFlywayを唯一の正とする。

- UNIQUE制約
- CHECK制約
- 外部キー制約名
- DEFAULT
- インデックス
- 文字コード
- 照合順序
- DB構造の変更履歴

#### 原則使用しないJPA定義

以下は原則使用しない。

```text
@Column(unique = true)
@Table(indexes = ...)
JPA側のCHECK制約生成定義
@Column(columnDefinition = ...)
```

理由：

HibernateにDDL生成をさせないため、DB物理定義をEntityとFlywayで二重管理しないためである。

#### NULL可否・文字列長

`nullable` および `length` はEntity側にも記載する。

例：

```java
@Column(
    name = "username",
    nullable = false,
    length = 20
)
private String username;
```

Flyway側にも同じ物理定義は存在するが、Entityの可読性およびDBとの対応を明確にするためJPA側にも記述する。

---

### DB/JPA設計の責務整理

| 内容 | 主な責任箇所 |
|---|---|
| Java型 | Entity |
| JavaとDBのマッピング | Entity / JPA |
| Entity間関連 | Entity / JPA |
| Enum型安全性 | Java Enum |
| Form入力検証 | Form / Bean Validation |
| 業務ルール | Service |
| 現在日時 | Service + Clock |
| DBの物理構造 | Flyway |
| UNIQUE | Flyway / MySQL |
| CHECK | Flyway / MySQL |
| FK | Flyway / MySQL |
| DEFAULT | Flyway / MySQL |
| インデックス | Flyway / MySQL |
| DB変更履歴 | Flyway |
| EntityとDBの整合確認 | Hibernate `validate` |

---

---

## 5. 認証・認可設計

### Spring Security全体構成

- 認証・認可にはSpring Securityを使用する。
- `SecurityConfig` を `config` 配下、認証固有クラスを `security` 配下へ配置する方針とする。
- `SecurityFilterChain` は初期版では **1本のみ** とする。
- `securityMatcher(...)` でChain自体を限定せず、アプリ全体へ適用する。
- Form Login、Remember-Me、HttpSession、CSRF、Logout、Security Headers、Exception Handling、URL認可を同一Chainで構成する。
- 複数Chain、`@Order` による優先順位管理は初期版では導入しない。
- `addFilterBefore` / `addFilterAfter` / `addFilterAt` による独自Filter追加・標準Filter順序変更は行わない。
- 将来 `/api/**` にBearer Token / stateless認証等を追加する場合のみ複数Chainを再検討する。


### URLアクセス制御

#### 公開URL

- `GET /`
- `GET /register`
- `POST /register`
- `GET /login`
- `POST /login`
- `GET /gauge-adjustment`
- `GET /yarn-combination`
- `GET /counter`
- `GET /yarn-guide`

CSS、JavaScript、ロゴ等の静的リソースもSecurityFilterChainを通し、その上で `permitAll` とする。`WebSecurityCustomizer` によるSecurity完全除外は使用しない。

#### 認証必須

`/works/**` は `authenticated()` とする。

対象例：

- `GET /works/new`
- `POST /works`
- `GET /works/in-progress`
- `GET /works/completed`
- `GET /works/{id}`
- `POST /works/{id}/basic-info`
- `POST /works/{id}/counter`
- `POST /works/{id}/memo`
- `POST /works/{id}/status`
- `POST /works/{id}/completion-date`
- `POST /works/{id}/delete`

`fullyAuthenticated()` は初期版では使用しない。Remember-Me認証でも通常ログインと同じ作品機能を利用可能とする。

#### その他

明示していないURLは `anyRequest().denyAll()` とする。ERROR/FORWARD dispatcher等の必要な扱いは共通エラー実装と整合させる。


### 未認証時の処理

#### 通常の保護画面

未認証で通常の認証必須画面へアクセスした場合：

`/login?required`

へ遷移する。

ログイン画面には「この機能を利用するにはログインしてください。」の趣旨を表示する。

元URLへ自動復帰しない。Saved Requestは使用せず、Request Cacheは `NullRequestCache` とする。

#### S-11非同期保存

以下は未認証時にHTTP 401を返す。

- `POST /works/{id}/counter`
- `POST /works/{id}/memo`
- `POST /works/{id}/status`
- `POST /works/{id}/completion-date`

URL + HTTP Methodの `RequestMatcher` で判定する。`Accept` や `X-Requested-With` には依存しない。

401は基本的にstatusのみとし、独自JSON、ログインHTML、`WWW-Authenticate` は返さない。JavaScriptは401/403を保存失敗として扱い、自動ログイン・自動再送は行わない。


### Form Login

- Knitting独自のS-03ログイン画面を使用する。
- `GET /login` はMVC Controller + Thymeleaf。
- `POST /login` はSpring Securityが直接処理する。
- Spring Securityの自動生成ログイン画面は開発時も使用しない。
- HTTP Basicは使用しない。
- ログインIDはusername。
- パラメータ名は標準の `username` / `password` / `remember-me`。

#### username

ログイン時usernameは前後空白のみ除去した後、4〜20文字・半角英数字を確認する。

形式不正時はDB検索せず、通常の認証失敗として扱う。内部空白は変更しない。検索は大文字小文字を区別せず `findByUsernameIgnoreCase(...)` 相当とする。

Principalのusernameはログイン入力値ではなくDB保存値の表記を使う。

#### password

ログイン時passwordは一切加工しない。

- trimしない
- stripしない
- 大小文字変換しない
- Unicode normalizationしない
- Blocklist確認しない
- 登録時の8〜64文字形式検証を再実施しない

入力値をそのままSpring Securityへ渡す。

#### 成功・失敗

- 成功：`/`
- 失敗：`/login?error`

独自SuccessHandler / FailureHandlerは作らない。username不存在とpassword不一致は外部から区別できない共通エラーとする。

失敗時はusername/passwordを保持しない。Remember-Me checkboxもOFFへ戻す。

autocomplete：

- username：`username`
- password：`current-password`

HTML `required` と最小限のJavaScriptはUX補助として使用してよい。

ログイン済みで `GET /login` へアクセスした場合は `/` へ遷移する。


### UserDetailsService / Principal

#### UserDetailsService

独自 `KnittingUserDetailsService` を使用し、`users` テーブルから認証ユーザーを取得する。

Spring Boot標準のin-memory `user`、自動生成password、`InMemoryUserDetailsManager`、`spring.security.user.*` は使用しない。

usernameが見つからない場合、内部では `UsernameNotFoundException` を投げ、`null` を返さない。

`hideUserNotFoundExceptions` はSpring Security標準動作を維持する。

#### Principal

`KnittingUserPrincipal implements UserDetails` とする。

初期版で保持する値：

- userId
- username
- 認証処理中のみ必要なpasswordHash

保持しないもの：

- JPA User Entityそのもの
- email
- createdAt
- その他プロフィール情報

PrincipalはEntityを直接保持せず、必要なscalar値だけをコピーする。

Authoritiesは空のimmutable collectionとし、`ROLE_USER` は付与しない。

enabled / accountNonLocked / accountNonExpired / credentialsNonExpired は初期版ではtrueとし、対応DBカラムは追加しない。

Principalは `CredentialsContainer` を実装して認証成功後にpasswordHashを消去する。

Serializableとし `serialVersionUID = 1L`。独自 `equals` / `hashCode` は実装しない。


### AuthenticationProvider / AuthenticationManager

- `DaoAuthenticationProvider` Beanを明示定義しない。
- `KnittingUserDetailsService` と `PasswordEncoder` を提供し、Spring Security標準構成へ任せる。
- 独自AuthenticationProviderは作らない。
- アプリケーションコード用 `AuthenticationManager` Beanは公開しない。
- Controller / Serviceから `authenticate(...)` を直接呼ばない。


### PasswordEncoder / password_hash

- `PasswordEncoderFactories.createDelegatingPasswordEncoder()` を使用する。
- 初期encoderはbcrypt、strengthは標準10。
- `PasswordEncoder.encode()` の戻り値を `{bcrypt}` 等のid prefixを含めてそのままDBへ保存する。
- `users.password_hash` は `VARCHAR(255) NOT NULL` を維持する。
- prefixを除去したり独自形式へ変換しない。
- `{id}` 欠損・未知id時にbcryptと推測してfallbackしない。
- 異常hashは認証成功させず、hash値や内部詳細をログへ出さない。
- 初期版ではlogin時hash自動upgrade、`UserDetailsPasswordService` は導入しない。
- 将来encoder変更時に `upgradeEncoding()` 等を再検討する。


### 登録時入力仕様

#### username

- 前後空白除去
- 4〜20文字
- 半角英数字
- DBには入力時の大文字小文字を維持
- 重複判定はIgnoreCase

#### email

1. 前後空白除去
2. lowercase
3. 形式検証
4. 重複検証
5. DB保存

内部空白は無効。

#### password

- 8〜64文字
- printable ASCII
- ASCII spaceは許可
- 制御文字・非ASCIIは初期版では不許可
- 大文字・小文字・数字・記号の組み合わせ必須ルールは設けない
- trim / normalizeしない
- confirmationとは完全一致比較

#### 登録失敗時

保持：

- email
- username

クリア：

- password
- password確認

autocomplete：

- email：`email`
- username：`username`
- password：`new-password`
- confirmation：`new-password`

username/email重複は具体的な項目エラーとして表示してよい。

非同期の利用可否確認APIは作らない。

同時登録でDB UNIQUE制約違反となっても500にせず、通常の項目エラーへ変換する。具体的例外処理は実装時に決定する。


### Password Blocklist

- 登録時にローカルBlocklistで弱い/common passwordを拒否する。
- 外部APIは使用しない。
- 使用データセットはSecListsの `Passwords/Common-Credentials/10k-most-common.txt` とする。
- 使用するファイルは10,001件とし、「10k」という名称に合わせて10,000件へ加工しない。
- ライセンスはMIT Licenseとする。
- SecListsの公式ライセンス全文をプロジェクトルートの `SECLISTS_LICENSE.txt` に配置する。
- Blocklistファイルは `src/main/resources/password-blocklist/10k-most-common.txt` に配置する。
- ファイル文字コードはUTF-8とする。
- データ形式は1行1passwordの改行区切りとする。
- 空行が存在する場合は読み飛ばしてよい。
- 各行をtrim / stripしない。
- コメント構文は設けない。
- 照合時のみ大文字小文字を区別しない。
- password本体はcase-sensitiveのまま。
- Unicode normalization / leetspeak変換 / substring判定は行わない。
- password全体一致のみ拒否する。
- 初期版では登録時の新しいpassword設定にのみ適用する。
- login時には適用しない。
- Blocklist更新で既存ユーザーを無効化しない。
- 起動時に1回読み込み、メモリへ保持する。
- requestごとのfile readやhot reloadは行わない。
- Blocklistファイルが欠損または読込不能の場合はfail closedとし、アプリケーションの起動を失敗させる。

エラー文言の趣旨：

> このパスワードは推測されやすいため使用できません。別のパスワードを設定してください。

情報源、順位、漏えい有無等は表示しない。


### ユーザー登録と認証

- ログイン済みで `GET /register` へアクセスした場合は `/`。
- ログイン済みで `POST /register` した場合も登録処理を行わず `/`。
- ただしCSRF不正は403を優先する。
- 登録成功後は自動ログインしない。
- 登録成功後：`/login?registered`
- 登録成功をAuthentication成功として扱わない。
- 登録成功だけを理由に匿名Sessionをinvalidateしない。
- 実際のlogin成功時にSession Fixation対策を適用する。


### Remember-Me

- token-based Remember-Meを使用する。
- Persistent Token Repository方式は使用しない。
- Remember-Me用DBテーブルは追加しない。
- checkbox名は `remember-me`。
- 明示的ON時のみ有効。
- `alwaysRemember` は使用しない。
- checkbox初期値は常にOFF。
- 有効期間は30日 = 2,592,000秒。
- 使用するたびに期限を延長しない。

Cookie：

- name：`remember-me`
- Path：標準/context path
- Domain：未設定(host-only)
- HttpOnly：true
- SameSite：Lax
- Secure：production HTTPSでtrue

key：

- 設定名：`knitting.security.remember-me-key`
- Javaソースへ直書きしない
- 未設定/blankなら起動失敗
- default/random-per-startupは使わない
- cryptographically secure random
- 最低32bytes / 256bit程度

keyは同一環境の通常再起動・再デプロイ・version updateでは維持する。定期rotationは行わず、漏えい/疑い時に交換する。環境ごとにkeyを分離する。

署名algorithmはSpring Security標準SHA-256を独自変更しない。

期限切れ・改ざんcookieは未認証として扱い、専用エラーは出さない。

通常loginとRemember-Meで同じ `KnittingUserPrincipal` を使う。Remember-Me成功後はSecurityContextをHttpSessionへ保存し `changeSessionId` を適用する。

Remember-Me OFF loginで、既存cookieを独自処理で強制削除しない。明示削除は `POST /logout`。

将来password_hash変更時に古いtoken-based Remember-Me cookieが無効になることを許容する。


### HttpSession / Session Fixation

- 通常認証状態はHttpSessionへ保持する。
- Spring Session / Redis / DB Sessionは使用しない。
- `SessionCreationPolicy.IF_REQUIRED`
- Session timeoutは明示的に30分。
- 最後のSession関連HTTP activityから30分の非アクティブ時間とする。
- heartbeatは送信しない。
- timeout警告UIは初期版では設けない。

未ログインでもCSRF等で必要な場合はSession作成を許可する。特に `GET /login` / `GET /register` でCSRF Tokenが必要なら作成されてよい。

Sessionあり = 認証済み、ではない。

JSESSIONID：

- Cookie追跡
- Domain未設定
- Path=context path
- 非永続cookie
- HttpOnly
- SameSite=Lax
- production HTTPSでSecure
- URL rewriting `;jsessionid=` は使用しない

Sessionは永続化しない。再起動/再デプロイで無効化されることを許容する。

Session Fixation対策は `changeSessionId`。

- Session IDのみ変更
- 一般の既存Session属性は維持
- `newSession` へ変更しない
- `migrateSession` へ変更しない
- 独自属性コピー/削除は行わない

複数端末/複数Sessionでの同時loginは許可し、concurrent session limitは設けない。


### Session期限切れとRemember-Me

Session timeout後：

#### 有効なRemember-Meあり
- Remember-Meで自動認証
- 新しいSession作成
- `/login?required` には遷移しない

#### Remember-Meなし/無効
- 通常保護画面は `/login?required`

Remember-Me期限は自動延長しない。


### SecurityContext

- Spring Security標準のSecurityContextRepositoryを使用する。
- Delegating構成（RequestAttribute + HttpSession）を維持する。
- 独自Repository、session-only置換は行わない。
- SecurityContextHolderは標準 `MODE_THREADLOCAL`。
- inheritable/globalへ変更しない。
- 将来async処理が必要な場合は個別設計する。


### Logout

- 実処理は `POST /logout`。
- Spring Security標準LogoutFilterで処理する。
- GET `/logout` でlogoutしない。
- Spring Security標準GET logout confirmation pageも使用しない。
- Knitting UIで確認後、通常HTML FormからPOSTする。
- fetch/AJAX logoutは使用しない。
- CSRF保護する。
- 独自Logout Controllerは作らない。

Logout時：

- SecurityContext clear
- current Session invalidate
- Remember-Me cookie削除

`deleteCookies("JSESSIONID")` を重複追加しない。`Clear-Site-Data` は初期版では送らない。localStorage簡易counter等も消さない。

Logout対象は現在の端末/ブラウザのみ。他端末Session/Remember-Meは維持し、「全端末logout」は作らない。

成功後は `logoutSuccessUrl("/")`。独自 `LogoutSuccessHandler`、専用画面、専用Controllerは作らない。


### CSRF

- CSRFは有効。
- 状態変更POSTを除外しない。
- `POST /login`、`POST /register`、`POST /logout`、作品更新・削除、S-11非同期POSTも対象。
- repositoryは標準 `HttpSessionCsrfTokenRepository`。
- CSRF cookie方式は使用しない。
- `/csrf` APIは作らない。
- 通常FormはThymeleaf/Spring Security標準連携を使用。
- S-11 JavaScriptはHTMLへ公開されたCSRF token/header名を取得し、そのままheaderへ設定する。
- JavaScript独自加工は行わない。

BREACH対策はSpring Security標準を維持し、`XorCsrfTokenRequestAttributeHandler` 相当の標準動作を使う。BREACH対策をopt-outしない。

Deferred CSRF Tokenも標準動作を維持する。

Login/Logout時のCSRF Token無効化・再生成もSpring Security標準へ任せる。

Session切れ後、古い/不正CSRF Tokenを持つ通常POSTは403とし、`/login?required` へ変換しない。

Login/Register POSTでCSRF無効・欠損ならRate Limit countより前に403とし、その試行はRate Limitへ加算しない。


### 作品所有者認可

URLレベルでは `/works/**` を `authenticated()` とする。

本人所有作品のみ閲覧・更新・削除できることはService/Repositoryで保証する。

Controllerでは `@AuthenticationPrincipal KnittingUserPrincipal` から認証ユーザーを取得し、ServiceへuserIdを渡す。

ServiceはSecurityContextへ直接依存しない。

Repository/Serviceは概念上 `findByIdAndUser(...)` 等、作品ID + owner条件で取得する。一覧もuserIdで絞る。

requestからowner userIdを受け取らない。

存在しない作品と他人所有作品は外部から区別できないowner-scoped lookupとし、いずれも第8章の方針に従い404として扱う。

Method Securityは初期版では使用しない。

- `@EnableMethodSecurity` なし
- `@PreAuthorize` なし
- `@PostAuthorize` なし

将来role追加や複数Service entry point等が必要になった場合のみ再検討する。


### Thymeleaf認証状態表示

認証状態による画面表示切替は、MVC側から共通Model属性として渡し、`th:if` / `th:unless` を使う方針とする。

Thymeleaf Security Extrasは初期版では必須としない。

JavaScriptで認証状態を判定しない。

画面表示制御は認可そのものではなく、サーバー側認可を必ず別途実施する。

共通Model属性は`isAuthenticated`とし、`CommonModelAttributeAdvice`から提供する。詳細は第12章を参照する。


### Anonymous Authentication

Spring Security標準anonymous authenticationを維持する。匿名認証を無効化しない。


### Security Headers / HTTPS / CORS

#### Security Headers

Spring Security標準Security Headersを使用する。

- X-Frame-Options：DENY
- HSTS：production HTTPS環境で有効
- 標準headerを安易に無効化しない

#### CSP

公開前にContent-Security-Policyを必須とする。ただし最終directiveはfront-end実装確定後に決める。必要ならReport-Onlyから開始してよい。

#### HTTPS

production公開ではHTTPS必須。

現時点でSpring Bootへhardcodedなforce-HTTPSを入れず、TLS終端/HTTP→HTTPS redirectはPlatform/Reverse Proxyを第一候補とする。

Forwarded Headers / trusted proxyは公開先決定後に確定する。

#### CORS

初期版はsame-originのSpring MVC + Thymeleaf構成とし、独自CORS設定は追加しない。


### Login Rate Limit

公開前に `POST /login` Rate Limitを必須とする。開発環境では省略可。

第一選択はPlatform / WAF / Reverse Proxy。必要な場合のみSpring Boot側で補完する。

独立bucket：

- username：5回 / 5分
- IP：20回 / 1分

username bucket keyは前後空白除去 + case-insensitive正規化。username存在確認はしない。形式不正usernameはshared invalid-username bucketへ入れる。

第一候補algorithmはSliding Window。難しい場合Token Bucket。Fixed Windowは第一選択にしない。

CSRF正常なmanual `POST /login` は成功・失敗ともcountする。成功でもresetしない。

429拒否後のrequestはcountせず、windowも延長しない。

Remember-Me自動認証はRate Limitへcountしない。

超過時：

- HTTP 429
- 「ログイン試行が多すぎます。しばらく時間をおいてから再度お試しください。」の趣旨
- bucket種類、残数、正確な解除時刻は表示しない
- `Retry-After` は初期版では送らない


### Registration Rate Limit

公開前に `POST /register` Rate Limitを必須とする。GETは対象外。

IP bucketのみ：

- 5回 / 1時間

algorithm：

- Sliding Window

CSRF正常POSTはvalidation失敗、duplicate、successを含めてcountする。成功でもresetしない。

429拒否はcountせず、windowを延長しない。

Login IP bucketとは分離する。

超過時はHTTP 429。`Retry-After` は初期版では送らない。


### Client IP

Client IPは信頼できるProxy設定を前提に取得する。

`X-Forwarded-For` を無条件に信用しない。

- IPv4：個別IP
- IPv6：原則/64
- unknown IP：共通bucket

unknownだからRate Limitを無効化しない。unknown大量発生時は環境設定不備として扱う。

trusted proxy / forwarded header設定は公開環境決定後に確定する。


### 初期版で導入しない認証関連機能

- CAPTCHA
- email verification
- password reset
- login後password change
- account deletion
- 永続failed-login count
- account lock DB管理
- 管理者role
- `ROLE_USER`
- all-device logout
- UserDetails cache
- authentication audit DB
- HTTP Basic
- Bearer Token
- JWT
- stateless API auth
- Persistent Remember-Me token table
- Spring Session / Redis Session / DB Session
- custom CORS
- custom timing sleep/delay


### Timing / Cache / Logging

#### Timing

username不存在時の独自sleep/delayは追加しない。Spring Security標準DaoAuthenticationProviderのtiming mitigation + Rate Limitへ任せる。

#### Browser Cache / Back

Logout後Back対策はSpring Security標準Cache-Control/no-store等を利用する。各Controllerでheaderを重複実装しない。History操作禁止JSは入れない。

#### Logging

productionは原則INFO。Spring Security DEBUG/TRACEを常時ONにしない。

ログへ出さない：

- password
- password hash
- Remember-Me token
- Remember-Me key
- Session ID
- CSRF token
- 不要なemail等の個人情報
- request param/headerの無差別unmask

初期版では独自認証監査DB、username/IP login履歴を作らない。


### 401 / 403整理

#### 通常保護画面 未認証
`/login?required`

#### S-11非同期POST 未認証
HTTP 401

#### CSRF不正
HTTP 403

#### 認証済みだがSpring Security認可拒否
HTTP 403

403を `/login?required` へ変換しない。

403はSpring Securityの`AccessDeniedHandler`で処理し、通常HTMLではHTTP 403を維持して共通403画面を表示する。S-11非同期通信ではHTTP 403・bodyなしとする。詳細は第8章を参照する。

---

## 6. 画像保存設計

### 詳細設計05「画像保存設計」確定事項

#### 作品画像本体の保存方式

- 作品画像本体は **MySQLへ保存しない**。
- MySQLには画像への参照情報のみを保持する。
- BLOBは使用しない。
- 画像未登録・削除後は `photo_reference = NULL` とする。
- 開発環境ではローカルファイル保存を使用する。
- 初期実装をS3等のクラウドストレージへ固定しない。

---

#### 開発環境と本番環境の保存方式

- 開発環境はローカルファイル保存とする。
- 本番環境の具体的な物理保存方式は、公開・ホスティング環境決定後に確定する。
- 本番では以下のいずれも候補となり得る。
  - 永続ローカルストレージ
  - クラウドオブジェクトストレージ
- 保存方式が変わってもDBスキーマや `photo_reference` の論理形式を変更しない設計とする。
- 開発環境をS3等へ依存させない。

---

#### DBへ保持する画像情報

`works.photo_reference` のみを保持する。

```text
VARCHAR(500) NULL
```

- 画像本体はDBへ保存しない。
- OS絶対パスは保存しない。
- storage-rootは保存しない。
- 公開URL・ドメインは保存しない。
- 元ファイル名は保存しない。
- 画像メタデータ用の追加カラム・テーブルは初期版では設けない。
- 画像なしは空文字ではなく `NULL` とする。

---

#### 保存ファイル名

保存ファイル名はアプリケーション側で生成する。

```text
<UUID>.<extension>
```

例：

```text
550e8400-e29b-41d4-a716-446655440000.jpg
```

- Java標準形式のUUID文字列表現を使用する。
- UUIDのハイフンは保持する。
- 元ファイル名を保存ファイル名として使用しない。
- 画像差し替え時も新しいUUIDを生成する。
- workId、userId、作品名、日時等を保存ファイル名へ含める必要はない。

---

#### 画像保存ディレクトリ構成

画像保存ルートは設定値として外部化する。

```text
<image-storage-root>/works/<UUID>.<ext>
```

- `works/` は固定の論理ディレクトリとする。
- 初期版では `works/` 直下へ画像を保存する。
- ユーザー別・作品別・ハッシュ分割等のサブディレクトリは初期版では作らない。
- `src/main/resources/static` 配下へユーザーアップロード画像を保存しない。
- build出力ディレクトリやJAR内部へ保存しない。

---

#### `photo_reference` の具体形式

DBへ保存する値は以下とする。

```text
works/<UUID>.jpg
works/<UUID>.png
```

ルール：

- storage-rootは含めない。
- 先頭 `/` は付けない。
- 区切り文字は `/` に統一する。
- Windows環境でもDBへ `\` を保存しない。
- 絶対パスは保存しない。
- 公開URLは保存しない。
- 実ファイル操作時は、設定されたstorage-rootと `photo_reference` を組み合わせて解決する。
- 画像なしは `NULL`。

---

#### 対応画像形式

初期版で対応する画像形式は以下のみ。

- JPEG
- PNG

入力時：

- JPEG：`.jpg` / `.jpeg`
- PNG：`.png`

保存時：

- JPEG：`.jpg`
- PNG：`.png`

初期版で非対応：

- GIF
- WebP
- HEIC
- HEIF
- SVG
- その他画像形式

拡張子だけでは画像形式を判定しない。

---

#### MIME Type・拡張子・実データ検証

サーバー側で以下を確認する。

1. 拡張子
2. MIME Type
3. 実際の画像データ形式
4. JPEGまたはPNGとして正常に読み込めること
5. 拡張子・MIME・実データ形式が一致していること

例：

JPEGの場合：

```text
拡張子：.jpg または .jpeg
MIME：image/jpeg
実データ：JPEG
```

PNGの場合：

```text
拡張子：.png
MIME：image/png
実データ：PNG
```

不一致・デコード不能の場合は保存しない。

---

#### 画像容量上限

アップロード元画像の最大容量：

```text
10 MiB
= 10 × 1024 × 1024
= 10,485,760 bytes
```

- リサイズ・圧縮前の元画像に適用する。
- 作品登録・画像差し替えの両方に適用する。
- 10,485,760 bytes以下は許可。
- 10,485,761 bytes以上は拒否。
- UI上は「10MB以下」と案内する。
- Multipart全体設定は画像10MiB＋multipart overheadを許容する値とし、具体値は実装時に決定する。
- 保存後画像のファイル容量上限は設けない。

---

#### 画像縦横サイズ上限

元画像の幅・高さは以下を上限とする。

```text
幅 <= 8192px
高さ <= 8192px
```

- 縦長・横長とも同じ条件。
- 実画像データ上のピクセル寸法を確認する。
- どちらか一方でも8192pxを超えた場合は拒否する。
- 最小寸法は初期版では設けない。

---

#### 保存画像のリサイズ

保存時の最大長辺：

```text
1920px
```

- 元画像の長辺が1920pxを超える場合のみ縮小する。
- アスペクト比を維持する。
- 長辺1920px以下の画像は拡大しない。
- 正方形へ強制しない。
- 元画像そのものを物理的にトリミングしない。
- 原本＋サムネイル等の複数画像は初期版では保存しない。
- 保存するのは処理後画像1枚のみ。

例：

```text
4000×3000
→ 1920×1440

3000×4000
→ 1440×1920

1200×900
→ 1200×900
```

---

#### 画像圧縮

JPEG：

```text
quality = 0.85
```

- 長辺1920px以下でもJPEGは再エンコードする。
- 保存形式はJPEGのまま。
- 拡張子は `.jpg`。

PNG：

- PNGのまま保存する。
- 非可逆JPEG変換は行わない。
- ロスレスで再エンコードする。
- PNGの具体的圧縮レベルはライブラリ／実装依存とする。

---

#### EXIF・画像メタデータ

EXIF Orientationが存在する場合：

- Orientationを画像ピクセルへ反映する。
- 必要に応じて回転・反転する。
- 保存後画像はOrientationメタデータへ依存しない状態とする。

保存後画像には不要なメタデータを意図的に引き継がない。

対象例：

- GPS
- 撮影日時
- カメラ情報
- 端末情報
- IPTC
- XMP
- 不要なEXIF

ICC Profile等の色管理については、表示を不必要に壊さないことを優先し、詳細は実装時に確認する。

Optional metadataが欠損・壊れていても、画像本体が安全に処理可能なら、それだけを理由に入力エラーにはしない。

---

#### 元ファイル名の扱い

元ファイル名はアップロード処理中の一時情報としてのみ扱う。

用途例：

- 拡張子確認

保存しないもの：

- DB
- 保存ファイル名
- sidecar
- 別テーブル
- 通常ログ

ユーザーの元ファイル名をパス生成へ使用しない。

---

#### 画像保存のタイミング

##### S-08 作品登録

ファイル選択時：

- ブラウザ側でプレビューのみ。
- 正式保存しない。

保存ボタン押下後：

- Form／画像バリデーション成功後に画像保存処理へ進む。

入力エラー時：

- 新画像は正式保存しない。

##### S-11 作品詳細

画像選択・削除指定時：

- プレビューだけ変更。
- 現在の正式画像・DBは変更しない。

保存ボタン押下後：

- 正式処理を実行する。

キャンセル：

- 現在のDB・正式画像は一切変更しない。

---

#### 画像差し替え時の旧画像

- 新画像の処理が成功するまで旧画像を保持する。
- 新画像は新しいUUIDで保存する。
- 旧ファイルへ上書きしない。
- 新画像保存・DB更新が失敗した場合は旧画像・旧 `photo_reference` を維持する。
- DB更新成功後に旧画像を削除対象とする。
- 画像履歴は初期版では保持しない。

---

#### 画像を単独削除した場合

S-11で「写真削除」を選択しただけでは削除を確定しない。

削除指定時：

- 初期画像をプレビュー表示する。
- `deletePhoto = true` の保留状態とする。

キャンセル：

- 現在の `photo_reference` を維持。
- 実画像も維持。

保存成功：

```text
photo_reference = NULL
```

- 旧アップロード画像を削除対象とする。
- 共通初期画像を表示する。
- 共通初期画像の参照先をDBへ保存しない。
- 写真削除は `last_updated_at` 更新対象。

---

#### 作品自体を削除した場合

作品削除成功時：

- `works` レコードを削除。
- 当該作品のアップロード画像がある場合は画像ファイルも削除する。

`photo_reference = NULL` の場合：

- 画像削除処理は行わない。

共通初期画像：

- 削除対象にしない。

削除キャンセル：

- DB・画像ファイルとも変更しない。

---

#### DBと画像ファイルの整合性・トランザクション方針

SpringのDBトランザクションはDBのみを対象とし、ファイルシステムは自動rollbackされない。

基本方針：

- **DBが存在しない画像を参照する状態を避ける。**
- そのため、必要なら一時的な孤児ファイルが残る方を優先する。
- 新規ファイルには補償削除を行う。

##### 新規作品＋画像

```text
画像検証・処理
↓
新画像の正式ファイル保存
↓
DB INSERT + photo_reference
↓
DB commit
```

DB失敗：

- 新画像を補償削除する。

##### 画像差し替え

```text
旧画像保持
↓
新画像保存
↓
DB photo_reference更新
↓
commit
↓
旧画像削除
```

DB失敗：

- 新画像を補償削除。
- 旧画像・旧参照を維持。

##### 画像単独削除

```text
DB photo_reference = NULL
↓
commit
↓
旧画像削除
```

##### 作品削除

```text
旧photo_referenceを保持
↓
DB DELETE
↓
commit
↓
旧画像削除
```

commit後の旧画像削除失敗：

- 正しいDB状態をrollbackしない。
- 孤児ファイルとして残る可能性を許容する。
- WARN／ERRORログ対象。

分散トランザクションは使用しない。

---

#### 孤児画像ファイルの検出・清掃

初期版：

- 自動スケジュール清掃は行わない。
- 通常は補償削除で孤児発生を抑える。

画像削除失敗：

- WARN／ERRORログを出す。

手動保守時：

- `<storage-root>/works/` 内の正式画像
- DBの非NULL `photo_reference`

を比較して孤児候補を確認する。

対象形式：

```text
works/<UUID>.jpg
works/<UUID>.png
```

手動清掃は画像更新が発生しない保守時間帯を基本とする。

---

#### 作品画像のアクセス方法・URL

作品画像は保存ディレクトリを直接Web公開しない。

画像取得URL：

```text
GET /works/{id}/image
```

- ログイン必須。
- ログインユーザー本人の作品のみ取得可能。
- ブラウザURLにはworkIdを使用する。
- `photo_reference`、保存ファイル名、物理パスをURLへ直接出さない。
- サーバー側で所有者確認済みWorkから `photoReference` を取得し、storage-rootと組み合わせて画像を返す。

`photo_reference == NULL` の場合：

- 通常画面では共通静的画像 `/images/default-work.png` を使用する。

S-08の保存前プレビュー：

- ブラウザローカルプレビュー。

---

#### DB参照はあるが実ファイルが存在しない場合

`photo_reference != NULL` なのに実ファイルがない／読めない場合：

- DBとファイルの不整合として扱う。
- broken imageにはしない。
- 共通初期画像をフォールバック表示する。
- WARN／ERRORログを出す。
- GET処理でDBを自動的に `NULL` へ変更しない。
- GET処理で `last_updated_at` を更新しない。
- 一時的なストレージ障害による論理データ消失を避ける。
- 画像だけの失敗で作品詳細画面全体をシステムエラーにしない。

---

#### HTTPキャッシュ

アップロード済み作品画像：

```text
Cache-Control: private, no-store
```

- URLが固定でも画像差し替えを即時反映することを優先。
- 長期ブラウザキャッシュを利用しない。
- 初期版ではETag／Last-Modified／304は必須としない。
- CDN等は本番環境決定後に再検討する。

共通初期画像やCSS／JS等の静的リソースは別扱い。

---

#### パストラバーサル対策

ファイル操作前に `photo_reference` を検証する。

許可形式：

```text
works/<UUID>.jpg
works/<UUID>.png
```

拒否例：

- `..`
- 絶対パス
- 先頭 `/`
- `\`
- URL
- 想定外拡張子
- 想定外ディレクトリ階層

処理：

1. `photo_reference` 形式を検証。
2. storage-rootと結合。
3. absolute/normalize。
4. `works` 管理領域内に収まることを確認。
5. 範囲外ならファイル操作を行わない。

元ファイル名を読み書き削除パス生成へ使用しない。

初期版では管理領域内にsymlinkを作らない。

---

#### 追加セキュリティ対策

初期版では外部アンチウイルス・マルウェアスキャナを必須としない。

保存対象：

- 元アップロードbytesではなく、
- 検証・デコード・補正・リサイズ・再エンコード後の出力画像。

0-byteファイルは拒否する。

返却Content-Type：

```text
JPEG -> image/jpeg
PNG  -> image/png
```

保存領域でアップロードファイルを実行可能にしない。

画像処理ライブラリは保守・更新する。

Rate Limit、WAF等は公開環境・規模に応じて再検討する。

---

#### storage-root設定

論理設定キー：

```text
knitting.image.storage-root
```

この値は画像保存ルート親ディレクトリを表す。

最終保存先：

```text
<knitting.image.storage-root>/works/<UUID>.<ext>
```

- `works/` は設定値に含めない。
- rootは環境ごとに変更可能。
- root変更でJavaロジック・DB参照形式を変更しない。
- 具体的なproperties／yml／環境変数への設定方法は実装・環境設定時に決定する。
- 開発中のアップロード画像はGit管理対象にしない。

---

#### 保存ディレクトリの起動時確認

ローカルファイル保存環境では、起動時にstorage-rootを確認する。

- root未設定・空・不正 → 勝手なfallbackを使わず起動失敗。
- rootディレクトリがない → 作成。
- `works/` がない → 作成。
- ディレクトリであるべき場所に通常ファイルがある → 削除・上書きせず起動失敗。
- 必要なread/writeができない → 起動失敗。

後述のstale `.tmp` 清掃は起動時処理として許可する。

将来オブジェクトストレージを使用する場合は、そのサービス向けの可用性確認方式へ変更する。

---

#### 画像保存失敗時のユーザー向け扱い

##### 入力エラー

例：

- 0-byte
- 10MiB超過
- 非対応形式
- extension／MIME／実データ不一致
- デコード不能
- 8192px超過

扱い：

- 正式画像保存なし。
- DB成功扱いにしない。
- S-08／S-11を入力エラーとして再表示。

##### システムエラー

例：

- ファイル書き込み失敗
- I/Oエラー
- 画像処理ライブラリの予期しない例外
- ストレージ停止

S-08：

- 画像保存システムエラー時は作品自体を写真なしで勝手に登録しない。
- 作品登録全体を失敗とする。

S-11：

- 作品名・写真・カテゴリーは1保存単位。
- 画像差し替えシステムエラー時は基本情報の一部だけ保存しない。
- 旧画像・旧基本情報を維持する。

---

#### 共通初期画像

共通初期画像：

```text
src/main/resources/static/images/default-work.png
```

ブラウザURL：

```text
/images/default-work.png
```

ファイル形式：

```text
PNG
```

DB：

- `photo_reference = NULL` で「アップロード画像なし」を表す。
- default画像パスはDBへ保存しない。

使用箇所：

- S-08初期プレビュー
- S-11写真なし
- S-11削除保留プレビュー
- 実画像欠損時fallback

default画像は、アップロード画像のUUID・リサイズ・圧縮・削除・孤児清掃の対象外。

---

#### 画像表示時の縦横比

保存画像：

- 元のアスペクト比を維持する。
- 保存時に正方形等へトリミングしない。

S-01／S-09／S-10作品カード：

```text
画像領域 4:3
cover相当
中央基準で表示上のみトリミング
```

S-08プレビュー：

```text
contain相当
画像全体を表示
```

S-11詳細・編集プレビュー：

```text
contain相当
画像全体を表示
```

---

#### Form項目名・削除指示

新規・差し替え画像：

```text
photo
```

削除指示：

```text
deletePhoto
```

S-11状態：

1. `photo`なし / `deletePhoto=false` → 現在画像を維持
2. `photo`あり / `deletePhoto=false` → 差し替え
3. `photo`なし / `deletePhoto=true` → 削除
4. `photo`あり / `deletePhoto=true` → 矛盾入力として拒否

UI側でも両方同時指定を防ぐ。

`photo_reference` はhiddenでブラウザから送信しない。

owner userId、保存パス、元ファイル名も送信させない。

---

#### 入力エラー時のプレビュー・再選択

サーバー入力エラー後：

- 新しく選択されたファイルは保持しない。
- ユーザーは再選択する。

S-08：

- previewは共通初期画像へ戻す。
- 通常フォーム値は可能な範囲で保持。

S-11差し替え失敗：

- 新ファイルは破棄。
- 現在DBに保存済み画像を表示。
- DBがNULLなら初期画像。

S-11削除保留＋別項目エラー：

- `deletePhoto=true` は通常Form値として保持可能。
- 初期画像プレビューを維持。
- 実画像・DBは未変更。

セッションやDBへ一時画像を保存しない。

---

#### 責務分担

##### Controller

担当：

- HTTP
- Form binding
- 認証ユーザー取得
- validation result
- view / redirect

直接ファイルI/Oを行わない。

##### Form

担当：

- name
- category
- photo
- deletePhoto
- 入力検証用値

EntityをそのままFormにしない。

##### Work Service

担当：

- 所有者確認
- 業務ロジック
- DB transaction
- 画像保存／DB更新／補償処理の順序制御

##### 画像処理・ストレージ担当コンポーネント

担当：

- 画像形式検証
- MIME／実データ確認
- Orientation
- resize
- re-encode
- UUID
- photo_reference生成
- safe path
- save / read / delete

Repositoryを直接更新しない。

##### Repository

担当：

- DB / JPAのみ。

##### Entity

`Work` は以下のみを保持する。

```text
photoReference: String
```

MultipartFile、byte[]、BufferedImage等を持たせない。

---

#### 画像保存結果とServiceへの受け渡し

画像保存成功時、画像コンポーネントはDBへ保存可能な論理参照を返す。

```text
works/<UUID>.jpg
works/<UUID>.png
```

返さないもの：

- absolute path
- storage-root
- public URL

Serviceは返却された論理参照を `Work.photoReference` へ設定する。

画像保存失敗を `null` で表さない。

`null` は正常な「画像なし」の意味に限定する。

---

#### 画像削除結果と不存在ファイル

有効な参照＋ファイル存在＋削除成功：

- 成功。

有効な参照＋ファイル既に不存在：

- desired state達成済みとして成功相当。
- 必要に応じて不整合WARN。

`photo_reference == NULL`：

- Serviceは削除処理を呼ばない。

default画像：

- 削除対象外。

不正ref：

- ファイル操作しない。
- エラー扱い＋ログ。

permission／I/O等で削除不能：

- 実削除失敗。

---

#### 画像取得HTTPレスポンス

作品画像取得：

```text
GET /works/{id}/image
```

JPEG：

```text
Content-Type: image/jpeg
Content-Disposition: inline
Cache-Control: private, no-store
```

PNG：

```text
Content-Type: image/png
Content-Disposition: inline
Cache-Control: private, no-store
```

fallback default画像：

```text
Content-Type: image/png
```

元ファイル名をContent-Dispositionへ含めない。

UUIDファイル名や `photo_reference` も外部へ公開しない。

---

#### 画像取得用セキュリティヘッダー

作品画像レスポンス：

```text
X-Content-Type-Options: nosniff
Cross-Origin-Resource-Policy: same-origin
```

- 外部公開用CORSは設定しない。
- `Access-Control-Allow-Origin: *` は使用しない。
- 既存のX-Frame-OptionsやCSP等は共通Security設計側で管理する。

将来CDN等で別origin配信する場合は再検討する。

---

#### 二重送信対策

S-08作品登録、S-11基本情報保存は以下の二重対策を使用する。

##### クライアント側

送信開始後：

- 保存ボタンをdisable。
- 必要に応じて「保存中」表示。

##### サーバー側

各フォーム表示ごとに：

- 1回限りのsubmission identifier / tokenを発行。

同じtokenによる2回目のリクエスト：

- 画像UUID生成
- 画像保存
- DB更新
- 旧画像削除

を再実行しない。

このtokenはCSRF Tokenとは別用途。

DBへ `submission_token` カラムは追加しない。

具体的な一時保存方式は実装時に決定する。

---

#### 複数タブ・複数端末の同時編集競合

`works.version` 等の新しいDBカラムは追加しない。

S-11基本情報フォーム表示時に、submission tokenと紐づけて以下のsnapshotを保持する。

- workId
- name
- category
- photoReference

POST時：

1. 認証・owner確認。
2. snapshotと現在DBのname/category/photoReferenceを比較。
3. 変更済みなら競合として処理中止。
4. 競合なしなら必要に応じて新画像処理。
5. 最終DB更新直前にWork行へ短時間のupdate/write lock。
6. 最新DBとsnapshotを再比較。
7. 未変更ならname/category/photoReferenceのみ更新してcommit。
8. 処理中に競合が発生した場合はDB更新せず、作成済み候補画像を補償削除。

lockは画像処理中ずっと保持しない。

競合判定対象：

- name
- category
- photoReference

判定対象外：

- stitch_count
- row_count
- memo
- status
- completion_date
- last_updated_at

そのため、カウンター等の変更だけでは基本情報競合にしない。

---

#### 競合時の画面再表示

競合発生時：

- 今回の基本情報変更は保存しない。
- 自動mergeしない。
- stale requestの入力内容を再適用しない。
- 作成済み候補画像があれば補償削除。
- 現在の正しい画像は削除しない。
- DBの最新状態を取得してS-11通常表示へ戻す。

案内文の趣旨：

```text
別の画面で作品情報が更新されたため、
今回の変更は保存されませんでした。
最新の内容を確認して、もう一度編集してください。
```

再編集開始時：

- 新しいsubmission token
- 新しいsnapshot

を発行する。

---

#### 画像処理中の一時データ

元アップロード画像用にKnitting独自の一時保存領域は作らない。

- Multipart受信の一時処理はSpring Boot／Servlet containerへ任せる。
- 画像デコード・加工データは必要に応じてメモリで扱う。
- 不要な全体byte[]コピーを避ける。
- `<storage-root>/temp` は作らない。
- `works/` に元アップロードbytesを保存しない。
- 入力エラー・競合時の画像を次リクエスト用に保持しない。

---

#### 完成画像の一時ファイルと正式切替

最終画像へ直接書き込まない。

一時ファイル：

```text
works/.<UUID>.tmp
```

手順：

1. 処理済み最終画像を `.tmp` へ完全書き込み。
2. close。
3. `works/<UUID>.jpg|png` へmove/rename。
4. atomic move可能なら優先。
5. atomic move不可でも同一ディレクトリ・同一filesystem内で完了済みtmpをmoveする。
6. final切替成功後のみ `photo_reference` をServiceへ返す。

UUID正式ファイルが既に存在した場合：

- 上書きしない。
- 保存失敗とする。

tmp write／move失敗：

- tmpをbest-effort削除。
- 削除失敗時はtmp残留を許容。
- DB参照は付けない。

---

#### 異常終了で残った `.tmp` 清掃

通常は、

- 正式move
- 即時補償削除

でtmpを残さない。

異常終了で残ったtmpは起動時に清掃する。

対象：

```text
works/.<UUID>.tmp
```

条件：

```text
最終更新から24時間以上
```

- 24時間未満のtmpは削除しない。
- 正式 `.jpg` / `.png` は触らない。
- DB参照との比較は不要。
- 自動スケジューラは初期版では設けない。

起動順：

1. storage-root / works確認・作成
2. read/write確認
3. 24時間以上のstale tmp清掃
4. 通常起動

個別tmp削除失敗：

- WARN／ERROR
- 起動継続

storage全体が使用不能：

- 起動失敗

---

#### 画像処理の順序

画像保存処理は以下の順で行う。

1. 0-byte確認
2. 元画像容量 <= 10MiB
3. 元ファイル名拡張子確認
4. client MIME確認
5. 実画像形式・ピクセル寸法確認
6. extension／MIME／実形式一致確認
7. width / height <= 8192
8. 画像デコード
9. EXIF Orientation反映
10. 長辺 > 1920 の場合resize
11. UUID生成・tmp/final準備
12. JPEG quality 0.85 / PNG losslessでtmpへ再エンコード
13. tmp close完了
14. tmp -> 正式UUIDファイルへmove
15. final切替成功後のみ `works/<UUID>.<ext>` をServiceへ返す

失敗した時点で後続処理へ進まない。

可能なら全ピクセルdecode前にformat・dimensionsを確認し、大きな不正入力の処理コストを抑える。

---

#### 画像処理ライブラリ

採用：

##### Java標準 `ImageIO` / `ImageReader`

用途：

- 実画像形式
- width / height
- reader availability
- 事前検証

##### Thumbnailator

用途：

- EXIF Orientation反映
- アスペクト比維持resize
- JPEG quality 0.85
- JPEG / PNG再エンコード

Maven依存：

```text
net.coobird:thumbnailator:0.4.21
```

バージョンは固定する。

##### Java NIO

用途：

- UUID
- safe path
- tmp
- move / rename
- delete

初期版では追加しない：

- metadata-extractor
- TwelveMonkeys ImageIO

ただし実利用上の互換性問題や対応形式拡張が発生した場合は再検討する。

保存後画像へ不要なメタデータが残っていないことは実画像fixtureでテストする。

---

#### 画像処理失敗の分類

原因基準で分類する。

##### 入力エラー

「正常に動作しているKnittingでも、その画像データ自体が不正・仕様外なら失敗するもの」

例：

- 0-byte
- 10MiB超過
- 非対応extension
- 非対応MIME
- 実形式がJPEG/PNGでない
- extension／MIME／実形式不一致
- 8192px超過
- 認識不能画像
- 壊れた／途中で切れたJPEG・PNG

JPEG/PNGとして判別できてもデコード不能な破損画像は入力エラー。

##### システムエラー

「本来許可される正常画像でも、アプリ・ライブラリ・ストレージ・環境要因で失敗するもの」

例：

- resize予期せぬ例外
- re-encode予期せぬ例外
- writer unavailable
- tmp create/write/close/move失敗
- permission
- disk full
- storage unavailable
- ライブラリ・実装互換性問題
- メモリ／リソース不足
- UUID保存衝突等の内部異常

競合・二重送信拒否・認証認可・CSRFは別分類。

---

#### 画像入力エラーメッセージ

##### 空／読み込み不能

```text
画像ファイルを正常に読み込めません。別の画像を選択してください。
```

##### 10MB超過

```text
画像は10MB以下のファイルを選択してください。
```

##### 非対応形式・形式不一致

```text
JPEGまたはPNG形式の画像を選択してください。
```

##### 8192px超過

```text
画像の幅と高さは8192px以下にしてください。
```

##### 壊れた画像・decode failure

```text
画像ファイルを正常に読み込めません。
別のJPEGまたはPNG画像を選択してください。
```

- 技術用語（MIME mismatch等）はユーザーへ出さない。
- 1画像で複数エラーがあっても、処理順で最初に検出した1件を表示する。
- 画像入力エラーは写真入力欄付近へ表示する。
- システムエラーは画像入力エラーとして表示しない。

---

#### ログ出力方針

共通原則：

- Spring Boot loggerを使用。
- `System.out.println` を運用ログに使用しない。
- 本番は原則INFO。
- DEBUGは常時有効にしない。

##### 正常系

通常の画像保存・差し替え・削除成功は毎回INFOにしない。

必要に応じてDEBUG。

##### WARN

例：

- DB参照はあるが実ファイル欠損
- 不正 `photo_reference` を検出しファイル操作拒否
- stale tmp個別削除失敗
- 既に削除済みのはずのファイル不存在等の不整合

##### ERROR

例：

- 予期せぬ画像処理失敗
- re-encode失敗
- tmp create/write/close/move失敗
- permission／disk／storage I/O
- 補償削除失敗
- commit後旧画像削除失敗
- 起動時storage検証失敗
- 深刻なpath/integrity異常

ログへ出してよい例：

- operation type
- workId
- processing stage
- internal error category
- 必要な場合のみ検証済みlogical photo_reference

出してはいけないもの：

- 画像バイナリ
- Base64
- 元ファイル名
- GPS／EXIF等
- 作品名
- 不要なusername／email
- submission token
- CSRF token
- Session ID
- password／secret
- multipart全文
- request body全文

未検証の悪意あるpath文字列をそのままログへ出さない。

---

---

## 7. 自動保存・非同期更新設計

### 基本方針

#### 通信方式

S-11 の自動保存には標準 JavaScript の `fetch()` を使用する。

- HTTPメソッド：`POST`
- リクエスト形式：`application/x-www-form-urlencoded`
- レスポンス形式：`application/json`
- 画面遷移：行わない
- ページ全体再読み込み：行わない
- SPA / React 等：採用しない
- JavaScript：必要最小限とする

リクエストは `URLSearchParams` 相当を使用して Form URL Encoding 形式で送信する。

JSON形式のリクエストボディは採用しない。

HTTPステータスを成功・失敗判定の第一基準とし、JSON内のフラグだけで判定しない。

---

### 自動保存エンドポイント

既存のURL・ルーティング設計を維持し、次の4エンドポイントを使用する。

| 機能 | HTTP | エンドポイント |
|---|---|---|
| 作品専用カウンター | POST | `/works/{id}/counter` |
| 制作メモ | POST | `/works/{id}/memo` |
| 制作ステータス | POST | `/works/{id}/status` |
| 完成日 | POST | `/works/{id}/completion-date` |

目数と段数は1つの `/counter` エンドポイントを共有するが、更新単位は1項目ずつとする。

制作ステータスと完成日はHTTPエンドポイントを分けるが、JavaScript上では同一の「制作状態」保存チャンネルとして直列管理する。

---

### CSRF・Session

#### CSRF

Spring Security のCSRF保護を有効のまま使用する。

S-11 の全自動保存POSTもCSRF保護対象とする。

ThymeleafでS-11のHTMLへCSRF tokenとheader名を出力する。

概念例：

```html
<meta name="_csrf" th:content="${_csrf.token}">
<meta name="_csrf_header" th:content="${_csrf.headerName}">
```

JavaScriptはページ初期化時にこれらを取得する。

CSRFヘッダー名をJavaScriptへ固定文字列としてハードコードしない。

Spring SecurityがHTMLへ公開したheader名をそのまま使用する。

#### CSRF情報を独自保存しない

以下は行わない。

- CSRF token の `localStorage` 保存
- CSRF token の `sessionStorage` 保存
- 独自Cookieへの保存
- `/csrf` 等のCSRF取得専用API作成
- JavaScript側での独自トークン加工

#### Session Cookie

同一オリジンのSessionを利用する。

`fetch()`では概念的に次を指定する。

```javascript
credentials: "same-origin"
```

---

### 共通fetch処理

`work-detail.js` 内に自動保存用の共通POST関数を設ける。

責務は次に限定する。

- POST送信
- CSRFヘッダー設定
- Form URL Encoding
- Session Cookie送信
- `keepalive` 切替

概念例：

```javascript
async function postAutoSave(url, params, options = {}) {
    return fetch(url, {
        method: "POST",
        headers: {
            [csrfHeader]: csrfToken,
            "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
        },
        body: new URLSearchParams(params),
        credentials: "same-origin",
        keepalive: options.keepalive ?? false
    });
}
```

共通関数へ以下の業務判断を集中させない。

- カウンター固有処理
- メモ固有処理
- ステータス／完成日固有処理
- 409時のチャンネル停止
- 画面値の更新
- 保存状態表示

これらは各保存チャンネル側で判断する。

---

### 保存タイミング

#### 目数・段数

目数・段数は画面上の値を即時に変更し、最後の操作から **300ms** 後に保存する。

対象：

- ＋ボタン
- －ボタン
- 直接入力

直接入力は正常値になった時点で300ms debounceを開始する。

連続変更中はタイマーをリセットし、中間値を送信しない。

#### 制作メモ

制作メモは最後の入力から **1秒** 操作がない場合に保存する。

入力が続いている間はdebounceタイマーをリセットする。

#### 制作ステータス

制作ステータスは選択変更直後に保存を開始する。

#### 完成日

完成日は正常な日付変更が確定した時点で即時保存する。

未来日、形式不正、制作中状態等、送信条件を満たさない値は送信しない。

#### debounce中の最新値

debounce中に複数回変更された場合、未送信の中間値は破棄し、最新値だけを保存対象とする。

---

### 保存チャンネル

JavaScriptでは次の4保存チャンネルを独立管理する。

| チャンネル | 対象 |
|---|---|
| `stitch` | 目数 |
| `row` | 段数 |
| `memo` | 制作メモ |
| `workState` | 制作ステータス＋完成日 |

目数と段数は同じHTTPエンドポイントを使用するが、保存状態は別チャンネルとして扱う。

制作ステータスと完成日は互いに関連するため、`workState` 1チャンネルとして直列管理する。

---

### 同一チャンネル内の通信直列化

同一保存チャンネルでは、通常動作中に複数の保存リクエストを並列実行しない。

例：

```text
10 → 11 を送信中
その間に 12 → 13 → 14
```

この場合：

```text
送信1：11
送信2：14
```

のみとする。

12、13は送信しない。

通信中に新しい値が入力された場合は、最新値をpending値として保持する。

先行リクエスト完了後、保存済み値とpending値が異なる場合だけ次の保存を行う。

別チャンネル同士は互いにブロックしない。

例：

- メモ保存中でも目数は保存可能
- 目数保存中でも段数は保存可能
- `workState` 内のステータスと完成日は並列化しない

---

### 保存状態

各保存チャンネルは次の状態を持つ。

```text
IDLE
WAITING
SAVING
SAVED
ERROR
CONFLICT
```

| 状態 | 意味 |
|---|---|
| `IDLE` | 通信していない通常状態 |
| `WAITING` | debounce待機中 |
| `SAVING` | HTTP通信中 |
| `SAVED` | 最新値まで保存成功し、保存完了表示中 |
| `ERROR` | 5xxまたは通信失敗等 |
| `CONFLICT` | 409競合により当該チャンネル停止中 |

`IDLE`自体を「必ず未保存変更なし」とは定義しない。

未保存の有無は主に `savedValue` と `pendingValue` の一致、およびvalidation error等の別状態から判断する。

---

### 保存状態表示

保存状態はページ全体で1つの表示にまとめず、関連機能付近へ表示する。

表示文言：

```text
保存中...
保存しました
保存できませんでした
```

#### 保存中

実際のHTTPリクエストを開始した時点で `保存中...` を表示する。

debounce待機中だけでは `保存中...` を表示しない。

#### 保存完了

最新値までサーバー保存済みになった場合のみ `保存しました` と表示する。

古い値の保存が成功しても、より新しいpending値が残っている場合は保存完了扱いにしない。

`保存しました` は約2秒後に自動で非表示とする。

#### 保存失敗

5xxまたは通信失敗等では `保存できませんでした` を表示する。

失敗状態は解消するまで保持する。

カウンター、メモは独立表示とし、ステータス＋完成日は共通の制作状態保存表示とする。

色、アイコン、具体的なCSSは後続の画面詳細・実装時に確定する。

---

### JavaScript状態データ

各チャンネルでは、少なくとも次の概念的情報を保持する。

```javascript
{
    state,
    savedValue,
    pendingValue,
    sentValue,
    debounceTimerId,
    savedMessageTimerId
}
```

意味：

- `savedValue`：サーバー保存成功が確認された最新値
- `pendingValue`：現在画面上でユーザーが最後に入力した最新値
- `sentValue`：現在進行中のHTTPリクエストで実際に送信したsnapshot

`workState` はステータスと完成日の組み合わせを保持する。

概念例：

```javascript
{
    state,
    savedStatus,
    savedCompletionDate,
    pendingStatus,
    pendingCompletionDate,
    sentStatus,
    sentCompletionDate
}
```

多数の独立したグローバル変数へ分散させず、保存チャンネルごとの状態オブジェクトとして管理する。

---

### 初期値

S-11表示時、Controllerは本人所有のWorkから現在のDB保存値をThymeleafへ渡す。

対象：

- `stitchCount`
- `rowCount`
- `memo`
- `status`
- `completionDate`

Thymeleafはこれらを通常の入力要素へ描画する。

JavaScriptは初期化時に、DOMへ描画済みの値を最初のサーバー保存済み値として読み取る。

同じ値をhidden inputや大量の `data-*` 属性へ重複保持しない。

初期化順序：

```text
1. DOM要素取得
2. 描画済み値を状態オブジェクトへ読み込み
3. saved値 / pending値を初期化
4. state = IDLE
5. イベントリスナー登録
```

ページ表示直後に自動保存POSTを発生させない。

#### 数値

目数・段数はDOMから文字列として取得後、JavaScriptの数値へ変換して状態管理する。

#### ステータス

表示文字列ではなく次の内部値で管理する。

```text
IN_PROGRESS
COMPLETED
```

#### 完成日

DBの `completion_date = NULL` はJavaScriptでは `null` として扱う。

`<input type="date">` の空文字は初期化時に `null` へ正規化する。

#### workId

`workId` はS-11ルート要素等へ `data-work-id` として1回だけ渡す。

概念例：

```html
<div id="workDetail" th:data-work-id="${work.id}">
```

`userId`、ownerId等はHTMLへ渡さない。

---

### 制作中時の完成日要素

制作中状態でも完成日入力要素自体はDOMから削除しない。

制作中：

```text
DOMには存在
画面上は非表示
操作不可
値は空
JavaScript上は null
```

完成へ変更後、同じ入力要素を表示・有効化する。

---

### クライアント側バリデーション

JavaScript側バリデーションはユーザー操作性向上のための補助とし、正式なデータ検証は必ずサーバー側でも行う。

#### 目数・段数

条件：

- 全角数字は半角数字へ正規化する
- 0～9999の整数
- 小数不可
- 負数不可
- 数字以外不可
- 10000以上不可

直接入力中に一時的に空欄になった場合は入力途中として扱い、その瞬間にはPOSTしない。

フォーカスを外した時点でも無効なら入力エラーを表示する。

－ボタンは0で無効化する。

＋ボタンは9999で無効化する。

#### 制作メモ

条件：

- 任意
- 空文字可
- 最大1000文字

1000文字を超えている間はPOSTしない。

1000文字以内へ戻れば通常の1秒debounceへ復帰する。

「1000文字」は第9章で定めるUnicodeコードポイント数で判定する。

#### 制作ステータス

許可値：

```text
IN_PROGRESS
COMPLETED
```

定義外値はサーバー側でも拒否する。

#### 完成日

条件：

- 正しい日付形式
- ステータスが `COMPLETED`
- 当日以前
- 未来日不可

制作中作品に対する完成日手動更新はサーバー側でも拒否する。

---

### 入力エラー時の画面値

入力不正時、ユーザーが入力した画面値を自動的にDB保存済み値へ戻さない。

例：

```text
DB：500
画面：10000
```

この場合：

- DBは500のまま
- 画面には10000を保持
- 入力エラーを表示
- ユーザー修正後に通常の自動保存へ復帰

---

### 自動保存用Form

Entityを直接リクエストFormとして使用しない。

以下の4Formを使用する。

#### CounterUpdateForm

```text
counterType   : CounterType
value         : Integer
expectedValue : Integer
```

`CounterType`：

```text
STITCH
ROW
```

例：

```text
counterType=STITCH
value=11
expectedValue=10
```

`value` と `expectedValue` は必須、0～9999とする。

#### MemoUpdateForm

```text
memo         : String
expectedMemo : String
```

- `memo`：新しく保存したい値
- `expectedMemo`：JavaScriptが認識している直前保存済み値

未入力は `NULL` ではなく空文字 `""` とする。

#### StatusUpdateForm

```text
status                 : WorkStatus
expectedStatus         : WorkStatus
expectedCompletionDate : LocalDate
```

新しい完成日はクライアントから送信しない。

`IN_PROGRESS → COMPLETED` の初期完成日はサーバーが `Clock` から決定する。

#### CompletionDateUpdateForm

```text
completionDate         : LocalDate
expectedStatus         : WorkStatus
expectedCompletionDate : LocalDate
```

`expectedCompletionDate` は制作中状態では `null` を許可する。

#### Formに含めない情報

次はFormへ含めない。

```text
workId
userId
ownerId
```

- `workId`：URLのPathVariable
- `userId`：Spring Security Principal

から取得する。

---

### Bean ValidationとService検証の分担

#### Form / Bean Validation

単純な入力条件を担当する。

例：

- 必須
- 数値範囲
- 文字数
- Enum型
- 日付型

#### Service

DB状態・業務ルールを必要とする検証を担当する。

例：

- 本人所有作品か
- expected値とDB現在値が一致するか
- 制作中／完成の状態整合性
- 完成日が未来日でないか
- 状態遷移
- `last_updated_at` 更新可否

---

### Controller設計

S-11の自動保存専用Controllerとして以下を設ける。

```java
WorkAutoSaveController
```

配置：

```text
controller
```

#### 責務

- HTTPリクエスト受付
- PathVariable取得
- Principal取得
- Formバインド
- Bean Validation
- Service呼び出し
- JSONレスポンス生成

以下は担当しない。

- DBアクセス
- 所有者判定
- 競合判定
- 状態遷移
- `last_updated_at` 制御

#### Controllerメソッド

概念的に次の4メソッドを持つ。

```text
updateCounter()
updateMemo()
updateStatus()
updateCompletionDate()
```

Formは `application/x-www-form-urlencoded` を受けるため、`@ModelAttribute` 相当でバインドする。

概念例：

```java
@PostMapping("/works/{id}/counter")
public ResponseEntity<?> updateCounter(
        @PathVariable Long id,
        @AuthenticationPrincipal KnittingUserPrincipal principal,
        @Valid @ModelAttribute CounterUpdateForm form,
        BindingResult bindingResult)
```

#### BindingResult

Bean Validationエラーがある場合はServiceを呼ばずHTTP 400を返す。

Enum、数値、日付等の型変換エラーも入力不正としてHTTP 400へ統一する。

#### 例外処理

Controllerごとに同種の `try-catch` を大量に記述しない。

- 入力エラー：Controller/Formレベル
- 業務例外：Serviceから送出
- 共通HTTP変換：第8章の共通例外・エラー処理設計
- 401/403：Spring Security

---

### Service設計

S-11自動保存専用Serviceとして以下を設ける。

```java
WorkAutoSaveService
```

配置：

```text
service
```

#### publicメソッド

```text
updateCounter(...)
updateMemo(...)
updateStatus(...)
updateCompletionDate(...)
```

各public更新メソッドを1回の自動保存処理のトランザクション境界とする。

#### Formを直接渡さない

ControllerはFormから必要な型付き値を取り出し、Serviceへ渡す。

概念例：

```java
updateCounter(
    Long userId,
    Long workId,
    CounterType counterType,
    int value,
    int expectedValue
)
```

```java
updateMemo(
    Long userId,
    Long workId,
    String memo,
    String expectedMemo
)
```

```java
updateStatus(
    Long userId,
    Long workId,
    WorkStatus status,
    WorkStatus expectedStatus,
    LocalDate expectedCompletionDate
)
```

```java
updateCompletionDate(
    Long userId,
    Long workId,
    LocalDate completionDate,
    WorkStatus expectedStatus,
    LocalDate expectedCompletionDate
)
```

#### Service処理順

基本処理順：

```text
本人所有Work取得
↓
行ロック
↓
最新DB状態確認
↓
expected値競合判定
↓
業務ルール検証
↓
実変更有無確認
↓
Entity更新
↓
必要時 last_updated_at 更新
↓
トランザクションcommit
```

#### 戻り値

必要な場合だけ小さなResult DTOを使用する。

##### CounterSaveResult

```text
counterType
value
```

##### Memo

全文をレスポンスへ返す必要がないためService戻り値は `void` でよい。

##### StatusSaveResult

```text
status
completionDate
```

##### CompletionDateSaveResult

```text
completionDate
```

Entity自体をControllerレスポンス用として返さない。

---

### Repository設計

既存の `WorkRepository` を使用する。

通常取得用のowner条件付き取得に加え、自動保存更新用にPESSIMISTIC_WRITEを使用するowner条件付き取得を設ける。

概念例：

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Work> findByIdAndUserId(Long id, Long userId);
```

検索条件へ最初から作品IDとログインユーザーIDを含める。

他人所有Workを先に取得し、その後Java側だけでowner判定する構成にはしない。

owner条件付き取得が空の場合、

- 作品不存在
- 他人所有作品

を自動保存処理内では区別しない。

作品不存在と他人所有作品は外部から区別せず、いずれも第8章の方針に従い404として扱う。

---

### PESSIMISTIC_WRITE

更新用取得には、

```java
LockModeType.PESSIMISTIC_WRITE
```

を使用する。

ロック後の最新DB状態に対してexpected値を比較する。

目的は、2リクエストが同じ古い値を見て両方更新成功してしまう競合を防止することである。

ロック保持範囲：

```text
Work取得
↓
expected比較
↓
更新
↓
commit
```

の短時間に限定する。

画像処理等の長時間処理は自動保存トランザクション内には存在しない。

managed Entityの変更はJPA dirty checkingを基本とし、自動保存ごとに `Repository.save()` を必須とはしない。

---

### トランザクション

以下の各Serviceメソッドを独立したトランザクションとする。

```text
updateCounter
updateMemo
updateStatus
updateCompletionDate
```

Controllerではトランザクションを管理しない。

特にステータス変更では、

```text
status
completion_date
last_updated_at
```

を必要に応じて同一トランザクションで更新し、中間状態を作らない。

---

### last_updated_at

`last_updated_at` はDBトリガーや全更新共通のJPAコールバックで自動更新せず、Serviceの業務処理から明示的に制御する。

日時取得はServiceへDIした `Clock` を使用する。

```java
LocalDateTime.now(clock)
LocalDate.now(clock)
```

業務ロジック内で `LocalDateTime.now()` / `LocalDate.now()` を直接使用しない。

#### 更新対象

実際に値が変わった場合、次で更新する。

- 目数
- 段数
- 制作メモ
- 制作ステータス

S-11以外の既存仕様では以下も更新対象：

- 作品名
- 写真変更
- 写真削除
- カテゴリー

#### 更新しない場合

- 作品詳細画面を閲覧しただけ
- 完成日の手動変更だけ
- 同じ値の再送

#### ステータス変更時

ステータス変更に伴い完成日も変化した場合は、完成日の手動変更ではなく「ステータス変更」として扱うため `last_updated_at` を更新する。

---

### 同値再送

次の場合：

```text
expected値 = DB現在値
new値      = DB現在値
```

は正常終了とする。

ただし実変更がないため、

```text
last_updated_at
```

は更新しない。

通信再試行等により同じ値が送られても、一覧順等へ不要な影響を与えない。

---

### 複数タブ・複数端末の競合制御

`works.version` 等の新しいDBカラムは追加しない。

各自動保存リクエストは、新しい値とともに、クライアントが認識している直前保存済み値をexpected値として送信する。

例：

```text
expectedValue = 10
value         = 11
```

Serviceは行ロック取得後の最新DB値とexpected値を比較する。

一致：

```text
更新実行
```

不一致：

```text
更新しない
AutoSaveConflictException
HTTP 409
```

#### 項目単位で競合判定

作品全体ではなく、原則として更新対象項目単位で競合判定する。

例：

```text
タブA：目数変更
タブB：メモ変更
```

は互いに競合させない。

```text
タブA：目数10 → 11
タブB：目数10 → 12
```

は競合とする。

---

### 制作ステータスと完成日の競合

制作ステータスと完成日は関連項目のため、競合判定では次の組み合わせを使用する。

```text
expectedStatus
expectedCompletionDate
```

ステータス更新、完成日更新のどちらでも両方を比較する。

これにより、別タブですでに制作中へ戻された作品へ、古い画面から完成日だけを書き戻すことを防止する。

---

### 制作ステータスと完成日の連動

#### IN_PROGRESS → COMPLETED

ステータス変更時にServiceが現在日を完成日の初期値として設定する。

```text
status = COMPLETED
completion_date = LocalDate.now(clock)
last_updated_at = 現在日時
```

同一トランザクションで処理する。

#### COMPLETED → IN_PROGRESS

```text
status = IN_PROGRESS
completion_date = NULL
last_updated_at = 現在日時
```

とする。

以前の完成日は現在の完成日として保持しない。

#### 再度COMPLETED

再完成日を新しい完成日の初期値とする。

#### 完成日の手動変更

サーバー保存済みステータスが `COMPLETED` の場合だけ許可する。

手動完成日変更だけでは `last_updated_at` を更新しない。

---

### workState保存チャンネル

JavaScriptではステータスと完成日を `workState` 1チャンネルとして管理する。

ステータス保存中は完成日入力を一時的に操作不可とする。

ステータス保存成功後、サーバーから返された、

```text
status
completionDate
```

をサーバー保存済み状態として反映する。

完成日の手動変更は、ステータス保存成功後かつ保存済み状態が `COMPLETED` の場合のみ可能とする。

---

### 保存成功レスポンス

成功はHTTP 200を第一基準とする。

基本JSON：

```json
{
  "saved": true
}
```

必要な場合のみ `data` を追加する。

#### カウンター

```json
{
  "saved": true,
  "data": {
    "counterType": "STITCH",
    "value": 11
  }
}
```

#### 制作メモ

本文全体を毎回レスポンスへ返さない。

```json
{
  "saved": true
}
```

#### ステータス

```json
{
  "saved": true,
  "data": {
    "status": "COMPLETED",
    "completionDate": "2026-09-10"
  }
}
```

#### 完成日

必要に応じてサーバー確定日を返す。

```json
{
  "saved": true,
  "data": {
    "completionDate": "2026-09-08"
  }
}
```

巨大・冗長なレスポンスは作らない。

---

### 保存成功後のsaved / pending / sent管理

リクエスト送信時の値を `sentValue` としてsnapshot保持する。

例：

```text
savedValue   = 10
sentValue    = 11
pendingValue = 14
```

11の保存成功後：

```text
savedValue   = 11
pendingValue = 14
```

とする。

画面の最新値14を11へ戻さない。

その後14を次の保存対象とする。

最終的に：

```text
savedValue   = 14
pendingValue = 14
```

になった時点で `SAVED` とする。

#### 画面値を書き換えてよい条件

原則として、

```text
pendingValue == sentValue
```

の場合のみ、サーバー確定値を必要に応じて画面へ同期する。

通信中にユーザーがさらに変更した場合、古い成功レスポンスで最新入力を上書きしない。

#### workState

ステータス成功レスポンスでサーバーが新しい完成日を返した場合、

```text
savedStatus
savedCompletionDate
```

は必ずサーバー結果へ更新する。

ただし通信中にユーザーがさらに状態変更していた場合、画面の最新pending状態を古いレスポンスで上書きしない。

次回送信のexpected値には、直前に成功したサーバー保存済み状態を使用する。

---

### エラー分類

| 結果 | JavaScript側の基本処理 |
|---|---|
| 200 | 保存成功 |
| 400 | 入力不正 |
| 401 | 認証状態喪失 |
| 403 | CSRF・認可等 |
| 409 | 同時編集競合 |
| 5xx | サーバーエラー |
| fetch自体失敗 | ネットワーク／通信失敗 |

HTTPステータスを先に確認し、必要な場合のみJSONを解析する。

401、403、5xx等でJSON bodyの存在を前提にしない。

---

### 400 Validation Error

入力不正はHTTP 400とする。

概念例：

```json
{
  "code": "VALIDATION_ERROR",
  "fieldErrors": {
    "value": "0～9999の整数で入力してください。"
  }
}
```

処理：

- DB更新なし
- `last_updated_at` 更新なし
- `savedValue` 変更なし
- `pendingValue` 維持
- 項目エラー表示
- `保存しました` 表示なし
- 通常の再試行ボタンなし

入力修正後、クライアント側検証を通過すれば通常の自動保存へ復帰する。

クライアント側validation errorは通信stateとは別に管理する。

---

### 401 Unauthorized

未認証時はHTTP 401とする。

処理：

- 全自動保存チャンネル停止
- 自動ログインしない
- 自動再送しない
- 通常の再試行を行わない
- 画面上のpending値はそのページが残る間は勝手に消さない
- 再ログインを案内する

ユーザー向け文言例：

```text
ログイン状態が切れたため保存できませんでした。再度ログインしてください。
```

---

### 403 Forbidden

CSRF不正等ではHTTP 403とする。

処理：

- 全自動保存チャンネル停止
- 自動再送しない
- 通常の再試行を行わない
- ページ再読み込みを案内する

ユーザー向け文言例：

```text
保存処理を続けられません。ページを再読み込みしてください。
```

401/403の具体的な共通レスポンス生成は認証・共通エラー設計に従う。

---

### 409 Conflict

expected値と最新DB値が不一致の場合は自動保存競合とする。

Serviceは、

```java
AutoSaveConflictException
```

相当のRuntimeException系業務例外を送出する。

外部仕様：

```text
HTTP 409 Conflict
Content-Type: application/json
```

概念JSON：

```json
{
  "code": "CONFLICT",
  "message": "他の画面で内容が更新されています。ページを再読み込みして最新の内容を確認してください。"
}
```

現在のDB値そのものは409レスポンスへ返さない。

競合時：

- DB更新しない
- `last_updated_at` 更新しない
- 自動mergeしない
- stale値を再適用しない
- 自動再試行しない
- 通常の再試行ボタンを表示しない
- 当該保存チャンネルを `CONFLICT` にする
- ページ再読み込みを解決手段とする

メモ競合ならメモのみ停止する。

ステータスまたは完成日競合なら `workState` 全体を停止する。

他の独立チャンネルは継続可能とする。

具体的な `ControllerAdvice` 等の例外ハンドラー構成は共通例外・エラー処理設計へ引き継ぐ。

---

### 5xx・ネットワーク失敗

#### 5xx

5xxでは当該保存チャンネルを `ERROR` とする。

```text
保存できませんでした
[再試行]
```

を表示する。

#### fetch失敗

HTTPレスポンスを受け取れなかった通信失敗も当該保存チャンネルを `ERROR` とする。

5xxとネットワーク失敗は内部的・テスト上は別ケースとして扱う。

#### 再試行

自動再試行は行わない。

ユーザーが手動で再試行した場合は、失敗した古いpayloadではなく、その時点の最新 `pendingValue` を送信する。

---

### 自動再試行の禁止

以下すべてで自動再試行は行わない。

- 400
- 401
- 403
- 409
- 5xx
- ネットワーク失敗

ただし、400は入力修正後に通常の自動保存へ戻る。

5xx・通信失敗のみ、ユーザー操作による手動再試行を提供する。

---

### 画面離脱時の保存

自動保存項目のみを理由とした未保存離脱警告は表示しない。

作品名・写真・カテゴリーの明示的編集に対する未保存離脱警告は別機能として維持し、本書の自動保存処理とは分離する。

#### イベント

主な契機：

```javascript
document.addEventListener("visibilitychange", ...)
```

`document.visibilityState === "hidden"` になった場合、未送信値をフラッシュ対象とする。

補助：

```javascript
window.addEventListener("pagehide", ...)
```

#### beforeunload

自動保存フラッシュ目的では `beforeunload` を使用しない。

自動保存項目のために保存完了までページ離脱を阻止しない。

#### debounce中

debounce待機中にページがhidden等になった場合：

```text
debounce timer解除
↓
待機時間を無視
↓
現在の有効なpending値を即時送信対象
```

とする。

#### keepalive

離脱時は通常の共通fetch処理を使用し、

```javascript
keepalive: true
```

へ切り替える。

通常保存は `keepalive: false` とする。

#### sendBeacon

初期版では `navigator.sendBeacon()` を使用しない。

理由：

- 通常fetch方式と統一する
- CSRFカスタムヘッダー方式を維持する
- 送信処理を一本化する

#### 二重送信防止

`visibilitychange` と `pagehide` は同じ `flushPendingAutoSaves()` 相当処理を呼ぶ。

各チャンネルのstate等を確認し、同じ値を二重送信しない。

#### 通信中

同一チャンネルがすでに `SAVING` の場合、離脱時でも別の並列リクエストを即時起動しない。

先行リクエスト完了後、JavaScript実行が継続できる場合のみ最新pending値のkeepalive送信を試みる。

#### 送信しない状態

以下の場合、離脱時にも強制送信しない。

- validation error
- `CONFLICT`
- 401による全停止
- 403による全停止

#### 保証レベル

離脱時保存はbest-effortとする。

以下のような場合まで100%保存を保証しない。

- ブラウザープロセス強制終了
- OSクラッシュ
- 端末電源断
- 通信断
- ブラウザーがイベントを発火しない終了

初期版では自動保存項目を `localStorage` 等へ二重保存する復旧機構は追加しない。

---

### JavaScriptファイル構成

S-11専用JavaScriptは初期版では1ファイルへまとめる。

例：

```text
src/main/resources/static/js/work-detail.js
```

機能ごとに多数のJSファイルへ細分化しない。

ただしファイル内部では責務ごとに関数を分ける。

例：

- 初期化
- カウンター処理
- メモ処理
- 制作状態処理
- CSRF取得
- 共通POST
- 保存状態表示
- エラー処理
- pending flush

---

---

## 8. 共通例外・エラー処理設計

### 基本方針

エラーは次の3種類に分類し、同一の扱いにしない。

1. 入力エラー
2. 業務エラー
3. システムエラー

基本原則：

- ユーザーが修正可能な入力エラーは、可能な限り元の入力画面内へ表示する。
- 業務上の不存在・競合等は、意味に合ったHTTPステータスまたは専用UXへ変換する。
- 予期しないシステム障害は500系とし、内部情報をユーザーへ露出しない。
- Controllerごとに同一のtry-catchを大量に記述しない。
- ServiceはHTML / JSON等のレスポンス形式を知らない。
- Service例外にHTTPステータスや画面遷移の責務を持たせない。
- `Exception#getMessage()` をそのままユーザーへ表示しない。
- Spring Boot標準のエラー処理を完全に置き換えず、最終フォールバックとして利用する。

---

### HTMLと非同期レスポンスの分離

通常HTMLリクエストとS-11非同期リクエストは、
エラー応答方式を明確に分離する。

#### 通常HTML

入力エラー：

- 原則として同じ入力画面を再表示する。
- 項目付近へエラーメッセージを表示する。
- 通常FormのBean ValidationエラーはHTTP 200で再表示してよい。

その他：

- 400 / 403 / 404 / 500は必要に応じて共通HTMLエラー画面を使用する。
- HTTPエラーを302リダイレクトで隠さない。

#### S-11非同期通信

対象：

- `POST /works/{id}/counter`
- `POST /works/{id}/memo`
- `POST /works/{id}/status`
- `POST /works/{id}/completion-date`

方針：

- ページ遷移しない。
- HTTPステータスを第一契約とする。
- JSON本文は必要なケースだけ返す。
- 401 / 403 / 404 / 5xxではJSON本文を必須としない。
- HTMLエラーページへリダイレクトしない。
- Acceptや`X-Requested-With`等の動的ヘッダーではなく、
  事前に定義したController / endpointの役割によってHTML / JSONを分ける。

---

### HTTPステータス体系

#### 最終一覧

| 状況 | HTTP / 遷移 | 外部処理 |
|---|---|---|
| 通常HTML Form入力不正 | 200 | 同じ入力画面＋項目エラー |
| malformed request / PathVariable型不正等 | 400 | 共通400画面 |
| S-11入力不正 | 400 | Validation JSON |
| 通常保護画面・未認証 | 302 | `/login?required` |
| S-11非同期・未認証 | 401 | bodyなし可 |
| CSRF不正 / Security認可拒否 | 403 | HTMLまたはstatusのみ |
| 作品不存在 | 404 | 共通404 |
| 他人所有作品 | 404 | 作品不存在と区別しない |
| S-11自動保存競合 | 409 | `{"code":"CONFLICT"}` |
| 基本情報編集競合 | 303 | `GET /works/{id}`へPRG |
| Login/Register Rate Limit超過 | 429 | 元画面を429のまま再表示 |
| 想定外システム障害 | 500 | 共通500 |
| S-11サーバー障害 | 5xx | 当該チャンネルERROR |

---

### 400 Bad Request

HTTP 400は、リクエスト入力自体がアプリの受入条件を満たさない場合に使用する。

例：

- 必須値不足
- 数値範囲外
- 数値変換エラー
- memo文字数超過
- 不正Enum
- 不正日付
- Bean Validation違反
- Spring MVC Binding / 型変換エラー
- malformed PathVariable

例：

```text
/works/abc
```

でworkIdがLong必須の場合は400。

一方、

```text
/works/999999
```

のように形式は正常だが作品が存在しない場合は404。

#### 通常HTML Form

ユーザー修正可能なValidationエラーは共通400画面へ送らない。

```text
POST
↓
BindingResultにエラー
↓
Serviceを呼ばない
↓
同じ入力画面をHTTP 200で再表示
```

#### S-11

BindingResultに入力エラーがある場合：

- Serviceを呼ばない。
- DBを更新しない。
- `last_updated_at`を更新しない。
- HTTP 400。
- Validation JSONを返す。

---

### 401 Unauthorized

401は、S-11非同期保存時に認証が存在しない、
または認証状態が失われた場合に限定して使用する。

通常保護画面の未認証アクセスでは401画面を表示せず、

```text
/login?required
```

へ遷移する。

#### S-11の401

レスポンス：

- HTTP 401
- body不要
- JSON不要
- `WWW-Authenticate`不要
- HTMLログイン画面を返さない
- redirectしない

JavaScript：

- 全自動保存チャンネル停止
- 自動ログインしない
- 自動再送しない
- 通常の手動再試行もしない
- pending値はページが残る間保持
- 再ログインを案内

401はControllerAdviceではなく、
Spring Securityの`AuthenticationEntryPoint`側で処理する。

---

### 403 Forbidden

403は主に次の場合に使用する。

- CSRF Token欠損 / 不正
- Spring Securityによるアクセス拒否
- `anyRequest().denyAll()`による拒否
- `GET /logout`直接アクセス

#### 通常HTML

- HTTP 403を維持する。
- 共通403画面を表示する。
- トップへ302 redirectして隠さない。

#### S-11

- HTTP 403
- body不要
- JSON不要
- 全自動保存チャンネル停止
- 自動再送しない
- 通常再試行しない
- ページ再読み込みを案内

---

### 404 Not Found

404は、形式上正常なリクエストで対象業務リソースが利用できない場合に使用する。

例：

- 本人所有作品が存在しない
- workIdは存在するが他ユーザー所有
- 削除済み作品を参照

通常HTML：

- HTTP 404
- 共通404画面

S-11：

- HTTP 404
- JSON本文不要
- 自動保存停止
- 自動再送しない
- 通常再試行しない
- 再読み込みまたは一覧へ戻ることを案内

共通メッセージ：

```text
お探しのページまたは作品が見つかりません。
```

---

### 他人所有作品の扱い

他ユーザー所有作品へのアクセスは403ではなく404とする。

目的：

- 作品の存在有無を外部から推測させない。
- private resourceの列挙を防止する。

Repository / Serviceは概念上、

```text
findByIdAndUserId(workId, userId)
```

相当のowner-scoped lookupを使用する。

取得結果が空の場合：

- 「不存在」と「他人所有」を区別するための追加ID検索を原則行わない。
- `WorkNotFoundException`として統一する。
- ユーザー向け表示では理由を区別しない。

---

### 409 Conflict

409は、入力自体は有効だが、
現在のサーバー状態との競合により更新できない場合に使用する。

初期版の主用途：

- S-11自動保存のexpected値競合

例：

```text
DB値 = 12
expectedValue = 10
送信value = 11
```

この場合、

```text
expectedValue != DB値
```

のため409。

競合判定はControllerではなくServiceで行う。

処理順：

```text
owner-scoped取得
↓
PESSIMISTIC_WRITE
↓
最新DB状態確認
↓
expected比較
↓
不一致
↓
AutoSaveConflictException
```

競合時：

- Entity更新なし
- DB更新なし
- `last_updated_at`更新なし
- partial saveなし
- 自動mergeなし
- 自動再送なし
- stale値再適用なし

レスポンス：

```json
{
  "code": "CONFLICT"
}
```

DB現在値、expected値、他ユーザー情報等は返さない。

JavaScript：

- 当該保存チャンネルのみ`CONFLICT`
- 通常再試行なし
- ページ再読み込みを案内
- 他の独立チャンネルは継続可能
- status / completionDateは同一`workState`チャンネルなので、
  どちらかが競合した場合は`workState`全体を停止

---

### 5xx / 500 Internal Server Error

5xxは予期しないサーバー・システム障害に使用する。

例：

- 想定外RuntimeException
- DB接続障害
- JPA / Hibernate障害
- 想定外DataAccessException
- 画像I/O失敗
- ストレージ障害
- サーバー側テンプレート描画失敗
- その他予期しないシステム障害

#### 通常HTML

- 原則HTTP 500
- 共通500画面
- 内部情報を表示しない
- トップへ自動redirectしない

表示例：

```text
処理中にエラーが発生しました。
時間をおいてもう一度お試しください。
```

#### S-11

JavaScriptは500だけでなく500～599を共通のサーバー障害として扱う。

- 当該チャンネルを`ERROR`
- 「保存できませんでした」
- 最新pending値を保持
- 自動再試行なし
- ユーザー操作による手動再試行可

5xx JSON本文は必須としない。

初期版では502 / 503 / 504等をアプリ独自に細分化しない。

---

### 429 Too Many Requests

対象：

- `POST /login`
- `POST /register`

Rate Limit超過時にHTTP 429を使用する。

#### Login

- HTTP 429を維持
- S-03ログイン画面を直接再表示
- `/login?error`へredirectしない

表示趣旨：

```text
ログイン試行が多すぎます。
しばらく時間をおいてから再度お試しください。
```

保持：

- username：保持しない
- password：保持しない
- Remember-Me：OFF

#### Register

- HTTP 429を維持
- S-02ユーザー登録画面を直接再表示

保持：

- email：保持可
- username：保持可
- password：クリア
- password確認：クリア

表示趣旨：

```text
登録の試行回数が多すぎます。
しばらく時間をおいてから再度お試しください。
```

#### その他

- `error/429.html`は作らない。
- bucket種類を表示しない。
- 残回数を表示しない。
- 正確な解除時刻を表示しない。
- 初期版では`Retry-After`を送信しない。
- 自動再送しない。
- 自動リロードしない。
- カウントダウン表示を作らない。
- CSRF不正は429より優先して403。
- CSRF不正リクエストはRate Limitへ加算しない。
- 初期版で429 JSON仕様は作らない。

---

### 共通HTMLエラー画面

以下を作成する。

```text
src/main/resources/templates/error/
  400.html
  403.html
  404.html
  500.html
```

4画面を基本とする。

#### 400

用途：

- malformed request
- PathVariable型変換失敗
- 通常フォーム以外のリクエスト形式不正等

表示例：

```text
リクエストを正しく処理できませんでした。
```

#### 403

用途：

- Securityアクセス拒否
- CSRF不正
- `anyRequest().denyAll()`
- `GET /logout`

表示例：

```text
このページを表示する権限がありません。
```

#### 404

用途：

- 作品不存在
- 他人所有作品

表示例：

```text
お探しのページまたは作品が見つかりません。
```

「削除されています」「他のユーザーの作品です」等の
原因を推測できる表現は使用しない。

#### 500

表示例：

```text
処理中にエラーが発生しました。
時間をおいてもう一度お試しください。
```

#### 共通ルール

各ページ：

- HTTPエラーステータスを維持する。
- 短い見出しと説明を表示する。
- 最低1つのナビゲーションを持つ。
- 「トップへ戻る」→ `GET /`
- `history.back()`のみを唯一の戻り方にしない。

表示禁止：

- Exceptionクラス名
- stack trace
- SQL
- DB schema
- table / columnの内部詳細
- file path
- Controller / Service名
- Spring / Hibernate内部情報
- 内部識別子
- password hash等

共通レイアウトはThymeleaf fragmentsで再利用してよい。

---

### ControllerAdvice / Securityの責務分担

エラー処理は3系統に分離する。

#### 通常HTML MVC

```text
@ControllerAdvice
```

相当を使用する。

担当：

- `WorkNotFoundException`
- 通常HTMLの業務例外
- 通常HTMLの想定外システム例外
- 共通HTMLエラー画面への変換

通常FormのBindingResultエラーはAdviceへ送らず、
Controllerで同じ入力画面を再表示する。

#### S-11非同期

S-11専用の、

```text
@RestControllerAdvice
```

相当を使用する。

例：

```text
WorkAutoSaveExceptionHandler
```

Controllerの適用範囲は、
`WorkAutoSaveController`に限定することを基本とする。

担当例：

- 400 Validation JSON
- 404 status
- 409 Conflict JSON
- 500 status

アプリ全体を無差別にREST Advice化しない。

#### Spring Security

401 / 403のSecurity起因エラーはAdviceへ依存しない。

- 未認証 → `AuthenticationEntryPoint`
- Access Denied / CSRF → `AccessDeniedHandler`

Service / Controllerで独自にログイン状態を判定して401を返す構成は採用しない。

---

### 業務例外クラス

初期版では次の4種類を主要カスタム例外とする。

```text
WorkNotFoundException
AutoSaveConflictException
BasicInfoConflictException
InvalidCompletionDateException
```

全て`RuntimeException`系を基本とする。

抽象`BusinessException`基底クラスは初期版では作成しない。

#### WorkNotFoundException

用途：

- 本人所有作品を取得できない場合
- 実不存在と他人所有を同じ例外で扱う

外部：

- 通常HTML → 404
- S-11 → 404

#### AutoSaveConflictException

用途：

- S-11 expected値競合

外部：

- HTTP 409
- `{"code":"CONFLICT"}`

#### BasicInfoConflictException

用途：

- S-11基本情報
  - 作品名
  - カテゴリー
  - 写真
- submission token / snapshot競合

外部：

- 303 PRG
- 最新S-11へ戻る
- Flash Message表示

#### InvalidCompletionDateException

用途：

ServiceでDB状態やClockを参照して判定する完成日ルール違反。

例：

- 未来日
- IN_PROGRESS状態なのに手動完成日変更
- completion date business rule違反

外部的にはユーザー修正可能な入力エラーとして400に変換可能。

必要であれば例外内部に、

```text
Reason.FUTURE_DATE
Reason.WORK_NOT_COMPLETED
```

等の機械的なreasonを保持してよい。

ただしUI文言をService例外へ直接保持することは原則避ける。

#### 作らない例外

初期版では以下を必須化しない。

```text
WorkAccessDeniedException
DuplicateUsernameException
DuplicateEmailException
ImageBusinessException
```

他人所有は`WorkNotFoundException`へ統一する。

username / email重複は入力エラーとして扱う。

---

### 入力エラーと業務エラーの境界

判定された層だけでHTTPステータスを決めない。

重要なのは、

```text
ユーザーが入力・操作を修正すれば解決できるか
```

である。

例：

`InvalidCompletionDateException`はServiceで検出されるが、
外部的にはValidation Errorとして400でよい。

#### Web入力層

担当：

- Bean Validation
- Binding
- 型変換
- 必須
- 文字数
- 数値範囲
- Enum形式
- 日付形式

S-11ではServiceを呼ばず400。

#### Service

担当：

- owner
- expected競合
- status / completion date整合
- completion date <= today
- 状態遷移
- DB現在値依存の業務検証

---

### 画像エラーの分類

画像処理失敗を一括で500にしない。

#### 入力エラー

例：

- 0 byte
- 最大10MiB超過
- JPEG / PNG以外
- MIME不一致
- 実形式不一致
- 認識不能
- 壊れた画像
- 最大8192px超過
- decode不可

通常HTMLフォームでは、

- 共通400画面へ送らない。
- S-08 / S-11の元画面へ項目エラーとして再表示する。

ユーザーが選択したfile入力はサーバーから復元しない。
必要なら再選択してもらう。

#### システムエラー

例：

- file write failure
- permission error
- disk full
- storage unavailable
- 想定外Image I/O
- library failure

これらは500系システムエラーとして扱う。

---

### username / email重複とDB UNIQUE

通常の重複は事前にService等で検証し、
項目エラーとして扱う。

同時登録競合等によりDB UNIQUE制約違反となった場合でも、
原因が安全かつ明確に、

- username重複
- email重複

と特定できる場合は、通常の項目エラーへ変換する。

一方、全ての`DataIntegrityViolationException`を400へ変換しない。

想定していない、

- FK違反
- CHECK違反
- その他DB整合性異常
- SQL異常

等は500とする。

---

### 非同期エラーJSON

JSONを返す場合は、
必ず文字列の`code`を持つ最小構成を基本とする。

基本：

```json
{
  "code": "ERROR_CODE"
}
```

必要な場合だけ`fieldErrors`を追加する。

#### Validation Error

```json
{
  "code": "VALIDATION_ERROR",
  "fieldErrors": {
    "value": "0～9999の整数で入力してください。"
  }
}
```

複数項目も可：

```json
{
  "code": "VALIDATION_ERROR",
  "fieldErrors": {
    "completionDate": "未来の日付は指定できません。",
    "status": "制作中の作品には完成日を設定できません。"
  }
}
```

Bean Validation由来とService業務検証由来の400を、
クライアント向けには同じ`VALIDATION_ERROR`形式へ統一する。

#### Conflict

```json
{
  "code": "CONFLICT"
}
```

#### body不要のステータス

以下はbodyなしでも正しい仕様とする。

```text
401
403
404
5xx
```

JavaScriptは必ずHTTP statusを先に判定し、
必要時のみJSON解析する。

#### ErrorResponse DTO

必要なら小さな共通DTOを1種類作成してよい。

概念：

```text
ErrorResponse
- code
- fieldErrors（任意）
```

以下を共通必須にしない。

- message
- timestamp
- status
- path
- exception
- trace

HTTPステータスごとに大量のError DTOを作らない。

Spring Boot標準error JSONの構造をKnittingのAPI仕様として依存しない。

---

### ユーザー向けエラーメッセージ管理

全メッセージを1つのJavaクラスへ無理に集約しない。

表示責務を持つ層で管理する。

#### Form

Bean Validation等の単純入力文言。

例：

```java
@NotBlank(message = "作品名を入力してください。")
```

初期版は日本語のみのため、
Formアノテーションへ直接文言を設定してよい。

共有量が増えた場合は`messages.properties`への切り出しを許容する。

#### Service / Exception

Service例外に、
ブラウザ表示用の日本語文言を直接持たせない。

必要ならmachine-readableなreasonのみ保持する。

#### S-11 JavaScript

401 / 403 / 404 / 409 / 5xx等の固定的な案内は、

```text
src/main/resources/static/js/work-detail.js
```

内の定数等へ集約する。

例：

```text
401:
ログイン状態が切れたため保存できませんでした。再度ログインしてください。

403:
保存処理を続けられません。ページを再読み込みしてください。

404:
作品を確認できません。ページを再読み込みするか一覧へ戻ってください。

409:
他の変更と競合したため保存できませんでした。ページを再読み込みしてください。

5xx:
保存できませんでした。
```

#### 共通HTML

400 / 403 / 404 / 500の固定文言は
各Thymeleaf template側で管理する。

---

### ログ方針

HTTPステータスだけで機械的にログレベルを決めない。

ユーザー操作として通常発生し得るか、
運用上調査が必要かによって決定する。

| 状況 | 基本ログ |
|---|---|
| 400 | 原則なし / DEBUG |
| 401 | 原則なし / DEBUG |
| 403 | WARN |
| 404 | 原則なし / DEBUG |
| 409 | INFO |
| 500 / 5xx | ERROR |

#### 400

通常入力ミスのためWARNにしない。

スタックトレース不要。

#### 401

Session切れ等で通常発生し得るため、
原則WARNにしない。

#### 403

Security上確認価値があるためWARNを基本とする。

ただし毎回stack traceを出す必要はない。

#### 404

原則ログなし。

他人所有だったか不存在だったかを、
ユーザー向けと同様、通常ログへ過度に詳細記録しない。

#### 409

正常な競合制御結果としてINFO。

stack trace不要。

#### 500

ERROR＋stack trace。

原因調査可能なサーバーログを残す。

#### ログへ出してよいもの

必要最小限：

- HTTP method
- endpoint / 処理名
- HTTP status
- exception class
- 必要な内部ID
- error種別

#### ログへ出さないもの

禁止：

- password
- password_hash
- password confirmation
- Remember-Me token
- Remember-Me key
- CSRF token
- Session ID
- Cookie全体
- Authorization情報
- Form全体dump
- request param/headerの無差別dump
- 制作メモ本文
- 画像bytes
- 不要なemail
- 不要なusername
- 元画像ファイル名

`log.debug("form={}", form)`のような
Formオブジェクト丸ごとのログ出力は禁止する。

#### 二重ログ

同じ例外をServiceとAdviceで二重にログしない。

原則：

```text
最終的に処理する境界で1回
```

ただし、上位へ例外を投げず処理を継続する補償処理失敗は例外。

例：

- 旧画像補償削除失敗
- candidate image cleanup failure

この場合は処理箇所でWARN / ERRORを記録してよい。

---

### `GET /logout`

正式なログアウトは、

```text
POST /logout
```

のみ。

Spring Security標準LogoutFilterを使用する。

採用しない：

- GET logout
- Spring Security標準GET logout confirmation page
- 独自Logout Controller
- fetch logout

`GET /logout`直接アクセス：

```text
Security拒否
↓
403
↓
共通403画面
```

ログアウト処理そのものは絶対に実行しない。

正常`POST /logout`では、

- SecurityContext clear
- current Session invalidate
- Remember-Me cookie削除
- success `/`

CSRF不正なら403。

---

### 異常なpassword_hash

`PasswordEncoderFactories.createDelegatingPasswordEncoder()`を使用する前提。

異常例：

- `{id}` prefix欠損
- unknown id
- encoding破損
- 不正なhash

処理：

- bcryptだと推測してfallbackしない。
- 認証成功させない。
- fail closed。
- ユーザーにはpassword_hash異常を知らせない。
- 外部的には通常ログイン失敗と区別しない。
- `/login?error`相当のログイン失敗として扱う。
- 内部ではデータ / システム異常としてERRORログ対象。
- password_hash本体をログへ出さない。
- 必要なら内部userIdのみ利用する。

---

### 認証処理中のDB障害

Login中に、

```text
KnittingUserDetailsService
↓
Repository
↓
DB障害
```

となった場合：

- 認証を成功させない。
- fail closed。
- DB / SQL / Exception詳細をユーザーへ表示しない。
- 内部ではERRORログ。
- 初期版では独自AuthenticationProviderを作らない。
- 既存方針どおり独自AuthenticationFailureHandlerも追加しない。

通常作品機能でのDB障害：

- HTML → 500
- S-11 → 5xx

---

### Spring Boot標準 `/error`

Spring Boot標準の、

```text
/error
BasicErrorController
```

は削除・置換しない。

初期版では独自`ErrorController`を作らない。

役割：

```text
ControllerAdvice等で処理しきれなかったエラー
↓
Spring Boot標準 /error
↓
最終フォールバック
```

共通`templates/error/*.html`は、
独自AdviceとSpring Boot標準error handlingの双方から利用できるものとする。

`/error`を通常ナビゲーションURLとして使用しない。

---

### redirect / forward / error dispatch

#### 通常MVC既知例外

```text
Exception
↓
@ControllerAdvice
↓
HTTP status
+
Thymeleaf error view
```

`redirect:/error`しない。

`forward:/error`しない。

#### 通常MVC想定外例外

```text
Exception
↓
GlobalExceptionHandler
↓
ERRORログ
↓
HTTP 500
+
error/500.html
```

#### Security通常HTML403

推奨経路：

```text
Security
↓
AccessDeniedHandler
↓
response.sendError(403)
↓
Servlet ERROR dispatch
↓
Spring Boot /error
↓
error/403.html
```

#### S-11 401 / 403

HTML error dispatchしない。

```text
HTTP statusを直接返す
↓
JavaScript処理
```

#### redirectを使ってよいケース

エラー画面表示目的以外の本来のフロー。

例：

- 未認証通常画面 → `/login?required`
- 正常PRG
- 登録成功後
- logout成功後

#### manual forward

初期版ではアプリケーションコードから
手動forwardを使用しない。

#### ERROR dispatcherとSecurity

`anyRequest().denyAll()`を維持しながら、
Spring Boot / Servlet Containerの正規ERROR dispatchを
再度Securityが拒否しないようにする。

概念上：

```text
DispatcherType.ERROR
```

等の内部error dispatchを通す。

これは外部から`/error`を公開ページにする意味ではない。

---

### S-11基本情報編集競合

対象：

```text
POST /works/{id}/basic-info
```

項目：

- 作品名
- 写真
- カテゴリー

basic infoは通常HTML Formであり、
S-11自動保存とは競合処理を分ける。

#### 競合判定

submission token / snapshotを使用。

概念：

```text
GET表示時snapshot
↓
POST
↓
owner確認
↓
最新DB取得
↓
snapshot比較
↓
差異あり
↓
BasicInfoConflictException
```

#### 外部処理

競合時：

- HTTP 409 HTMLを直接描画しない。
- PRGを使用。
- HTTP 303 See Other。
- `GET /works/{id}`へ遷移。

```text
POST
↓
BasicInfoConflictException
↓
DB更新なし
↓
candidate image補償削除
↓
Flash Attribute
↓
303
↓
GET /works/{id}
↓
最新DB状態表示
```

#### stale入力

競合したPOSTの入力値はリダイレクト後へ再適用しない。

最新DB値を表示する。

#### 写真

競合した新規画像は保持しない。

- candidate imageが作成済みなら補償削除。
- 現在の正式画像を維持。
- ユーザーが変更を続ける場合は画像を再選択。

#### Flash Message

競合案内はFlash Attribute等で1度だけ表示する。

表示趣旨：

```text
別の画面で作品情報が更新されたため、今回の変更は保存されませんでした。
最新の内容を確認して、もう一度編集してください。
```

URLへ`?conflict=true`等を恒久的に残さない。

#### 新しいsnapshot

リダイレクト後のGETでは、

- 最新DB状態
- 新しいsubmission token
- 新しいsnapshot

を発行する。

#### last_updated_at

競合したリクエストでは更新しない。

#### 作品取得不能

本人所有作品自体が取得できない場合は競合ではない。

```text
WorkNotFoundException
↓
404
```

---

### 共通エラー処理の推奨パッケージ

既存のpackage by layer方針へ、

```text
exception
```

パッケージを追加することを推奨する。

概念：

```text
controller
service
repository
entity
domain
form
dto
security
config
exception
```

例：

```text
exception/
  WorkNotFoundException
  AutoSaveConflictException
  BasicInfoConflictException
  InvalidCompletionDateException
  GlobalExceptionHandler
  WorkAutoSaveExceptionHandler
```

厳密なクラス配置・クラス名は、
本書の責務分担を変えない範囲で実装時調整可。

---

---

## 9. Form・Validation設計

### 基本方針

#### 入力処理の基本フロー

画面入力は原則として次の流れで処理します。

```text
Thymeleaf
↓
Form
↓
Controller
↓
Service
↓
Repository
↓
Entity
↓
MySQL
```

FormはWeb入力を受け取るためのクラスであり、Entityを直接リクエスト入力へ使用しません。

ControllerはFormから型付き値を取り出してServiceへ渡し、ServiceはFormそのものを受け取りません。

---

#### 責務分担

##### Form / Web入力層

担当：

- 必須チェック
- 文字数
- 数値範囲
- 小数桁
- 文字形式
- EnumのBinding
- LocalDateのBinding
- Form内部の値だけで判断できるcross-field条件
- 型変換エラー
- Bean Validation

##### Controller

担当：

- HTTPリクエスト受付
- Form Binding
- `BindingResult`確認
- PrincipalからuserId取得
- Serviceへの型付き値の受け渡し
- 通常HTML Formの再表示
- S-11非同期Validation JSONの返却
- Service業務エラーを画面・JSON用表現へ変換

##### Service

担当：

- username / email重複
- Password Blocklist
- 作品所有者確認
- expected値競合
- snapshot競合
- ステータス・完成日の整合性
- 完成日が未来日でないこと
- 状態遷移
- DB現在状態を必要とする業務Validation
- Entity生成・更新
- トランザクション

##### Repository

担当：

- DB取得
- owner条件付き取得
- 必要な行ロック

##### Entity

担当：

- 永続状態の保持

##### DB

担当：

- NOT NULL
- UNIQUE
- 外部キー
- CHECK
- その他の構造的整合性

##### JavaScript

担当：

- UX補助
- 入力支援
- ボタン無効化
- 文字数表示
- 画像プレビュー
- debounce
- 自動保存pending管理

JavaScriptのみを正式なValidation手段にはしません。

---

### Form適用範囲

#### 使用するFormクラス

初期版では次の9クラスを使用します。

```text
UserRegistrationForm
GaugeAdjustmentForm
YarnCombinationForm
WorkCreateForm
WorkBasicInfoForm
CounterUpdateForm
MemoUpdateForm
StatusUpdateForm
CompletionDateUpdateForm
```

---

#### Formを作成しない対象

以下には専用Java Formを作成しません。

```text
S-01 トップ
S-03 ログイン
S-06 簡易カウンター
S-07 毛糸・推奨針号数対応表
S-09 制作中一覧
S-10 完成一覧
S-11 GET表示
作品削除
```

S-03ログインのPOSTはSpring Securityが直接処理します。

---

### Form / DTO / Entityの境界

#### Form

Formは画面入力の受け皿です。

Formには以下を含めます。

- 画面から送信される入力値
- Bean Validation
- Form内のcross-field Validation

Formには以下を含めません。

- Entity
- Repository
- DBアクセス処理
- owner判定
- DB現在状態依存の業務ルール
- Web画面向け日本語メッセージを持つServiceロジック

---

#### DTO

FormとDTOを1対1で作成しません。

DTO / Result DTOは、画面出力やAPIレスポンス等で本当に必要な場合だけ使用します。

既存のS-11 Result DTOは維持します。

```text
CounterSaveResult
StatusSaveResult
CompletionDateSaveResult
```

---

#### Entity

Entityは永続化専用です。

以下は禁止します。

```java
@ModelAttribute Work work
```

また、Controllerで以下のような一括コピーは行いません。

```java
BeanUtils.copyProperties(form, entity);
```

Entityの生成・更新はServiceが担当します。

---

### Formで使用するJava型

| 入力種類 | Java型 |
| --- | --- |
| 一般文字列 | `String` |
| 整数 | `Integer` |
| 小数 | `BigDecimal` |
| 選択値 | Enum |
| 日付 | `LocalDate` |
| 画像 | `MultipartFile` |
| URLのID | `Long` PathVariable |

Formでは原則としてprimitive型を使用せず、空欄と0を区別できるwrapper型を使用します。

---

### Binding・型変換方針

#### Integer

整数入力は`Integer`を使用します。

```text
空欄
→ null

0
→ 0

abc
→ Binding error

1.5
→ Binding error
```

---

#### BigDecimal

S-04 / S-05の小数入力は`BigDecimal`を使用します。

`double` / `Double`は使用しません。

---

#### Enum

選択値はStringではなくEnumへBindingします。

例：

```text
WorkCategory
WorkStatus
CounterType
YarnCombinationMode
```

存在しないEnum値は入力エラーです。

---

#### LocalDate

日付は`LocalDate`を使用します。

推奨：

```java
@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
```

HTMLの`<input type="date">`から`yyyy-MM-dd`として受け取ります。

不正な日付文字列はBinding errorとします。

---

#### MultipartFile

画像は`MultipartFile`で受け取ります。

写真は任意のため`@NotNull`は付けません。

画像内容の正式な検証は既存画像Validationコンポーネントで実施します。

---

### Bean Validation共通方針

#### 標準Annotationを優先

基本的に以下を使用します。

```text
@NotBlank
@NotNull
@NotEmpty
@Size
@Min
@Max
@DecimalMin
@DecimalMax
@Digits
@Pattern
@Email
```

標準Annotationで自然に表現できない場合のみCustom Validatorを使用します。

---

#### `@NotBlank`

一般的な必須Stringへ使用します。

ただしpasswordはASCIIスペース自体が有効文字であるため、`@NotBlank`を使用しません。

---

#### `@NotNull`

Integer、BigDecimal、Enum、LocalDate等の必須値へ使用します。

---

#### `@Size`

ASCII等、Java `String.length()`と仕様上の文字数が一致する文字列に使用します。

Unicode自由記述の作品名・制作メモでは使用せず、`@CodePointLength`を使用します。

---

#### 完成日の未来日

`@PastOrPresent`は使用しません。

完成日の未来日判定はServiceで`Clock`を用いて行います。

---

### 文字列の正規化・空白ルール

#### username

処理：

```text
入力
↓
strip()
↓
4～20文字
↓
半角英数字
↓
DB保存時は入力時の大文字小文字を維持
↓
重複・ログイン判定はIgnoreCase
```

Unicode normalizationは行いません。

内部空白は除去せず、そのまま形式不正となります。

---

#### email

処理：

```text
入力
↓
strip()
↓
toLowerCase(Locale.ROOT)
↓
メール形式Validation
↓
Service重複Validation
↓
DB保存
```

内部空白は無効です。

Unicode normalizationは行いません。

---

#### password

以下を一切行いません。

```text
trim
strip
lowercase
uppercase
Unicode normalization
```

ASCIIスペースはpasswordの有効文字です。

---

#### passwordConfirmation

passwordと同様、一切加工しません。

完全一致で比較します。

---

#### 作品名

`strip()`してからValidation・保存します。

内部空白は保持します。

空白のみの作品名は`strip()`後に空文字となり必須エラーです。

---

#### 制作メモ

以下を一切行いません。

```text
trim
strip
case変換
Unicode normalization
```

前後空白・内部空白・改行はそのまま保存します。

---

#### グローバルString変換

`StringTrimmerEditor`等の全Form共通自動trimは使用しません。

フィールドごとの必要な正規化のみ行います。

---

### null・空文字・未入力の扱い

#### 一般ルール

空文字をグローバルにnullへ変換しません。

FormのnullとDBのNULLを機械的に同一視しません。

---

#### 必須String

username、email、作品名は、null・空文字・空白のみを必須エラーとして扱います。

---

#### password

passwordはASCIIスペースが有効なため`@NotBlank`を使用しません。

---

#### 制作メモ

制作メモ未入力は、

```text
""
```

として扱います。

DBでは`NULL`にしません。

空白のみのメモもそのまま保存可能です。

---

#### 数値

数値入力の空欄はnullです。

0は0として扱います。

---

#### LocalDate

日付未入力はnullです。

不正形式はBinding errorです。

---

#### Optional

Formフィールドに`Optional<T>`は使用しません。

---

### Unicode文字数

#### 文字数単位

Unicode自由記述文字列では、

```text
1文字 = 1 Unicode code point
```

とします。

Javaの`String.length()`は使用しません。

---

#### Java側

```java
text.codePointCount(0, text.length())
```

を使用します。

---

#### 対象

```text
作品名：最大50 code points
制作メモ：最大1000 code points
```

---

#### JavaScript側

JavaScriptも同じ基準に合わせます。

例：

```javascript
Array.from(value).length
```

---

#### ZWJ等

家族絵文字等の複合emojiはgrapheme clusterではなく、複数code pointとして数えます。

---

### Custom Validator一覧

初期版では以下の5種類のみ使用します。

```text
@CodePointLength
@PasswordMatches
@ValidGaugeAdjustment
@ValidYarnCombination
@ValidPhotoOperation
```

以下は作成しません。

```text
@UniqueUsername
@UniqueEmail
@ValidCompletionDate
@WorkOwner
@ExpectedValueMatches
@ValidImage
```

DBアクセスを必要とするValidationはService、画像本体検査は画像コンポーネントの責務です。

---

### Custom Validator実装方針

#### 配置

推奨：

```text
form/
  UserRegistrationForm
  GaugeAdjustmentForm
  YarnCombinationForm
  WorkCreateForm
  WorkBasicInfoForm
  CounterUpdateForm
  MemoUpdateForm
  StatusUpdateForm
  CompletionDateUpdateForm

  validation/
    CodePointLength
    CodePointLengthValidator
    PasswordMatches
    PasswordMatchesValidator
    ValidGaugeAdjustment
    ValidGaugeAdjustmentValidator
    ValidYarnCombination
    ValidYarnCombinationValidator
    ValidPhotoOperation
    ValidPhotoOperationValidator
```

---

#### Repository / Serviceを呼ばない

Custom ValidatorからDBアクセスしません。

---

#### Form値を書き換えない

Validatorは判定とConstraintViolation登録のみ行います。

正規化はValidation前のForm setter等で行います。

---

#### 例外をValidation失敗に使わない

Validation失敗は`ConstraintValidatorContext`で表現します。

---

### `@CodePointLength`

概念：

```java
@CodePointLength(max = 50)
private String name;
```

```java
@CodePointLength(max = 1000)
private String memo;
```

`null`は`@CodePointLength`単体ではvalidとします。

必須判定は別Annotationで行います。

---

### `@PasswordMatches`

`UserRegistrationForm`へclass-levelで付与します。

passwordとpasswordConfirmationを完全一致比較します。

不一致の場合は`passwordConfirmation`フィールドへ、

```text
パスワードが一致していません。
```

を付与します。

---

### `@ValidGaugeAdjustment`

目数側・段数側それぞれ3項目の入力完全性のみ確認します。

```text
3項目すべてnull
→ valid

3項目すべて入力済み
→ valid

一部のみ入力
→ 未入力項目へfield error
```

数値範囲・小数桁等は標準Annotationへ任せます。

---

### `@ValidYarnCombination`

選択中モードに必要な項目だけをValidationします。

```text
DOUBLE_STRAND
→ doubleStrandWeight
→ doubleStrandLength

COMBINATION
→ yarn1Weight
→ yarn1Length
→ yarn2Weight
→ yarn2Length
```

非選択モードの値は業務Validation対象外です。

ただし、非選択モードの値であってもBigDecimalへBindingできない不正文字列が送信された場合はBinding errorとなります。

---

### `@ValidPhotoOperation`

`WorkBasicInfoForm`で、

```text
新しいphotoあり
AND
deletePhoto = true
```

を拒否します。

メッセージ：

```text
写真の変更と削除を同時に行うことはできません。
```

0-byteファイルも「ユーザーがファイルを選択した」状態としてcross-field判定対象にします。

実画像内容の正否は画像Validation側で判定します。

---

### UserRegistrationForm

#### フィールド

```java
private String email;
private String username;
private String password;
private String passwordConfirmation;
```

---

#### email

```java
@NotBlank(message = "メールアドレスを入力してください。")
@Size(max = 255, message = "メールアドレスは255文字以内で入力してください。")
@Email(message = "正しいメールアドレス形式で入力してください。")
private String email;
```

setter等で、

```text
strip()
↓
toLowerCase(Locale.ROOT)
```

を適用します。

Serviceで重複確認します。

重複メッセージ：

```text
このメールアドレスはすでに登録されています。
```

---

#### username

```java
@NotBlank(message = "ユーザー名を入力してください。")
@Size(
    min = 4,
    max = 20,
    message = "ユーザー名は4～20文字の半角英数字で入力してください。"
)
@Pattern(
    regexp = "^[A-Za-z0-9]+$",
    message = "ユーザー名は4～20文字の半角英数字で入力してください。"
)
private String username;
```

setter等で`strip()`します。

DB保存時はcaseを保持します。

重複判定はcase-insensitiveです。

重複メッセージ：

```text
このユーザー名はすでに使用されています。
```

---

#### password

```java
@NotNull(message = "パスワードは8～64文字で入力してください。")
@Size(
    min = 8,
    max = 64,
    message = "パスワードは8～64文字で入力してください。"
)
@Pattern(
    regexp = "^[\\x20-\\x7E]+$",
    message = "パスワードは半角の英数字・記号・スペースで入力してください。"
)
private String password;
```

ルール：

- 8～64文字
- printable ASCII
- ASCII space許可
- control char禁止
- 非ASCII禁止
- 特定文字種の組み合わせ必須ルールなし
- trim / normalizeなし

---

#### passwordConfirmation

```java
@NotEmpty(message = "確認用パスワードを入力してください。")
private String passwordConfirmation;
```

`@NotBlank`は使用しません。

ASCIIスペースのみの文字列自体はpassword仕様上の有効文字として扱います。

---

#### password一致

`@PasswordMatches`を使用します。

メッセージ：

```text
パスワードが一致していません。
```

---

#### Password Blocklist

Serviceで確認します。

一致時：

```text
このパスワードは推測されやすいため使用できません。別のパスワードを設定してください。
```

Blocklistは登録時のみ適用し、ログイン時には使用しません。

---

#### 登録失敗時

保持：

```text
email
username
```

クリア：

```text
password
passwordConfirmation
```

passwordはHTMLへ再出力しません。

---

#### autocomplete

```text
email                → email
username             → username
password             → new-password
passwordConfirmation → new-password
```

---

#### 成功時

登録成功後は自動ログインしません。

```text
/login?registered
```

へredirectします。

---

### GaugeAdjustmentForm

#### フィールド

```java
private BigDecimal specifiedStitchGauge;
private BigDecimal measuredStitchGauge;
private Integer originalStitchCount;

private BigDecimal specifiedRowGauge;
private BigDecimal measuredRowGauge;
private Integer originalRowCount;
```

---

#### gauge値

範囲：

```text
0.01～999.99
```

小数：

```text
最大2桁
```

推奨Annotation：

```java
@DecimalMin("0.01")
@DecimalMax("999.99")
@Digits(integer = 3, fraction = 2)
```

---

#### 元の目数・段数

```text
1～9999
```

推奨Annotation：

```java
@Min(1)
@Max(9999)
```

---

#### `@NotNull`を付けない理由

目側・段側を個別に省略可能とするため、6フィールドへ一律`@NotNull`を付けません。

---

#### cross-fieldルール

目側・段側それぞれ：

```text
3項目すべて未入力
→ 計算対象外・エラーなし

3項目すべて入力
→ 計算対象

一部のみ入力
→ 不足項目へfield error
```

`@ValidGaugeAdjustment`で判定します。

---

#### URL

```text
GET /gauge-adjustment
```

計算も同じGETです。

パラメータ名：

```text
specifiedStitchGauge
measuredStitchGauge
originalStitchCount
specifiedRowGauge
measuredRowGauge
originalRowCount
```

`calculate=true`は使用しません。

6項目すべてnullなら初期表示として扱います。

---

#### 入力エラー時

Serviceを呼ばず、同じ画面をHTTP 200で再表示します。

---

### YarnCombinationForm

#### Enum

```java
enum YarnCombinationMode {
    DOUBLE_STRAND,
    COMBINATION
}
```

表示：

```text
DOUBLE_STRAND → 2本取り
COMBINATION   → 引き揃え
```

初期値：

```text
DOUBLE_STRAND
```

---

#### フィールド

```java
private YarnCombinationMode mode;

private BigDecimal doubleStrandWeight;
private BigDecimal doubleStrandLength;

private BigDecimal yarn1Weight;
private BigDecimal yarn1Length;

private BigDecimal yarn2Weight;
private BigDecimal yarn2Length;
```

---

#### 重量・長さ

すべて、

```text
0.01～99999.99
小数第2位まで
```

とします。

---

#### mode

```java
@NotNull(message = "計算モードを選択してください。")
private YarnCombinationMode mode;
```

不正Enum時：

```text
計算モードが正しくありません。
```

---

#### mode別Validation

##### DOUBLE_STRAND

必須：

```text
doubleStrandWeight
doubleStrandLength
```

##### COMBINATION

必須：

```text
yarn1Weight
yarn1Length
yarn2Weight
yarn2Length
```

---

#### URL

```text
GET /yarn-combination
```

初期表示と計算要求を区別するため、

```text
calculate=true
```

を使用します。

例：

```text
/yarn-combination?mode=DOUBLE_STRAND&doubleStrandWeight=50&doubleStrandLength=120&calculate=true
```

```text
/yarn-combination?mode=COMBINATION&yarn1Weight=50&yarn1Length=120&yarn2Weight=40&yarn2Length=100&calculate=true
```

---

#### 非選択モードの値保持

画面を離れていない間、モードを切り替えても各モードの入力値を保持します。

結果は選択中モードのみ表示します。

---

### WorkCreateForm

#### フィールド

```java
private String name;
private MultipartFile photo;
private WorkCategory category;
private WorkStatus status;
private String submissionToken;
```

---

#### name

```java
@NotBlank(message = "作品名を入力してください。")
@CodePointLength(
    max = 50,
    message = "作品名は50文字以内で入力してください。"
)
private String name;
```

setter等で`strip()`します。

重複は許可します。

---

#### category

```java
@NotNull(message = "カテゴリーを選択してください。")
private WorkCategory category;
```

---

#### status

```java
@NotNull(message = "制作ステータスを選択してください。")
private WorkStatus status;
```

GET `/works/new`では初期値を`IN_PROGRESS`とします。

POSTで値が欠落している場合は自動補完せず入力エラーです。

---

#### completionDate

Formには含めません。

Serviceが、

```text
IN_PROGRESS
→ completionDate = null

COMPLETED
→ completionDate = LocalDate.now(clock)
```

とします。

---

#### counters / memo

Formには含めません。

Service初期値：

```text
stitchCount = 0
rowCount = 0
memo = ""
```

---

#### userId等

Formには含めません。

```text
userId
ownerId
photoReference
```

はサーバー側で取得・生成します。

---

#### photo

任意です。

`@NotNull`や`@ValidImage`は付けません。

画像詳細Validationは画像コンポーネントで行います。

---

#### submissionToken

```java
@NotBlank
private String submissionToken;
```

CSRFとは別の一回限り識別子です。

DBへ保存しません。

---

#### 成功時

```text
POST /works
↓
登録成功
↓
redirect /works/{id}
```

---

### WorkBasicInfoForm

#### フィールド

```java
private String name;
private MultipartFile photo;
private WorkCategory category;
private Boolean deletePhoto;
private String submissionToken;
```

---

#### name

WorkCreateFormと同じです。

---

#### category

WorkCreateFormと同じです。

---

#### photo / deletePhoto

4状態を扱います。

```text
photoなし / deletePhoto=false
→ 現在写真を維持

photoあり / deletePhoto=false
→ 新写真へ差し替え

photoなし / deletePhoto=true
→ 写真削除

photoあり / deletePhoto=true
→ 入力エラー
```

矛盾メッセージ：

```text
写真の変更と削除を同時に行うことはできません。
```

---

#### hiddenで送信しない値

以下はブラウザへhiddenで送信しません。

```text
photoReference
expectedName
expectedCategory
expectedPhotoReference
ownerId
physical path
original filename
```

---

#### submissionTokenとsnapshot

サーバー側でtokenにsnapshotを紐付けます。

snapshot：

```text
workId
name
category
photoReference
```

---

#### 競合判定

Serviceで現在DBの、

```text
name
category
photoReference
```

とsnapshotを比較します。

競合時：

```text
BasicInfoConflictException
↓
303
↓
GET /works/{id}
↓
最新DB状態
```

stale入力は再適用しません。

---

#### Validation error再表示時のsnapshot

通常の入力エラー再表示時は、競合基準を最新DB状態へ勝手に差し替えません。

編集開始時のsnapshotとの関係を維持します。

同じtokenを継続するか、新tokenへ同snapshotを紐付けるかは実装時調整可能です。

---

### CounterUpdateForm

#### フィールド

```java
private CounterType counterType;
private Integer value;
private Integer expectedValue;
```

---

#### counterType

```java
@NotNull(message = "カウンターの種類が正しくありません。")
private CounterType counterType;
```

許可：

```text
STITCH
ROW
```

---

#### value

```java
@NotNull(message = "0～9999の整数で入力してください。")
@Min(value = 0, message = "0～9999の整数で入力してください。")
@Max(value = 9999, message = "0～9999の整数で入力してください。")
private Integer value;
```

直接入力による任意の値変更を許可します。

`±1`制限はUIボタンだけの仕様です。

---

#### expectedValue

```java
@NotNull(message = "保存状態が正しくありません。画面を再読み込みしてください。")
@Min(value = 0, message = "保存状態が正しくありません。画面を再読み込みしてください。")
@Max(value = 9999, message = "保存状態が正しくありません。画面を再読み込みしてください。")
private Integer expectedValue;
```

---

#### 競合

```text
expectedValue形式・範囲不正
→ HTTP 400 VALIDATION_ERROR

expectedValue形式正常
DB現在値と不一致
→ HTTP 409 CONFLICT
```

---

#### 同値再送

```text
expectedValue == DB
AND
value == DB
```

なら正常終了します。

DB UPDATEおよび`last_updated_at`更新は行いません。

---

### MemoUpdateForm

#### フィールド

```java
private String memo;
private String expectedMemo;
```

---

#### memo

```java
@NotNull(message = "制作メモを正しく入力してください。")
@CodePointLength(
    max = 1000,
    message = "制作メモは1000文字以内で入力してください。"
)
private String memo;
```

空文字`""`はvalidです。

nullは不正リクエストです。

---

#### expectedMemo

```java
@NotNull(message = "保存状態が正しくありません。画面を再読み込みしてください。")
@CodePointLength(
    max = 1000,
    message = "保存状態が正しくありません。画面を再読み込みしてください。"
)
private String expectedMemo;
```

空文字`""`はvalidです。

---

#### 保存値

memo / expectedMemoともtrim等を行いません。

完全一致で比較します。

---

#### 競合

```text
expectedMemo形式正常
DB現在memoと不一致
→ HTTP 409 CONFLICT
```

---

#### 空文字への更新

```text
memo = ""
```

は制作メモ削除として正常です。

DBへNULLは保存しません。

---

#### 同値再送

```text
expectedMemo == DB
AND
memo == DB
```

なら正常終了し、`last_updated_at`を更新しません。

---

### StatusUpdateForm

#### フィールド

```java
private WorkStatus status;
private WorkStatus expectedStatus;
private LocalDate expectedCompletionDate;
```

---

#### status

```java
@NotNull(message = "制作ステータスを選択してください。")
private WorkStatus status;
```

許可：

```text
IN_PROGRESS
COMPLETED
```

---

#### expectedStatus

```java
@NotNull(message = "保存状態が正しくありません。画面を再読み込みしてください。")
private WorkStatus expectedStatus;
```

---

#### expectedCompletionDate

```java
@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
private LocalDate expectedCompletionDate;
```

nullを許可します。

---

#### 競合

Serviceで、

```text
expectedStatus
expectedCompletionDate
```

をセットでDB現在値と比較します。

不一致：

```text
HTTP 409
{"code":"CONFLICT"}
```

---

#### IN_PROGRESS → COMPLETED

Serviceで、

```text
status = COMPLETED
completionDate = LocalDate.now(clock)
lastUpdatedAt更新
```

を同一トランザクションで実行します。

---

#### COMPLETED → IN_PROGRESS

```text
status = IN_PROGRESS
completionDate = null
lastUpdatedAt更新
```

とします。

---

#### 再完成

再び`COMPLETED`へ変更した場合、以前の完成日を復元せず、再完成日を新しい完成日とします。

---

#### 同値再送

現在statusと同じstatusを再送した場合、expected値が一致していれば正常終了します。

DB UPDATEおよび`last_updated_at`更新は行いません。

---

### CompletionDateUpdateForm

#### フィールド

```java
private LocalDate completionDate;
private WorkStatus expectedStatus;
private LocalDate expectedCompletionDate;
```

---

#### completionDate

```java
@NotNull(message = "完成日を入力してください。")
@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
private LocalDate completionDate;
```

---

#### expectedStatus

```java
@NotNull(message = "保存状態が正しくありません。画面を再読み込みしてください。")
private WorkStatus expectedStatus;
```

---

#### expectedCompletionDate

```java
@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
private LocalDate expectedCompletionDate;
```

nullを許可します。

---

#### Service業務Validation

Serviceで以下を確認します。

```text
expectedStatus / expectedCompletionDateの競合
↓
現在statusがCOMPLETEDか
↓
completionDateが未来日ではないか
↓
同値か
↓
更新
```

---

#### 未来日

未来日：

```text
HTTP 400
VALIDATION_ERROR
fieldErrors.completionDate
```

メッセージ：

```text
未来の日付は指定できません。
```

---

#### 制作中作品への完成日設定

```text
HTTP 400
VALIDATION_ERROR
fieldErrors.completionDate
```

メッセージ：

```text
制作中の作品には完成日を設定できません。
```

`fieldErrors.status`は使用しません。

---

#### 同値再送

同じ完成日の再送は正常終了します。

---

#### lastUpdatedAt

完成日の手動変更だけでは`last_updated_at`を更新しません。

---

### S-03ログイン入力

#### LoginForm

専用`LoginForm`は作成しません。

---

#### URL

```text
GET /login
→ MVC Controller + Thymeleaf

POST /login
→ Spring Security
```

---

#### username

Spring Securityの`loadUserByUsername()`等の入口で、

```text
strip()
↓
4～20文字
↓
半角英数字
↓
IgnoreCase検索
```

とします。

形式不正ならDB検索せず通常の認証失敗とします。

---

#### password

ログイン時のpasswordは一切加工しません。

登録時のpassword policyやBlocklistをログイン時に再実行しません。

---

#### 認証失敗

以下を区別しません。

```text
username不存在
password不一致
username形式不正
```

外部メッセージ：

```text
ユーザー名またはパスワードが正しくありません。
```

---

#### ログイン失敗時

保持しません。

```text
username
password
remember-me
```

remember-meはOFFへ戻します。

---

#### autocomplete

```text
username → username
password → current-password
```

---

#### 成功・失敗

```text
成功
→ /

失敗
→ /login?error
```

---

#### CSRF

`POST /login`もCSRF保護対象です。

---

### BindingResult・Validation実行順

#### 基本順序

```text
HTTP Request
↓
Form Binding / 型変換
↓
Bean Validation
↓
BindingResult確認
├─ errorあり → Serviceを呼ばない
└─ errorなし → Service
```

---

#### Controller引数

Formの直後に`BindingResult`を置きます。

例：

```java
public String create(
        @Valid @ModelAttribute("workCreateForm") WorkCreateForm form,
        BindingResult bindingResult,
        ...) {
```

---

#### 型変換エラー

以下も入力エラーです。

```text
Integerへabc
Integerへ1.5
BigDecimalへ非数値
不正Enum
不正LocalDate
```

500にはしません。

---

### 通常HTML Formのエラー処理

対象：

```text
S-02
S-04
S-05
S-08
S-11基本情報
```

入力エラー時：

```text
Serviceを呼ばない
↓
redirectしない
↓
同画面を再表示
↓
HTTP 200
```

PRGは成功時に使用します。

---

### S-11非同期Validation

対象：

```text
CounterUpdateForm
MemoUpdateForm
StatusUpdateForm
CompletionDateUpdateForm
```

入力エラー時：

```text
Serviceを呼ばない
DB更新なし
lastUpdatedAt更新なし
HTTP 400
application/json
```

形式：

```json
{
  "code": "VALIDATION_ERROR",
  "fieldErrors": {
    "value": "0～9999の整数で入力してください。"
  }
}
```

---

### 1フィールド1メッセージ

同一フィールドに複数Validation errorが発生しても、ユーザーへ表示するのは原則1件とします。

優先順位：

```text
1. Binding / typeMismatch
2. 必須
3. 範囲・長さ・形式
4. cross-field
```

---

### typeMismatchメッセージ

Spring標準の内部エラーメッセージを表示しません。

`messages.properties`等でユーザー向け日本語へ置き換えます。

例：

```properties
typeMismatch.counterUpdateForm.value=0～9999の整数で入力してください。
typeMismatch.gaugeAdjustmentForm.originalStitchCount=元の目数は1～9999の整数で入力してください。
typeMismatch.completionDateUpdateForm.completionDate=正しい日付を入力してください。
```

---

### Service業務Validationの返し方

#### 原則

Serviceは画面field名・日本語UIメッセージ・HTTP statusを直接持ちません。

machine-readableなReasonを持つドメイン例外を使用し、Web層が画面・JSON表現へ変換します。

---

#### RegistrationValidationException

登録用として1クラスを使用します。

```text
RegistrationValidationException
└ Reason
   ├ USERNAME_DUPLICATE
   ├ EMAIL_DUPLICATE
   └ PASSWORD_BLOCKED
```

個別に以下は作りません。

```text
DuplicateUsernameException
DuplicateEmailException
BlockedPasswordException
```

---

#### Reasonからfieldへの変換

| Reason | field | メッセージ |
| --- | --- | --- |
| USERNAME_DUPLICATE | username | このユーザー名はすでに使用されています。 |
| EMAIL_DUPLICATE | email | このメールアドレスはすでに登録されています。 |
| PASSWORD_BLOCKED | password | このパスワードは推測されやすいため使用できません。別のパスワードを設定してください。 |

---

#### DB UNIQUE競合

事前重複チェック後の同時登録によるDB UNIQUE違反は、原因がusername/emailだと明確に判別できる場合のみ通常のfield errorへ変換します。

原因不明のDB例外を重複扱いにしません。

---

#### Validationへ変換しない例外

```text
AutoSaveConflictException
→ HTTP 409

BasicInfoConflictException
→ 303 PRG

WorkNotFoundException
→ 404

システム障害
→ 5xx
```

---

### 入力エラー時の値保持

#### 基本方針

ユーザーが修正に必要な通常入力値は保持します。

password・アップロードファイル等は保持しません。

---

#### S-02

保持：

```text
email
username
```

クリア：

```text
password
passwordConfirmation
```

---

#### S-04

6入力値を可能な限り保持します。

Binding不能のRejected Valueも画面上で保持します。

---

#### S-05

modeと6入力値を保持します。

非選択モード側の値も保持します。

---

#### S-08

保持：

```text
name
category
status
有効なsubmissionToken
```

保持しない：

```text
photo
```

photoエラー後のプレビューは初期画像へ戻します。

---

#### S-11基本情報

保持：

```text
name
category
原則deletePhoto
有効なsubmissionToken
```

保持しない：

```text
新しいphoto
```

---

#### deletePhoto=true＋他項目エラー

deletePhotoは保持します。

プレビューは初期画像状態を維持します。

---

#### photoあり＋deletePhoto=true

矛盾エラー後：

```text
photo → 破棄
deletePhoto → false
現在DBの写真を表示
```

とします。

---

#### S-11基本情報競合

競合時はstale入力を保持しません。

```text
303
↓
GET /works/{id}
↓
最新DB状態
↓
新submissionToken
↓
新snapshot
```

から再編集します。

---

#### S-11自動保存400

画面入力値をDB保存済み値へ自動的に戻しません。

例：

```text
DB = 500
画面 = 10000

400後
DB = 500のまま
画面 = 10000のまま
```

ユーザーが修正後、通常の自動保存へ復帰します。

---

### submissionToken

#### 通常入力エラー

Bean Validation・Binding・画像入力Validation等で保存処理が成立しなかった場合、submissionTokenを消費しません。

---

#### 正常保存

状態変更が成立したtokenは再利用不可です。

---

#### token自体が欠落・不正

token自体が欠落・不正な場合、現在tokenを再利用しません。

##### S-08

通常入力値を保持し、新tokenを発行して再表示可能です。

##### S-11基本情報

古い編集値を新snapshotへ引き継ぎません。

```text
最新DB状態
+
新submissionToken
+
新snapshot
```

から再編集します。

---

### 画像Validation連携

画像内容Validationは既存画像設計を正とします。

Form側では画像内容をBean Validation Annotationへ移しません。

画像入力エラーは`photo` fieldへ紐づけます。

---

#### 確定画像エラーメッセージ

##### 空・読み込み不能

```text
画像ファイルを正常に読み込めません。別の画像を選択してください。
```

##### 10MB超過

```text
画像は10MB以下のファイルを選択してください。
```

##### 非対応形式・形式不一致

```text
JPEGまたはPNG形式の画像を選択してください。
```

##### 8192px超過

```text
画像の幅と高さは8192px以下にしてください。
```

##### 壊れた画像・decode failure

```text
画像ファイルを正常に読み込めません。別のJPEGまたはPNG画像を選択してください。
```

1画像で複数エラーがある場合、最初に検出した1件のみ表示します。

システム障害は画像入力エラーとして表示しません。

---

### エラーメッセージ一覧

#### UserRegistrationForm

| 条件 | メッセージ |
| --- | --- |
| email未入力 | メールアドレスを入力してください。 |
| email 255文字超 | メールアドレスは255文字以内で入力してください。 |
| email形式不正 | 正しいメールアドレス形式で入力してください。 |
| email重複 | このメールアドレスはすでに登録されています。 |
| username未入力 | ユーザー名を入力してください。 |
| username長さ・形式不正 | ユーザー名は4～20文字の半角英数字で入力してください。 |
| username重複 | このユーザー名はすでに使用されています。 |
| password長さ不正 | パスワードは8～64文字で入力してください。 |
| password文字種不正 | パスワードは半角の英数字・記号・スペースで入力してください。 |
| Blocklist | このパスワードは推測されやすいため使用できません。別のパスワードを設定してください。 |
| confirmation未入力 | 確認用パスワードを入力してください。 |
| password不一致 | パスワードが一致していません。 |

---

#### GaugeAdjustmentForm

##### 指定目ゲージ

```text
指定目ゲージを入力してください。
指定目ゲージは数値で入力してください。
指定目ゲージは0.01～999.99の範囲で入力してください。
指定目ゲージは小数第2位までで入力してください。
```

##### 実測目ゲージ

```text
実測目ゲージを入力してください。
実測目ゲージは数値で入力してください。
実測目ゲージは0.01～999.99の範囲で入力してください。
実測目ゲージは小数第2位までで入力してください。
```

##### 元の目数

```text
元の目数を入力してください。
元の目数は1～9999の整数で入力してください。
```

##### 指定段ゲージ

```text
指定段ゲージを入力してください。
指定段ゲージは数値で入力してください。
指定段ゲージは0.01～999.99の範囲で入力してください。
指定段ゲージは小数第2位までで入力してください。
```

##### 実測段ゲージ

```text
実測段ゲージを入力してください。
実測段ゲージは数値で入力してください。
実測段ゲージは0.01～999.99の範囲で入力してください。
実測段ゲージは小数第2位までで入力してください。
```

##### 元の段数

```text
元の段数を入力してください。
元の段数は1～9999の整数で入力してください。
```

---

#### YarnCombinationForm

##### mode

```text
計算モードを選択してください。
計算モードが正しくありません。
```

##### 2本取り

```text
重量を入力してください。
重量は数値で入力してください。
重量は0より大きい値を入力してください。
重量は99999.99以下で入力してください。
重量は小数第2位までで入力してください。

長さを入力してください。
長さは数値で入力してください。
長さは0より大きい値を入力してください。
長さは99999.99以下で入力してください。
長さは小数第2位までで入力してください。
```

##### 引き揃え

糸1・糸2それぞれ、

```text
糸1の重量を入力してください。
糸1の重量は数値で入力してください。
糸1の重量は0より大きい値を入力してください。
糸1の重量は99999.99以下で入力してください。
糸1の重量は小数第2位までで入力してください。
```

の形式とし、長さ・糸2も同様です。

---

#### WorkCreateForm / WorkBasicInfoForm

```text
作品名を入力してください。
作品名は50文字以内で入力してください。
カテゴリーを選択してください。
制作ステータスを選択してください。
写真の変更と削除を同時に行うことはできません。
```

---

#### CounterUpdateForm

```text
カウンターの種類が正しくありません。
0～9999の整数で入力してください。
保存状態が正しくありません。画面を再読み込みしてください。
```

---

#### MemoUpdateForm

```text
制作メモを正しく入力してください。
制作メモは1000文字以内で入力してください。
保存状態が正しくありません。画面を再読み込みしてください。
```

---

#### StatusUpdateForm

```text
制作ステータスを選択してください。
制作ステータスが正しくありません。
保存状態が正しくありません。画面を再読み込みしてください。
```

---

#### CompletionDateUpdateForm

```text
完成日を入力してください。
正しい日付を入力してください。
未来の日付は指定できません。
制作中の作品には完成日を設定できません。
保存状態が正しくありません。画面を再読み込みしてください。
```

---

#### submissionToken

hidden fieldの直下には表示せず、フォーム共通位置へ表示します。

```text
画面の情報が古くなっています。ページを再読み込みして、もう一度操作してください。
```

---

---

## 10. Controller・Service設計

### 基本方針

Knittingの基本的な処理フローは以下とします。

```text
Thymeleaf
↓
Form
↓
Controller
↓
Service
↓
Repository
↓
Entity
↓
MySQL
```

表示時は必要に応じてServiceでEntityをDTOへ変換し、Controllerを介してThymeleafへ渡します。

ControllerからRepositoryを直接呼びません。

ServiceはFormそのものを受け取りません。ControllerがFormから必要な型付き値を取り出してServiceへ渡します。

DTOは必要な箇所のみ使用し、機械的な1対1 DTO化は行いません。

---

### Controllerクラス構成

初期版では以下のControllerを使用します。

```text
HomeController
UserRegistrationController
LoginController
GaugeAdjustmentController
YarnCombinationController
CounterController
YarnGuideController
WorkController
WorkAutoSaveController
```

#### HomeController

担当：

- S-01トップ画面
- ログイン状態に応じた表示切替
- ログイン済みの場合、制作中作品の最新4件表示

依存：

```text
WorkService
```

#### UserRegistrationController

担当：

- S-02ユーザー登録画面
- ユーザー登録POST
- Bean Validationエラーの同画面再表示
- 登録業務Validation例外のfield error化
- ログイン済みユーザーの登録画面アクセス抑止

依存：

```text
UserRegistrationService
```

#### LoginController

担当：

- S-03ログイン画面GETのみ
- ログイン済みユーザーのログイン画面アクセス抑止

POST `/login` はSpring Securityが処理します。

LoginServiceは作成しません。

#### GaugeAdjustmentController

担当：

- S-04ゲージ・目数・段数調整
- GET入力受付
- Validation
- 計算結果表示

依存：

```text
GaugeAdjustmentService
```

#### YarnCombinationController

担当：

- S-05 2本取り・引き揃え計算
- GET入力受付
- `calculate=true` による初期表示と計算要求の区別
- Validation
- 計算結果表示

依存：

```text
YarnCombinationService
```

#### CounterController

担当：

- S-06簡易カウンター画面GET

S-06はJavaScript + localStorageで完結するためServiceは作成しません。

#### YarnGuideController

担当：

- S-07毛糸・推奨針号数対応表画面GET

初期版では専用Serviceは作成しません。

#### WorkController

担当：

- S-08作品登録
- S-09制作中一覧
- S-10完成一覧
- S-11作品詳細
- S-11基本情報保存
- 作品画像取得
- 作品削除

依存：

```text
WorkService
SubmissionTokenManager
```

`SubmissionTokenManager`はS-08 GET時のsubmissionToken発行等で使用します。

#### WorkAutoSaveController

担当：

- S-11目数・段数自動保存
- S-11制作メモ自動保存
- S-11制作ステータス自動保存
- S-11完成日自動保存

`@RestController` とします。

依存：

```text
WorkAutoSaveService
```

---

### Serviceクラス構成

初期版では以下のServiceを使用します。

```text
UserRegistrationService
GaugeAdjustmentService
YarnCombinationService
WorkService
WorkAutoSaveService
```

画面ごとに機械的にServiceを作る構成にはしません。

#### UserRegistrationService

担当：

- username重複確認
- email重複確認
- Password Blocklist確認
- password hash化
- User生成
- createdAt設定
- User保存

#### GaugeAdjustmentService

担当：

- 目数調整計算
- 段数調整計算
- 計算結果DTO生成

#### YarnCombinationService

担当：

- 2本取り計算
- 引き揃え計算
- 100g換算糸長算出
- 太さ区分・推奨針情報取得
- 結果DTO生成

#### WorkService

担当：

- 作品登録
- 制作中一覧取得
- 完成一覧取得
- 作品詳細取得
- Validation再表示用作品詳細取得
- 作品画像取得
- 基本情報更新
- 作品削除
- owner確認
- submissionToken / snapshot連携
- 画像保存処理のオーケストレーション
- TransactionTemplateによるDB transaction境界制御
- DTO変換
- lastUpdatedAt / completionDate制御

#### WorkAutoSaveService

担当：

- 目数・段数保存
- メモ保存
- ステータス保存
- 完成日保存
- owner確認
- PESSIMISTIC_WRITE
- expected値競合判定
- 業務Validation
- lastUpdatedAt制御
- 自動保存Result DTO生成

---

### Controllerメソッド構成

Controllerのpublicメソッドは、原則として1 endpoint / 1 use case = 1 methodとします。

#### HomeController

```java
showHome(...)
```

#### UserRegistrationController

```java
showRegistrationForm(...)
registerUser(...)
```

#### LoginController

```java
showLogin(...)
```

#### GaugeAdjustmentController

```java
showGaugeAdjustment(...)
```

#### YarnCombinationController

```java
showYarnCombination(...)
```

#### CounterController

```java
showCounter(...)
```

#### YarnGuideController

```java
showYarnGuide(...)
```

#### WorkController

```java
showCreateForm(...)
createWork(...)
showInProgressWorks(...)
showCompletedWorks(...)
showWorkDetail(...)
updateBasicInfo(...)
getWorkImage(...)
deleteWork(...)
```

#### WorkAutoSaveController

```java
updateCounter(...)
updateMemo(...)
updateStatus(...)
updateCompletionDate(...)
```

---

### Service publicメソッド構成

#### UserRegistrationService

```java
public void registerUser(
        String email,
        String username,
        String password
)
```

FormやpasswordConfirmationはServiceへ渡しません。

#### GaugeAdjustmentService

```java
public GaugeAdjustmentResult calculateAdjustment(
        BigDecimal specifiedStitchGauge,
        BigDecimal measuredStitchGauge,
        Integer originalStitchCount,
        BigDecimal specifiedRowGauge,
        BigDecimal measuredRowGauge,
        Integer originalRowCount
)
```

#### YarnCombinationService

```java
public YarnCombinationResult calculateCombination(
        YarnCombinationMode mode,
        BigDecimal doubleStrandWeight,
        BigDecimal doubleStrandLength,
        BigDecimal yarn1Weight,
        BigDecimal yarn1Length,
        BigDecimal yarn2Weight,
        BigDecimal yarn2Length
)
```

#### WorkService

```java
public Long createWork(
        Long userId,
        String name,
        MultipartFile photo,
        WorkCategory category,
        WorkStatus status,
        String submissionToken
)
```

```java
public Page<InProgressWorkSummary> getInProgressWorks(
        Long userId,
        WorkCategory category,
        int pageIndex,
        int pageSize
)
```

```java
public Page<CompletedWorkSummary> getCompletedWorks(
        Long userId,
        WorkCategory category,
        int pageIndex
)
```

```java
public WorkDetailResult getWorkDetail(
        Long userId,
        Long workId
)
```

```java
public WorkDetailResult getWorkDetailForRedisplay(
        Long userId,
        Long workId,
        String existingSubmissionToken
)
```

```java
public WorkImageResult getWorkImage(
        Long userId,
        Long workId
)
```

```java
public void updateBasicInfo(
        Long userId,
        Long workId,
        String name,
        MultipartFile photo,
        WorkCategory category,
        boolean deletePhoto,
        String submissionToken
)
```

```java
public void deleteWork(
        Long userId,
        Long workId
)
```

#### WorkAutoSaveService

```java
public CounterSaveResult updateCounter(
        Long userId,
        Long workId,
        CounterType counterType,
        int value,
        int expectedValue
)
```

```java
public void updateMemo(
        Long userId,
        Long workId,
        String memo,
        String expectedMemo
)
```

```java
public StatusSaveResult updateStatus(
        Long userId,
        Long workId,
        WorkStatus status,
        WorkStatus expectedStatus,
        LocalDate expectedCompletionDate
)
```

```java
public CompletionDateSaveResult updateCompletionDate(
        Long userId,
        Long workId,
        LocalDate completionDate,
        WorkStatus expectedStatus,
        LocalDate expectedCompletionDate
)
```

---

### Result DTO

#### GaugeAdjustmentResult

```java
public record GaugeAdjustmentResult(
        BigDecimal stitchCalculatedValue,
        Integer adjustedStitchCount,
        BigDecimal rowCalculatedValue,
        Integer adjustedRowCount
) {}
```

片側のみ計算した場合、未計算側はnullを許可します。

#### YarnCombinationResult

```java
public record YarnCombinationResult(
        long lengthPer100g,
        String thicknessLabel,
        String knittingNeedleLabel,
        String crochetHookLabel
) {}
```

#### InProgressWorkSummary

```java
public record InProgressWorkSummary(
        Long id,
        String name,
        int stitchCount,
        int rowCount,
        boolean hasPhoto
) {}
```

#### CompletedWorkSummary

```java
public record CompletedWorkSummary(
        Long id,
        String name,
        LocalDate completionDate,
        boolean hasPhoto
) {}
```

#### WorkDetailResult

```java
public record WorkDetailResult(
        Long id,
        String name,
        WorkCategory category,
        int stitchCount,
        int rowCount,
        String memo,
        WorkStatus status,
        LocalDate completionDate,
        String basicInfoSubmissionToken,
        boolean hasPhoto
) {}
```

#### WorkImageResult

```java
public record WorkImageResult(
        Resource resource,
        String contentType
) {}
```

HTTP responseそのものはServiceへ持ち込みません。

#### CounterSaveResult

```java
public record CounterSaveResult(
        CounterType counterType,
        int value
) {}
```

#### StatusSaveResult

```java
public record StatusSaveResult(
        WorkStatus status,
        LocalDate completionDate
) {}
```

#### CompletionDateSaveResult

```java
public record CompletionDateSaveResult(
        LocalDate completionDate
) {}
```

#### ErrorResponse

HTTPエラー表現用DTOとして1種類だけ採用します。

```java
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String code,
        Map<String, String> fieldErrors
) {}
```

Serviceでは生成せず、Controller / Adviceで生成します。

---

### DTOを作らないもの

初期版では以下のようなDTOを機械的に追加しません。

```text
UserRegistrationDto
WorkCreateDto
WorkUpdateDto
WorkDeleteResult
MemoSaveResult
UserDto
WorkDto
```

BasicInfoSnapshotはController用DTOではなく、submissionToken管理の内部状態として扱います。

---

### Controller Mapping最終構成

#### HomeController

```java
@GetMapping("/")
public String showHome(
        @AuthenticationPrincipal KnittingUserPrincipal principal,
        Model model
)
```

未ログイン時はWorkServiceを呼びません。

ログイン済み時：

```java
workService.getInProgressWorks(
        principal.getUserId(),
        null,
        0,
        4
);
```

Model：

```text
inProgressWorks
```

---

### UserRegistrationController

#### GET `/register`

```java
@GetMapping("/register")
public String showRegistrationForm(
        @AuthenticationPrincipal KnittingUserPrincipal principal,
        Model model
)
```

ログイン済みなら：

```text
redirect:/
```

未ログインなら：

```text
register
```

Model：

```text
userRegistrationForm
```

#### POST `/register`

```java
@PostMapping("/register")
public String registerUser(
        @Valid
        @ModelAttribute("userRegistrationForm")
        UserRegistrationForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal KnittingUserPrincipal principal
)
```

`BindingResult`はForm直後に置きます。

ログイン済みなら登録処理を行わず：

```text
redirect:/
```

CSRF不正はSpring Security Filterが先に処理するため403を優先します。

Bean Validationエラー：

```text
Service未呼出
同じregister view
HTTP 200
```

Service成功：

```text
redirect:/login?registered
```

---

### LoginController

#### GET `/login`

```java
@GetMapping("/login")
public String showLogin(
        @AuthenticationPrincipal KnittingUserPrincipal principal
)
```

ログイン済み：

```text
redirect:/
```

未ログイン：

```text
login
```

POST `/login`はSpring Securityが処理します。

---

### GaugeAdjustmentController

```java
@GetMapping("/gauge-adjustment")
public String showGaugeAdjustment(
        @Valid
        @ModelAttribute("gaugeAdjustmentForm")
        GaugeAdjustmentForm form,
        BindingResult bindingResult,
        Model model
)
```

全6項目null：

```text
初期表示
Service未呼出
```

Validationエラー：

```text
同じ画面
HTTP 200
Service未呼出
```

計算成功：

```text
adjustmentResult
```

をModelへ追加します。

View：

```text
gauge-adjustment
```

---

### YarnCombinationController

```java
@GetMapping("/yarn-combination")
public String showYarnCombination(
        @RequestParam(
            name = "calculate",
            defaultValue = "false"
        )
        boolean calculate,
        @Valid
        @ModelAttribute("yarnCombinationForm")
        YarnCombinationForm form,
        BindingResult bindingResult,
        Model model
)
```

`calculate=false`：

```text
初期表示
Service未呼出
```

`calculate=true` + Validationエラー：

```text
同じ画面
HTTP 200
Service未呼出
```

`calculate=true` + 正常：

```text
combinationResult
```

をModelへ追加します。

View：

```text
yarn-combination
```

---

### CounterController

```java
@GetMapping("/counter")
public String showCounter()
```

View：

```text
counter
```

Serviceは使用しません。

---

### YarnGuideController

```java
@GetMapping("/yarn-guide")
public String showYarnGuide(
        Model model
)
```

View：

```text
yarn-guide
```

初期版では専用Serviceは使用しません。

---

### WorkController

クラスレベル：

```java
@Controller
@RequestMapping("/works")
```

#### GET `/works/new`

```java
@GetMapping("/new")
public String showCreateForm(
        Model model
)
```

新規Form：

```text
status = IN_PROGRESS
submissionToken = SubmissionTokenManager.issueWorkCreateToken()
```

Model：

```text
workCreateForm
categories
workStatuses
```

View：

```text
works/new
```

#### POST `/works`

```java
@PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
public String createWork(
        @Valid
        @ModelAttribute("workCreateForm")
        WorkCreateForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal
        KnittingUserPrincipal principal,
        Model model
)
```

Validationエラー：

```text
同じworks/new
HTTP 200
Service未呼出
通常入力値を保持
photoは保持しない
同じsubmissionTokenを維持
```

成功：

```text
redirect:/works/{id}
```

#### GET `/works/in-progress`

```java
@GetMapping("/in-progress")
public String showInProgressWorks(
        @RequestParam(
            name = "category",
            required = false
        )
        WorkCategory category,
        @RequestParam(
            name = "page",
            defaultValue = "1"
        )
        int page,
        @AuthenticationPrincipal
        KnittingUserPrincipal principal,
        Model model
)
```

Controllerで：

```text
page < 1
→ 400

pageIndex = page - 1
```

Service：

```text
pageSize = 10
```

Model：

```text
worksPage
categories
selectedCategory
```

View：

```text
works/in-progress
```

#### GET `/works/completed`

S-09と同様です。

```java
@GetMapping("/completed")
public String showCompletedWorks(
        @RequestParam(
            name = "category",
            required = false
        )
        WorkCategory category,
        @RequestParam(
            name = "page",
            defaultValue = "1"
        )
        int page,
        @AuthenticationPrincipal
        KnittingUserPrincipal principal,
        Model model
)
```

Model：

```text
worksPage
categories
selectedCategory
```

View：

```text
works/completed
```

#### GET `/works/{id}`

```java
@GetMapping("/{id}")
public String showWorkDetail(
        @PathVariable("id")
        Long workId,
        @RequestParam(name = "from", required = false)
        String from,
        @AuthenticationPrincipal
        KnittingUserPrincipal principal,
        Model model
)
```

正常GETでは`getWorkDetail()`が新しいsubmissionToken + snapshotを発行します。

Model：

```text
work
workBasicInfoForm
categories
workStatuses
```

View：

```text
works/detail
```

#### POST `/works/{id}/basic-info`

```java
@PostMapping(
        value = "/{id}/basic-info",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
public String updateBasicInfo(
        @PathVariable("id")
        Long workId,
        @Valid
        @ModelAttribute("workBasicInfoForm")
        WorkBasicInfoForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal
        KnittingUserPrincipal principal,
        Model model,
        RedirectAttributes redirectAttributes
)
```

`deletePhoto`はControllerで：

```java
boolean deletePhoto =
        Boolean.TRUE.equals(form.getDeletePhoto());
```

通常Validationエラー：

```text
同じworks/detail
HTTP 200
Service未呼出
bound formを置き換えない
同じsubmissionToken / snapshotを維持
```

詳細表示用データのみ：

```java
workService.getWorkDetailForRedisplay(
        userId,
        workId,
        existingSubmissionToken
);
```

から取得します。

成功：

```text
redirect:/works/{id}
```

有効な`from`がある場合は、redirect先のGETへ同じ`from`を引き継ぐ。不正な`from`は引き継がない。

#### GET `/works/{id}/image`

```java
@GetMapping("/{id}/image")
public ResponseEntity<Resource> getWorkImage(
        @PathVariable("id")
        Long workId,
        @AuthenticationPrincipal
        KnittingUserPrincipal principal
)
```

Service：

```java
WorkImageResult getWorkImage(...)
```

ControllerがHTTP Headerを付与します。

主なHeader：

```text
Content-Type: image/jpeg または image/png
Content-Disposition: inline
Cache-Control: private, no-store
X-Content-Type-Options: nosniff
Cross-Origin-Resource-Policy: same-origin
```

内部photoReferenceやfilenameをHTTPへ露出しません。

#### POST `/works/{id}/delete`

```java
@PostMapping("/{id}/delete")
public String deleteWork(
        @PathVariable("id")
        Long workId,
        @AuthenticationPrincipal
        KnittingUserPrincipal principal
)
```

成功：

```text
redirect:/
```

---

### WorkAutoSaveController

クラスレベル：

```java
@RestController
@RequestMapping("/works")
```

戻り値は4メソッドとも：

```java
ResponseEntity<?>
```

とします。

これにより成功Result DTOとErrorResponseの両方を返せます。

#### Counter

```java
@PostMapping("/{id}/counter")
public ResponseEntity<?> updateCounter(
        @PathVariable("id")
        Long workId,
        @Valid
        @ModelAttribute("counterUpdateForm")
        CounterUpdateForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal
        KnittingUserPrincipal principal
)
```

#### Memo

```java
@PostMapping("/{id}/memo")
public ResponseEntity<?> updateMemo(
        @PathVariable("id")
        Long workId,
        @Valid
        @ModelAttribute("memoUpdateForm")
        MemoUpdateForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal
        KnittingUserPrincipal principal
)
```

成功時：

```text
HTTP 200
bodyなし
```

#### Status

```java
@PostMapping("/{id}/status")
public ResponseEntity<?> updateStatus(
        @PathVariable("id")
        Long workId,
        @Valid
        @ModelAttribute("statusUpdateForm")
        StatusUpdateForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal
        KnittingUserPrincipal principal
)
```

#### Completion Date

```java
@PostMapping("/{id}/completion-date")
public ResponseEntity<?> updateCompletionDate(
        @PathVariable("id")
        Long workId,
        @Valid
        @ModelAttribute("completionDateUpdateForm")
        CompletionDateUpdateForm form,
        BindingResult bindingResult,
        @AuthenticationPrincipal
        KnittingUserPrincipal principal
)
```

Bean Validation / Binding error：

```text
HTTP 400
{
  "code": "VALIDATION_ERROR",
  "fieldErrors": {
    ...
  }
}
```

Service競合：

```text
HTTP 409
{
  "code": "CONFLICT"
}
```

---

### Model属性

#### S-01

```text
inProgressWorks
```

#### S-02

```text
userRegistrationForm
```

#### S-04

```text
gaugeAdjustmentForm
adjustmentResult
```

`adjustmentResult`は計算成功時のみ。

#### S-05

```text
yarnCombinationForm
combinationResult
```

`combinationResult`は計算成功時のみ。

#### S-08

```text
workCreateForm
categories
workStatuses
```

#### S-09

```text
worksPage
categories
selectedCategory
```

#### S-10

```text
worksPage
categories
selectedCategory
```

#### S-11

```text
work
workBasicInfoForm
categories
workStatuses
```

Pageのpage number / totalPages / hasNext等は`Page`自身を利用し、重複したModel属性を大量追加しません。

---

### Validationエラー再表示方針

#### 通常HTML Form

```text
Binding / Bean Validation error
↓
Service未呼出
↓
redirectしない
↓
同じview
↓
HTTP 200
```

Bound Formを新しいFormで置き換えません。

#### S-08

保持：

```text
name
category
status
submissionToken
```

保持しない：

```text
photo
```

#### S-11基本情報

通常Validationエラーでは：

```text
同じsubmissionToken
同じsnapshot
```

を維持します。

`getWorkDetail()`を使うと新token / snapshotが発行されるため使用しません。

```java
getWorkDetailForRedisplay(...)
```

を使用します。

bound `workBasicInfoForm`を置き換えません。

---

### 一覧ページ境界

URL上のpageは1始まりです。

```text
?page=1
?page=2
...
```

ServiceのpageIndexは0始まりです。

Controllerで：

```java
int pageIndex = page - 1;
```

とします。

#### `page < 1`

```text
400 Bad Request
```

Serviceは呼びません。

#### 大きすぎるpage

Service取得後にControllerで判定します。

```text
page > 1
かつ
page > worksPage.getTotalPages()
→ 400
```

0件 + page=1は正常表示します。

#### 不正category

`WorkCategory`へのSpring MVC Binding失敗として400です。

ServiceでString→Enum変換は行いません。

---

### 一覧Pageable / Sort

PageableはServiceで生成します。

Controllerでは`Pageable`や`Sort`を直接受け取りません。

ブラウザから`size`や`sort`を自由指定させません。

#### S-01 / S-09

```text
lastUpdatedAt DESC
id DESC
```

S-01：

```text
pageIndex = 0
pageSize = 4
```

S-09：

```text
pageSize = 10
```

#### S-10

```text
completionDate DESC
id DESC
pageSize = 10
```

---

### Repository最終構成

#### UserRepository

```java
public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(
            String username
    );

    boolean existsByUsernameIgnoreCase(
            String username
    );

    boolean existsByEmail(
            String email
    );
}
```

#### WorkRepository

```java
public interface WorkRepository
        extends JpaRepository<Work, Long> {

    Optional<Work> findByIdAndUser(
            Long id,
            User user
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Work> findByIdAndUserId(
            Long id,
            Long userId
    );

    Page<Work> findByUserAndStatus(
            User user,
            WorkStatus status,
            Pageable pageable
    );

    Page<Work> findByUserAndStatusAndCategory(
            User user,
            WorkStatus status,
            WorkCategory category,
            Pageable pageable
    );
}
```

通常取得：

```text
findByIdAndUser(...)
```

更新ロック取得：

```text
findByIdAndUserId(...)
+ PESSIMISTIC_WRITE
```

他人所有か実不存在かを調べるための追加`findById()`は行いません。

---

### owner取得private helper

WorkService：

```java
private User getAuthenticatedUser(Long userId)
```

```java
private Work getOwnedWork(
        Long workId,
        User user
)
```

```java
private Work getOwnedWorkForUpdate(
        Long workId,
        Long userId
)
```

`getAuthenticatedUser()`で認証済みuserIdに対応するUserが存在しない場合は内部不整合として`IllegalStateException`等の5xx系へ流します。

`getOwnedWork()` / `getOwnedWorkForUpdate()`で作品が取得できない場合：

```text
WorkNotFoundException
```

です。

実不存在と他人所有は外部上区別しません。

WorkAutoSaveServiceにも：

```java
private Work getOwnedWorkForUpdate(
        Long workId,
        Long userId
)
```

を持たせます。

数行の共通化のためにBase ServiceやAbstract Serviceは作成しません。

---

### Constructor Injection

依存BeanはConstructor Injectionとします。

```java
private final WorkService workService;

public WorkController(
        WorkService workService,
        SubmissionTokenManager submissionTokenManager
) {
    this.workService = workService;
    this.submissionTokenManager = submissionTokenManager;
}
```

ルール：

```text
private final
単一constructor
@Autowired省略
field injection不使用
setter injection不使用
Lombokによるconstructor生成は初期版では使わない
```

Controller → Service → Repository / componentの方向を維持します。

Controller → Repositoryは禁止します。

循環依存は作りません。

DIのためだけのService interfaceは作りません。

---

### 依存関係最終版

#### Controller

```text
HomeController
→ WorkService

UserRegistrationController
→ UserRegistrationService

LoginController
→ 依存Serviceなし

GaugeAdjustmentController
→ GaugeAdjustmentService

YarnCombinationController
→ YarnCombinationService

CounterController
→ 依存Serviceなし

YarnGuideController
→ 初期版では依存Serviceなし

WorkController
→ WorkService
→ SubmissionTokenManager

WorkAutoSaveController
→ WorkAutoSaveService
```

#### Service

```text
UserRegistrationService
→ UserRepository
→ PasswordEncoder
→ Clock
→ Password Blocklist担当Bean
```

```text
GaugeAdjustmentService
→ 初期版では外部依存なし
```

```text
YarnCombinationService
→ 初期版では外部依存なし
```

```text
WorkService
→ UserRepository
→ WorkRepository
→ Clock
→ 画像処理・ストレージ担当コンポーネント
→ SubmissionTokenManager
→ TransactionTemplate
```

```text
WorkAutoSaveService
→ WorkRepository
→ Clock
```

```text
KnittingUserDetailsService
→ UserRepository
```

WorkServiceからWorkAutoSaveServiceは呼びません。

---

### Transaction方針

#### UserRegistrationService

```text
registerUser
→ @Transactional
```

#### GaugeAdjustmentService

transactionなし。

#### YarnCombinationService

transactionなし。

#### WorkService 読み取り

```text
getInProgressWorks
getCompletedWorks
getWorkDetail
getWorkDetailForRedisplay
getWorkImage
```

は：

```java
@Transactional(readOnly = true)
```

を基本とします。

#### WorkService 更新

以下は画像ファイルI/OとDB transactionを適切に分離するため、メソッド全体へ`@Transactional`を付けず、内部で`TransactionTemplate`を使用します。

```text
createWork
updateBasicInfo
deleteWork
```

#### WorkAutoSaveService

以下は各publicメソッドを独立transactionとします。

```text
updateCounter
updateMemo
updateStatus
updateCompletionDate
```

`@Transactional`を使用します。

Springのannotationを使用し、原則REQUIRED / default isolationです。

機械的な`rollbackFor = Exception.class`は付けません。

---

### createWork transaction順序

```text
submissionToken begin
AVAILABLE → PROCESSING
↓
必要なら候補画像を検証・生成・保存
↓
TransactionTemplate
  User取得
  Work生成
  owner設定
  name設定
  category設定
  status設定
  stitchCount = 0
  rowCount = 0
  memo = ""
  completionDate設定
  photoReference設定
  lastUpdatedAt設定
  save
  commit
↓
submissionToken complete / remove
↓
workId返却
```

DB transaction失敗：

```text
rollback
candidate画像削除
tokenを再試行可能状態へ戻す
例外再送出
```

---

### updateBasicInfo transaction順序

```text
submissionToken begin
↓
snapshot取得
↓
Transaction A
  owner付きWork通常取得
  snapshot一次比較
  transaction終了
↓
必要ならcandidate画像処理
↓
Transaction B
  owner付きWork PESSIMISTIC_WRITE取得
  snapshot再比較
  実変更有無判定
  name/category/photoReference更新
  必要時lastUpdatedAt更新
  commit
↓
submissionToken complete
↓
必要なら旧画像削除
```

画像処理中にDB行ロックを保持しません。

snapshot比較対象：

```text
workId
name
category
photoReference
```

競合時：

```text
DB更新しない
candidate画像削除
BasicInfoConflictException
303
最新GET /works/{id}
新token
新snapshot
```

古い入力を自動再適用しません。

---

### deleteWork transaction順序

```text
TransactionTemplate
  owner付きPESSIMISTIC_WRITE取得
  oldPhotoReference保持
  Work削除
  commit
↓
oldPhotoReference != null
  → 旧画像削除
```

commit後の画像削除失敗はDB rollbackしません。

orphan候補としてログ対象とします。

default画像は削除しません。

---

### submissionToken / snapshot管理

#### SubmissionTokenManager

```java
@Component
@SessionScope
public class SubmissionTokenManager {
    ...
}
```

推奨配置：

```text
service/SubmissionTokenManager.java
```

独立した業務Serviceではなく、Service支援用Managerとして扱います。

HttpSessionをWorkServiceへ直接渡しません。

#### token生成

```java
UUID.randomUUID().toString()
```

CSRFとは別物です。

DB保存しません。

#### 用途区分

```text
WORK_CREATE
WORK_BASIC_INFO
```

を内部的に区別します。

別用途tokenの使い回しを禁止します。

#### BasicInfoSnapshot

内部状態：

```text
workId
name
category
photoReference
```

browserへhidden送信しません。

#### 状態

永続的なUSED状態は持ちません。

```text
AVAILABLE
PROCESSING
```

のみを基本とし、成功・無効時は削除します。

#### 状態遷移

```text
issue
→ AVAILABLE

begin
→ AVAILABLE → PROCESSING

success
→ remove

retry可能な失敗
→ PROCESSING → AVAILABLE

invalid / mismatch / conflict
→ invalidate / remove
```

#### 同時POST防止

Managerの短い状態変更操作は` synchronized `相当でatomicに処理し、同じSession・同じtokenで2リクエストが同時に処理開始できないようにします。

DB処理や画像処理の間、Managerのsynchronized lockを保持しません。

#### 概念API

```java
String issueWorkCreateToken();
```

```java
String issueBasicInfoToken(
        Long workId,
        String name,
        WorkCategory category,
        String photoReference
);
```

```java
void beginWorkCreate(
        String token
);
```

```java
BasicInfoSnapshot beginBasicInfo(
        String token,
        Long workId
);
```

```java
void complete(
        String token
);
```

```java
void releaseForRetry(
        String token
);
```

```java
void invalidate(
        String token
);
```

厳密なprivate内部実装は実装時調整可とします。

---

### S-08 submissionToken

GET `/works/new`：

```text
新token発行
```

Bean Validationエラー：

```text
Service未呼出
token AVAILABLEのまま
同じtokenを保持
```

Service到達後の再試行可能入力エラー：

```text
releaseForRetry
```

成功：

```text
complete / remove
```

token欠落・不正：

```text
旧tokenを使用しない
通常入力は保持
photoは保持しない
新tokenを発行
同画面再表示
```

---

### S-11 submissionToken

通常GET：

```text
最新DB基本情報
↓
新token
↓
新snapshot
```

通常Bean Validationエラー：

```text
同じtoken
同じsnapshot
```

を維持します。

`getWorkDetailForRedisplay()`は新token / snapshotを発行しません。

snapshot競合：

```text
BasicInfoConflictException
↓
旧token無効化
↓
303
↓
最新GET
↓
新token
新snapshot
```

token欠落・不存在・用途不一致・PROCESSING中・workId不一致等：

```text
InvalidSubmissionTokenException
↓
古い基本情報入力を保存しない
↓
最新GET
↓
新token
新snapshot
```

---

### 業務例外

初期版で使用する主な業務例外：

```text
WorkNotFoundException
AutoSaveConflictException
BasicInfoConflictException
InvalidCompletionDateException
RegistrationValidationException
InvalidSubmissionTokenException
ImageValidationException
```

抽象`BusinessException`基底クラスは作りません。

---

### RegistrationValidationException

Reason：

```text
USERNAME_DUPLICATE
EMAIL_DUPLICATE
PASSWORD_BLOCKED
```

Serviceは機械的Reasonだけを持ち、UI向け日本語文言やfield名を持ちません。

ControllerがBindingResultへ変換します。

例：

```text
USERNAME_DUPLICATE
→ username field error

EMAIL_DUPLICATE
→ email field error

PASSWORD_BLOCKED
→ password field error
```

登録失敗再表示では：

```text
email保持
username保持
password再出力なし
passwordConfirmation再出力なし
```

---

### ImageValidationException

画像入力に起因するValidation専用例外として使用します。

Reason例：

```text
EMPTY_OR_UNREADABLE
FILE_TOO_LARGE
UNSUPPORTED_FORMAT
DIMENSIONS_TOO_LARGE
DECODE_FAILED
```

Controllerでは：

```text
photo field error
```

へ変換します。

画像I/O failure、disk full、permission error等のシステムエラーとは区別します。

システム画像障害は5xxです。

---

### BasicInfoConflictException

用途をS-11基本情報の**DB現在値とsnapshotの競合専用**へ明確化します。

対象：

```text
name
category
photoReference
```

外部：

```text
HTTP 303 See Other
最新 /works/{id}
Flash Message
```

例外はredirect先判定のため：

```text
workId
```

を保持してよいものとします。

submissionToken自体の不正は`InvalidSubmissionTokenException`へ分離します。

---

### InvalidSubmissionTokenException

対象：

```text
token欠落
token不存在
用途不一致
PROCESSING中
workId不一致
その他token利用条件不一致
```

S-08：

```text
通常入力を保持
photo破棄
新token
同画面
```

S-11：

```text
stale入力を保存しない
最新GET
新token
新snapshot
```

---

### WorkNotFoundException

用途：

```text
本人所有Workが取得できない
実不存在
他人所有
```

外部：

```text
404
```

実不存在と他人所有を区別する追加DB検索は行いません。

---

### AutoSaveConflictException

expected値不一致時：

```text
HTTP 409
{
  "code": "CONFLICT"
}
```

競合時DB更新は行いません。

---

### InvalidCompletionDateException

ServiceでDB状態やClockを参照して判定する完成日ルール違反に使用します。

例：

```text
未来日
IN_PROGRESS状態で完成日手動変更
```

外部：

```text
HTTP 400
VALIDATION_ERROR
completionDate field error
```

---

### Controller / Advice / Securityの例外責務

#### Controller内で処理

```text
BindingResult
RegistrationValidationException
ImageValidationException
InvalidSubmissionTokenException
```

現在のFormや入力値を保持して同画面へ戻す必要があるものをControllerで処理します。

#### GlobalExceptionHandler

```text
BasicInfoConflictException
WorkNotFoundException
通常HTML想定外例外
```

#### WorkAutoSaveExceptionHandler

```text
AutoSaveConflictException
InvalidCompletionDateException
WorkNotFoundException
非同期想定外例外
```

#### Spring Security

```text
未認証
CSRF
Access Denied
```

を担当します。

Service / ControllerでSecurityの401 / 403処理を再実装しません。

---

### 自動保存BindingResult

Bean Validation / Binding errorがあればServiceを呼びません。

```text
HTTP 400
VALIDATION_ERROR
fieldErrors
```

1フィールドにつき原則1エラーを返します。

必要に応じてWorkAutoSaveController内private helperでBindingResult→ErrorResponse変換を共通化して構いません。

---

### Entity→DTO変換責務

Entity→DTO変換はServiceで行います。

```text
Repository
↓
Entity
↓
Service
↓
DTO
↓
Controller
↓
Thymeleaf / JSON
```

ControllerへJPA Entityを直接返しません。

ThymeleafへJPA Entityを直接渡す構成にしません。

---

### Work一覧DTO変換

```java
return works.map(
        this::toInProgressWorkSummary
);
```

```java
return works.map(
        this::toCompletedWorkSummary
);
```

のようにService内private helperで変換します。

`hasPhoto`：

```java
work.getPhotoReference() != null
```

から生成し、photoReferenceそのものをControllerへ渡しません。

---

### WorkDetailResult変換

通常：

```text
Work
+
新submissionToken
→ WorkDetailResult
```

再表示：

```text
Work
+
existingSubmissionToken
→ WorkDetailResult
```

共通変換private helperを使用して構いません。

---

### Mapper / Projection方針

初期版では以下を導入しません。

```text
WorkMapper @Component
MapStruct
Repository Projection
DTO.from(Entity)
```

小規模な変換はServiceのprivate helperで十分とします。

DTO自身はEntityへ依存させません。

---

### Service publicメソッドの構造

publicメソッドは業務ユースケースの処理順序が上から下へ追える構造とします。

private helperへ切り出してよい例：

```text
owner付きEntity取得
Entity→DTO変換
snapshot比較
Pageable生成
画像補償削除
同じ計算式
小さな日時更新補助
```

切り出さないもの：

```text
publicユースケース全体
重要な業務処理順序
transaction境界そのもの
異なる業務ルールをObject / String等で無理に共通化した巨大処理
行数を短くするだけの分割
```

private helperの具体的な全メソッド名は設計上固定しません。

---

### UserRegistrationService処理順

```text
username重複確認
↓
email重複確認
↓
Password Blocklist確認
↓
password hash化
↓
User Entity生成
↓
createdAt設定
↓
save
```

DB UNIQUE競合について、username / emailどちらかを安全に判定できる場合だけ通常入力エラーへ変換します。

原因不明のDataIntegrityViolationExceptionをすべて重複エラー扱いにはしません。

---

### GaugeAdjustmentService処理順

```text
目側入力あり？
↓
あれば計算
↓
段側入力あり？
↓
あれば計算
↓
GaugeAdjustmentResult生成
```

Formが部分入力Validationを担当するため、同じBean ValidationをServiceで二重実装しません。

目・段で共通する計算式はprivate helper化して構いません。

---

### YarnCombinationService処理順

```text
mode確認
↓
選択中modeの値だけ使用
↓
各糸 g/m
↓
合成 g/m
↓
100g換算糸長
↓
最終段階で整数丸め
↓
太さ・推奨針対応取得
↓
YarnCombinationResult
```

非選択modeの入力値は画面で保持されても計算へ使用しません。

---

### getInProgressWorks処理順

```text
userId → User取得
↓
Pageable生成
lastUpdatedAt DESC
id DESC
↓
category == null
  → findByUserAndStatus
category != null
  → findByUserAndStatusAndCategory
↓
Page<Work>
↓
Page.map
↓
Page<InProgressWorkSummary>
```

---

### getCompletedWorks処理順

```text
userId → User取得
↓
Pageable生成
completionDate DESC
id DESC
size = 10
↓
category有無でRepository切替
↓
Page<Work>
↓
Page<CompletedWorkSummary>
```

---

### getWorkDetail処理順

```text
User取得
↓
owner付きWork取得
↓
不存在ならWorkNotFoundException
↓
current Workからsnapshot生成
↓
SubmissionTokenManagerでtoken + snapshot登録
↓
Work + token
↓
WorkDetailResult
```

---

### getWorkDetailForRedisplay処理順

```text
User取得
↓
owner付きWork取得
↓
不存在ならWorkNotFoundException
↓
新token / snapshotを発行しない
↓
Work + existingSubmissionToken
↓
WorkDetailResult
```

---

### getWorkImage処理順

```text
User取得
↓
owner付きWork取得
↓
photoReference確認
↓
画像処理・ストレージ担当へread依頼
↓
実fileなし / no photoならdefault fallback
↓
WorkImageResult
```

owner確認前にファイルを読みません。

---

### WorkAutoSaveService共通処理順

4メソッド共通の基本骨格：

```text
owner付きPESSIMISTIC_WRITE取得
↓
expected値比較
↓
業務Validation
↓
実変更有無確認
↓
変更あり
  → Entity更新
  → 必要ならlastUpdatedAt更新
変更なし
  → DB更新なし
↓
Result DTO生成（必要な場合）
↓
commit
```

4種類の更新を1つの汎用Objectメソッドへ統合しません。

---

### 文字列正規化責務

原則、画面入力に対する文字列正規化はForm setterで行います。

Controller / Service / Entityで同じ正規化を繰り返しません。

グローバルな`StringTrimmerEditor`は使用しません。

#### 登録username

UserRegistrationForm：

```java
this.username =
        username == null
        ? null
        : username.strip();
```

内部空白は保持し、Validationで不正とします。

DB保存時は大文字小文字を維持します。

重複 / login判定のみIgnoreCaseです。

#### 登録email

UserRegistrationForm：

```java
this.email =
        email == null
        ? null
        : email
            .strip()
            .toLowerCase(Locale.ROOT);
```

#### password

一切加工しません。

```text
trimしない
stripしない
case変換しない
Unicode normalizationしない
```

ASCII spaceは有効文字です。

passwordConfirmationも同様です。

#### 作品名

WorkCreateForm / WorkBasicInfoForm：

```java
strip()
```

内部空白は保持します。

#### memo / expectedMemo

一切加工しません。

```text
前後空白
内部空白
改行
```

をそのまま保存・比較します。

#### submissionToken

完全無加工・完全一致で扱います。

#### Login username

LoginFormを使用しないため例外的に認証入口で：

```text
strip()
↓
4〜20文字
↓
半角英数字
↓
findByUsernameIgnoreCase
```

とします。

Login passwordは完全無加工です。

---

### Clock / 日時

共通Bean：

```java
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(
                ZoneId.of("Asia/Tokyo")
        );
    }
}
```

業務日時では：

```java
LocalDateTime.now(clock)
LocalDate.now(clock)
```

を使用します。

以下へ依存しません。

```text
LocalDateTime.now()
LocalDate.now()
DEFAULT CURRENT_TIMESTAMP
ON UPDATE CURRENT_TIMESTAMP
全更新共通JPA callback
DB trigger
```

---

### User.createdAt

`UserRegistrationService.registerUser()`で：

```java
user.setCreatedAt(
        LocalDateTime.now(clock)
);
```

とします。

MySQLの自動時刻設定へ依存しません。

---

### Work登録時日時

1つの業務処理で日付と日時の両方が必要な場合は、原則1回だけ：

```java
LocalDateTime now =
        LocalDateTime.now(clock);

LocalDate today =
        now.toLocalDate();
```

とします。

新規作品：

```text
lastUpdatedAt = now

status = IN_PROGRESS
→ completionDate = null

status = COMPLETED
→ completionDate = today
```

---

### lastUpdatedAt更新対象

実際に値が変わった場合のみ更新します。

```text
作品名
カテゴリー
写真追加
写真差し替え
写真削除
目数
段数
memo
status
```

status変更に伴うcompletionDate変更も「status変更」としてlastUpdatedAtを更新します。

---

### lastUpdatedAtを更新しない場合

```text
作品詳細を閲覧しただけ
一覧表示
画像fallback表示
完成日だけを手動変更
同値再送
保存ボタンを押したが永続状態が変わらなかった
作品削除直前
```

---

### status / completionDate連動

#### IN_PROGRESS → COMPLETED

```text
status = COMPLETED
completionDate = now.toLocalDate()
lastUpdatedAt = now
```

同一transactionで処理します。

#### COMPLETED → IN_PROGRESS

```text
status = IN_PROGRESS
completionDate = null
lastUpdatedAt = now
```

#### 同じstatus再送

```text
正常終了
completionDate再設定なし
lastUpdatedAt更新なし
```

---

### completionDate手動変更

Serviceで：

```text
status == COMPLETED
当日以前
未来日不可
```

を検証します。

正常変更：

```text
completionDate更新
lastUpdatedAtは更新しない
```

未来判定は：

```java
LocalDate.now(clock)
```

を使用します。

---

### Optional方針

`Optional`は原則Repositoryの単一検索結果だけで使用します。

```text
Repository
Optional<Entity>
↓
Serviceで即解決
↓
Entity / Exception
```

ControllerへOptionalを返しません。

Service publicメソッドからOptionalを返しません。

DTO / Entity / Form fieldにOptionalを持たせません。

---

### null方針

正常な「値なし」を表現するもの：

```text
photoReference
completionDate
expectedCompletionDate
一覧category=null → 全カテゴリー
GaugeAdjustmentResultの未計算側
```

使用しない例：

```text
memo=null
```

memo未入力：

```text
""
```

作品一覧0件：

```text
empty Page
```

作品不存在：

```text
WorkNotFoundException
```

認証User不存在：

```text
IllegalStateException等の内部不整合
```

---

### `orElse(null)`方針

Repositoryのowner検索等で：

```java
.orElse(null)
```

して後でnull判定する方式は原則使用しません。

```java
.orElseThrow(...)
```

でその場で業務上の意味へ変換します。

---

### 初期版で作らない構造

```text
LoginService
CounterService
YarnGuideService（現時点）
Service interfaceをDIのためだけに作る
AbstractService
BaseWorkService
WorkOwnershipService
WorkMapper
MapStruct
Repository Projection
Controller→Repository直接依存
DAO
Specification（初期版）
DTO機械的1対1化
EntityをFormとして直接使用
HttpSessionをWorkServiceへ直接渡す
DB versionカラム
画像本体のDB保存
```

---

### パッケージ配置方針

package-by-layerを維持します。

```text
controller
service
repository
entity
domain
form
dto
security
config
exception
```

Custom Validator：

```text
form.validation
```

SubmissionTokenManager：

```text
service
```

を推奨します。

---

### 主要クラス一覧

#### Controller

```text
HomeController
UserRegistrationController
LoginController
GaugeAdjustmentController
YarnCombinationController
CounterController
YarnGuideController
WorkController
WorkAutoSaveController
```

#### Service

```text
UserRegistrationService
GaugeAdjustmentService
YarnCombinationService
WorkService
WorkAutoSaveService
SubmissionTokenManager
```

#### Repository

```text
UserRepository
WorkRepository
```

#### DTO

```text
GaugeAdjustmentResult
YarnCombinationResult
InProgressWorkSummary
CompletedWorkSummary
WorkDetailResult
WorkImageResult
CounterSaveResult
StatusSaveResult
CompletionDateSaveResult
ErrorResponse
```

#### 主要例外

```text
WorkNotFoundException
AutoSaveConflictException
BasicInfoConflictException
InvalidCompletionDateException
RegistrationValidationException
InvalidSubmissionTokenException
ImageValidationException
```

---

---

## 11. 計算ロジック・毛糸対応基準設計

### S-04 ゲージ調整計算

#### 基本計算式

目数：

```text
調整後の目数の計算値
= 元の目数 × 実測目ゲージ ÷ 指定目ゲージ
```

段数：

```text
調整後の段数の計算値
= 元の段数 × 実測段ゲージ ÷ 指定段ゲージ
```

途中で表示用・業務上の桁丸めは行いません。

#### Java型

計算には `BigDecimal` を使用します。

`double` / `Double` は使用しません。

入力側の既存仕様は以下を維持します。

```text
ゲージ：
0.01～999.99
小数第2位まで

元の目数・段数：
1～9999
整数
```

#### 内部計算精度

割り切れない `BigDecimal` 除算に対応するため、内部除算には以下を使用します。

```java
MathContext.DECIMAL128
```

例：

```java
BigDecimal calculatedValue =
        BigDecimal.valueOf(originalStitchCount)
                .multiply(measuredStitchGauge)
                .divide(
                        specifiedStitchGauge,
                        MathContext.DECIMAL128
                );
```

固定小数桁へ途中丸めする方式は採用しません。

#### 最終整数への丸め

最終的な目数・段数のみ、

```java
RoundingMode.HALF_UP
```

で四捨五入して整数化します。

```java
int adjustedCount =
        calculatedValue
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
```

現在の入力上限では `Integer` の範囲へ収まるため、
既存の `GaugeAdjustmentResult` の型は変更しません。

#### 四捨五入前計算値

四捨五入前の計算値は `BigDecimal` のまま
`GaugeAdjustmentResult` へ保持します。

```java
public record GaugeAdjustmentResult(
        BigDecimal stitchCalculatedValue,
        Integer adjustedStitchCount,
        BigDecimal rowCalculatedValue,
        Integer adjustedRowCount
) {}
```

片側のみ計算した場合は、既存仕様どおり未計算側に `null` を許可します。

#### 四捨五入前計算値の画面表示

画面上では、小数部を最大5桁まで表示します。

不要な末尾0は表示しません。

```text
12 → 12
12.5 → 12.5
12.345 → 12.345
12.345678... → 12.34568
```

表示用の桁調整は画面表示上のフォーマット処理とし、
Service内部の計算値そのものは変更しません。

最終整数は、表示用に整形した値ではなく、
内部 `BigDecimal` 値へ直接 `HALF_UP` を適用して求めます。

具体的なThymeleafのフォーマット記述は、
この仕様を満たす範囲で実装時に決定して構いません。

---

### S-05 2本取り・引き揃え計算

#### 共通方針

重量・長さおよび内部計算には `BigDecimal` を使用します。

既存の入力範囲を維持します。

```text
重量・長さ：
0.01～99999.99
小数第2位まで
```

途中計算では固定小数桁への業務上の丸めを行いません。

割り算には `MathContext.DECIMAL128` を使用します。

#### 2本取り

```text
1本分の1mあたり重量
= 重量 ÷ 長さ

2本取り後の1mあたり重量
= 1本分の1mあたり重量 × 2

100gあたり換算糸長
= 100 ÷ 合成1mあたり重量
```

概念コード：

```java
BigDecimal weightPerMeter =
        doubleStrandWeight.divide(
                doubleStrandLength,
                MathContext.DECIMAL128
        );

BigDecimal combinedWeightPerMeter =
        weightPerMeter.multiply(BigDecimal.valueOf(2));

BigDecimal calculatedLengthPer100g =
        BigDecimal.valueOf(100)
                .divide(
                        combinedWeightPerMeter,
                        MathContext.DECIMAL128
                );
```

#### 引き揃え

```text
糸1の1mあたり重量
= 糸1重量 ÷ 糸1長さ

糸2の1mあたり重量
= 糸2重量 ÷ 糸2長さ

合成1mあたり重量
= 糸1 + 糸2

100gあたり換算糸長
= 100 ÷ 合成1mあたり重量
```

概念コード：

```java
BigDecimal yarn1WeightPerMeter =
        yarn1Weight.divide(
                yarn1Length,
                MathContext.DECIMAL128
        );

BigDecimal yarn2WeightPerMeter =
        yarn2Weight.divide(
                yarn2Length,
                MathContext.DECIMAL128
        );

BigDecimal combinedWeightPerMeter =
        yarn1WeightPerMeter.add(yarn2WeightPerMeter);

BigDecimal calculatedLengthPer100g =
        BigDecimal.valueOf(100)
                .divide(
                        combinedWeightPerMeter,
                        MathContext.DECIMAL128
                );
```

#### 100g換算糸長の整数化

100g換算糸長を最終結果へ変換するときだけ、
`RoundingMode.HALF_UP` で整数へ四捨五入します。

```java
long lengthPer100g =
        calculatedLengthPer100g
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
```

既存の `YarnCombinationResult.lengthPer100g` は `long` のまま維持します。

#### 計算処理順

既存の詳細設計09の処理順を維持します。

```text
mode確認
↓
選択中modeの値だけ使用
↓
各糸 g/m
↓
合成 g/m
↓
100g換算糸長
↓
HALF_UPで整数化
↓
太さ・推奨針対応取得
↓
YarnCombinationResult生成
```

---

### 太さ判定に使用する換算糸長

毛糸の太さ区分、推奨棒針、推奨かぎ針の判定には、
丸め前の `BigDecimal` 値ではなく、
`HALF_UP` で整数化済みの `lengthPer100g` を使用します。

```text
153.846153...
↓
HALF_UP
↓
154
↓
154m / 100g が属する太さ区分を判定
```

これにより、S-05の画面表示値、S-07の対応表の基準値、
内部の太さ判定値を一致させます。

---

### 毛糸基準の前提

#### 基準素材

S-05およびS-07の太さ・推奨針基準は、

```text
ウール100％を中心とした
一般的なウール系ストレートヤーン
```

を基準とします。

基準データの調査対象から、次のような特殊糸は除外します。

- モヘヤ等の強い起毛糸
- スラブヤーン
- ループヤーン
- ファーヤーン
- テープヤーン
- その他、重量・長さだけでは一般的なストレートヤーンと比較しにくい特殊形状

S-05の機能自体は素材を制限しません。

アクリル、コットン、混紡等を入力しても計算は行えますが、
太さ・推奨針は本書のウール系基準に基づく推定値とします。

#### 基準の考え方

主要メーカーの一般的なウール系製品および
一般的な毛番手目安を比較し、
Knitting独自の連続した判定範囲を設定します。

メーカーごとに同じ太さ名称でも糸長・推奨針が異なる場合があるため、
「すべてのメーカー製品へ100％一致する規格」ではなく、
一般的な目安として扱います。

---

### 毛糸太さ区分

| 太さ | 100gあたり換算糸長 |
|---|---:|
| 極細 | 800m以上 |
| 合細 | 500～799m |
| 中細 | 350～499m |
| 合太 | 250～349m |
| 並太 | 175～249m |
| 極太 | 101～174m |
| 超極太 | 100m以下 |

内部判定上は `超極太 = 0～100m` とします。

負数は正常値として扱いません。

---

### 推奨棒針・かぎ針基準

| 太さ | 100g換算糸長 | 推奨棒針 | 推奨かぎ針 |
|---|---:|---|---|
| 極細 | 800m以上 | 0～2号（2.1～2.7mm） | 2/0号（2.0mm） |
| 合細 | 500～799m | 1～3号（2.4～3.0mm） | 2/0～3/0号（2.0～2.3mm） |
| 中細 | 350～499m | 2～4号（2.7～3.3mm） | 2/0～4/0号（2.0～2.5mm） |
| 合太 | 250～349m | 4～6号（3.3～3.9mm） | 4/0～5/0号（2.5～3.0mm） |
| 並太 | 175～249m | 6～8号（3.9～4.5mm） | 5/0～7/0号（3.0～4.0mm） |
| 極太 | 101～174m | 9～11号（4.8～5.4mm） | 8/0～10/0号（5.0～6.0mm） |
| 超極太 | 100m以下 | 12号以上（5.7mm以上） | 10/0号以上（6.0mm以上） |

超極太でさらに太い針を使用する場合は、
号数ではなくmm表記を使用して構いません。

---

### 毛糸基準データのJava保持

#### 基本方針

S-05とS-07で同じ基準値を重複定義しません。

Java Enum `YarnThicknessStandard` を作成し、
毛糸基準データの唯一の正とします。

専用DBテーブル、Repository、YarnGuideService、
専用DTO、JSON、CSV、application.properties等への外出しは行いません。

#### Enum値

```java
GOKUBOSO
AIBOSO
CHUBOSO
AIBUTO
NAMIBUTO
GOKUBUTO
CHOGOKUBUTO
```

表示順もこの宣言順とします。

#### Enumが保持する情報

- 表示用太さ名称
- 100g換算糸長の下限
- 100g換算糸長の上限
- 100g換算糸長表示文字列
- 推奨棒針号数
- 推奨棒針mm
- 推奨かぎ針号数
- 推奨かぎ針mm

#### 判定メソッド

```java
public static YarnThicknessStandard fromLengthPer100g(
        long lengthPer100g
)
```

0以上の `lengthPer100g` は必ず7区分のいずれかへ分類します。

負数が渡された場合は `IllegalArgumentException` で拒否します。

新しい業務例外クラスは追加しません。

---

### S-05での利用

`YarnCombinationService` では、

```text
100g換算糸長をBigDecimalで算出
↓
HALF_UPでlongへ整数化
↓
YarnThicknessStandard.fromLengthPer100g(...)
↓
太さ・推奨針取得
↓
YarnCombinationResult生成
```

とします。

既存DTO：

```java
public record YarnCombinationResult(
        long lengthPer100g,
        String thicknessLabel,
        String knittingNeedleLabel,
        String crochetHookLabel
) {}
```

は変更しません。

例：

```text
knittingNeedleLabel
= 6～8号（3.9～4.5mm）

crochetHookLabel
= 5/0～7/0号（3.0～4.0mm）
```

---

### S-05 計算結果表示

結果領域は以下の順を基本とします。

1. 入力内容
2. 100gあたりの換算糸長
3. 推定される毛糸の太さ
4. 推奨棒針
5. 推奨かぎ針
6. S-07「毛糸・推奨針号数対応表」へのリンク
7. 注意書き

換算糸長：

```text
約○m / 100g
```

途中計算値である各糸g/m、合成g/m、
丸め前100g換算糸長は画面表示しません。

推奨針はS-05では1行形式とします。

```text
推奨棒針
6～8号（3.9～4.5mm）

推奨かぎ針
5/0～7/0号（3.0～4.0mm）
```

S-07へのリンクは通常遷移とし、
該当行への自動スクロール、色付け、強調表示は行いません。

---

### S-05 入力変更後の再計算

計算後に入力値を変更しても、直前の結果は表示したままにします。

```text
入力内容が変更されています。再計算してください。
```

という趣旨の案内を表示します。

再計算成功時に結果を更新し、同案内を非表示にします。

---

### S-05 注意書き

```text
計算結果は目安です。
太さ・推奨針は、一般的なウール系ストレートヤーンを基準としています。
素材、撚り、膨らみ、毛足、糸の構造などによって、
実際の太さや適した針サイズは異なります。
商品にメーカー推奨値が記載されている場合はメーカー表示を優先し、
実際の糸と針で試し編みすることをおすすめします。
```

文意を変えない範囲で、句読点・改行は実装時調整可とします。

---

### S-07 毛糸・推奨針号数対応表

#### 表構成

4列・7行の表とします。

```text
毛糸の太さ
100gあたりの長さ
推奨棒針
推奨かぎ針
```

#### 表示内容

| 毛糸の太さ | 100gあたりの長さ | 推奨棒針 | 推奨かぎ針 |
|---|---|---|---|
| 極細 | 800m以上 | 0～2号 / 2.1～2.7mm | 2/0号 / 2.0mm |
| 合細 | 500～799m | 1～3号 / 2.4～3.0mm | 2/0～3/0号 / 2.0～2.3mm |
| 中細 | 350～499m | 2～4号 / 2.7～3.3mm | 2/0～4/0号 / 2.0～2.5mm |
| 合太 | 250～349m | 4～6号 / 3.3～3.9mm | 4/0～5/0号 / 2.5～3.0mm |
| 並太 | 175～249m | 6～8号 / 3.9～4.5mm | 5/0～7/0号 / 3.0～4.0mm |
| 極太 | 101～174m | 9～11号 / 4.8～5.4mm | 8/0～10/0号 / 5.0～6.0mm |
| 超極太 | 100m以下 | 12号以上 / 5.7mm以上 | 10/0号以上 / 6.0mm以上 |

内部判定上は超極太を0～100mとしますが、
画面では「100m以下」と表示します。

#### 針サイズ表示

S-07では各セル内で、

```text
1行目：号数
2行目：mm
```

と表示します。

S-05は1行、S-07は2行ですが、
参照する基準データは同じ `YarnThicknessStandard` とします。

#### レスポンシブ

PCでは4列表を通常表示します。

スマートフォンでもカード表示へ変更せず、
画面幅へ収まらない場合は表全体を横スクロール可能とします。

縦スクロール時には表ヘッダーを固定します。

区分ごとの色分け・おすすめ表示・強調表示は行いません。

具体的な配色、罫線、余白、文字サイズ等は
詳細デザイン・実装時に決定します。

---

### S-07 注意書き

```text
この対応表は、一般的なウール系ストレートヤーンを基準とした目安です。
素材、撚り、膨らみ、毛足、糸の構造などによって、
実際の太さや適した針サイズは異なります。
商品にメーカー推奨値が記載されている場合はメーカー表示を優先し、
実際の糸と針で試し編みすることをおすすめします。
```

---

### 異常値・極端値

`YarnCombinationForm` のValidationを正常に通過した入力から
数学的に算出できた結果は、値が極端であっても正常結果として扱います。

現実的な毛糸かどうかを理由とした追加Validationは行いません。

整数化結果が `0m` になった場合も正常とし、超極太へ分類します。

0を1へ補正しません。

800m以上には上限を設けず、すべて極細へ分類します。

「判定不能」「その他」等の追加区分は作成しません。

正常なForm入力では重量・長さとも0より大きいため、
通常計算で0除算は発生しません。

現在の入力範囲では `lengthPer100g` はJava `long` の範囲へ十分収まるため、
型変更は行いません。

---

### 境界値

| 値 | 判定 |
|---:|---|
| 800 | 極細 |
| 799 | 合細 |
| 500 | 合細 |
| 499 | 中細 |
| 350 | 中細 |
| 349 | 合太 |
| 250 | 合太 |
| 249 | 並太 |
| 175 | 並太 |
| 174 | 極太 |
| 101 | 極太 |
| 100 | 超極太 |
| 0 | 超極太 |

区分間に隙間や重複を作りません。

---

### YarnGuideController

S-07専用 `YarnGuideService` は作成しません。

`YarnGuideController` から、

```java
model.addAttribute(
        "yarnStandards",
        YarnThicknessStandard.values()
);
```

のようにEnum一覧をModelへ渡します。

専用DTOは追加しません。

---

### YarnThicknessStandardのパッケージ配置

`YarnThicknessStandard` のJavaパッケージ配置は、
S-05実装時に `com.example.demo.domain` として確定しました。

---

---

## 12. Thymeleaf・JavaScript・画面動作設計

### 共通方針

- Thymeleafによるサーバーサイドレンダリングを基本とする。
- SPA化しない。
- JavaScriptは必要最小限とする。
- JavaScriptで業務ロジックを重複実装しない。
- JavaScriptによるValidationはUX補助とし、正式ValidationはForm・Service・DB側で保証する。
- JavaScriptだけを前提に認証・認可・データ整合性を保証しない。
- 外部JavaScriptライブラリやUIフレームワークは、必要性が明確でない限り追加しない。
- インラインJavaScriptは原則使用せず、イベントは`addEventListener()`で登録する。

初期版のJavaScript構成は次を基本とする。

```text
src/main/resources/static/js/
├─ counter.js
├─ navigation.js
├─ work-list.js
├─ work-detail.js
├─ register.js
├─ password-toggle.js
└─ yarn-combination.js
```

---

### S-06 localStorage保存単位・キー名

目数・段数は2つの独立したlocalStorageキーへ保存する。

```text
knitting.simpleCounter.stitchCount
knitting.simpleCounter.rowCount
```

- 0～9999の整数を文字列として保存する。
- 1つのJSONオブジェクトにはまとめない。
- userId、workId、username等をキー名に含めない。
- S-11の作品専用カウンターとは完全に分離する。
- JavaScript内ではキー文字列を定数へ集約し、同じ文字列を各所へ重複記述しない。

---

### S-06 初期読込・保存タイミング

S-06表示時に、目数・段数をlocalStorageから1回ずつ読み込む。

- 保存値なし → 0
- ＋／－ → 正常値確定時に即時保存
- 個別リセット → 即時保存
- すべてリセット → 確認後に両方を0として即時保存
- 直接入力 → 正常値確定時に即時保存

次は使用しない。

- debounce保存
- interval/timer保存
- `beforeunload`での追加保存
- `unload`での追加保存
- `pagehide`での追加保存
- HTTP通信

---

### S-06 直接入力・全角数字変換・入力途中

直接入力欄は次を使用する。

```html
type="text"
inputmode="numeric"
```

`type="number"`は使用しない。

`input`イベントで全角数字`０～９`のみを半角`0～9`へ変換する。英字、記号、小数点、符号等は別の値へ補正しない。

正常値は0～9999の整数とする。

編集中の一時的な空欄は許可し、空欄中は次の扱いとする。

- 0へ自動変換しない。
- 保存しない。
- 直ちにエラー表示しない。
- 最後に確定した値を変更しない。
- ＋／－を一時的に無効化する。
- blur時に空欄なら最後の確定値へ戻す。

不正値は以下とする。

- 数字以外
- 小数
- 負数
- 9999超

不正値の場合は次を表示する。

```text
0～9999の整数で入力してください。
```

不正値は保存せず、最後の確定値も変更しない。blur時は最後の確定値へ戻す。

`maxlength="4"`は使用しない。10000等はJavaScript Validationで判定する。

---

### S-06 不正保存値の初期復旧

localStorage保存値は目数・段数を独立して検証する。

正常値は、半角数字のみで構成された0～9999の整数とする。

以下は不正保存値とする。

- 空文字列
- 数字以外
- 小数
- 負数
- 9999超
- 全角数字
- その他正常形式でない文字列

不正保存値の場合は、対象キーのみ画面値0・保存値`"0"`へ正規化する。ユーザー向けエラーは表示しない。

片方のみ不正なら、正常なもう片方は変更しない。

`localStorage.clear()`は使用しない。

---

### S-06 localStorage利用不可時

初期表示時に`try-catch`で一時キーの書込・読込・削除を実際に行い、localStorageが利用可能か判定する。

利用不可でもS-06自体は使用可能とし、目数・段数を0から開始してページ内JavaScript状態だけで動作させる。

表示する案内は次とする。

```text
カウンターの値を保存できません。ページの移動・再読み込み・終了後は、現在の値を保持できない場合があります。
```

初期判定後でも`setItem()`が例外になった場合は保存不可モードへ移行し、現在画面値は維持する。ページ内では以降の保存再試行を繰り返さず、次回の完全なページ読み込み時に再判定する。

次の自動フォールバックは行わない。

- sessionStorage
- Cookie
- HttpSession
- MySQL
- IndexedDB

---

### S-06 リセット

#### 個別リセット

目数／段数の個別リセットは確認なしで対象だけ0へ戻す。

- 画面値を0
- 現在の確定値を0
- localStorage利用可能なら対象キーを`"0"`
- 入力途中の空欄／不正状態を破棄
- エラー表示解除

#### すべてリセット

「すべてリセット」は`window.confirm()`を使用する。

```text
目数と段数を0に戻します。よろしいですか？
```

OKなら両方0、Cancelなら何も変更しない。完了トースト等は表示しない。

---

### S-06 JavaScript構成

JavaScriptは次に配置する。

```text
src/main/resources/static/js/counter.js
```

`counter.html`から`defer`付きで読み込む。

基本IDは次とする。

```text
stitch-count
stitch-decrement-button
stitch-increment-button
stitch-reset-button
stitch-error
row-count
row-decrement-button
row-increment-button
row-reset-button
row-error
reset-all-button
storage-warning
```

DOM取得は主に`document.getElementById()`を使用する。

初期化順は次とする。

1. DOM参照取得
2. localStorage利用可否確認
3. 保存値読込
4. 画面反映
5. ボタン状態反映
6. イベント登録

`defer`を使用するため、初期化だけを目的とした`DOMContentLoaded`は使用しない。

専用class、外部ライブラリ、ES Modules、import/export、複数JSへの分割は行わない。

`CounterController`はGETのみで、カウンター値をModelへ渡さない。

---

### S-11 遷移元保持・一覧へ戻る動作

S-11の遷移元一覧をquery parameter`from`で保持する。

有効値：

```text
in-progress
completed
```

S-09からS-11へ移動する場合：

```text
/works/{id}?from=in-progress
```

S-10からS-11へ移動する場合：

```text
/works/{id}?from=completed
```

S-11内で制作ステータスが変わっても、有効な`from`がある場合は戻り先と現在地強調を遷移元一覧に固定する。

`from`なしの場合は、現在の作品ステータスにより戻り先・現在地を決定する。

不正`from`はエラーにせず、`from`なしとして扱う。

`WorkController.showWorkDetail()`へ任意`from`を追加する。基本情報保存成功後のPOST→Redirect→GETでは、有効な`from`のみ引き継ぐ。

自動保存APIには`from`を送らない。DB、Session、Cookie、Referer、`history.back()`には依存しない。

---

### S-09／S-10 ページネーション

S-09・S-10で共通ルールを使用する。

- 1ページ最大10件
- 10件以下はページネーション非表示
- 11件以上で一覧下部に表示
- 「前へ」「ページ番号」「次へ」

総ページ数`N`、現在ページ`C`として、ページ番号表示は次とする。

```text
N <= 7
1 2 3 4 5 6 7

N >= 8、C=1～4
1 2 3 4 5 … N

N >= 8、5 <= C <= N-4
1 … C-1 C C+1 … N

N >= 8、C >= N-3
1 … N-4 N-3 N-2 N-1 N
```

- `…`はリンクにしない。
- 現在ページはリンクにせず`aria-current="page"`等を付ける。
- 前へ／次へはページネーション表示中は常に表示する。
- 先頭ページでは前へ無効、最終ページでは次へ無効。
- pagination linkは選択中`category`を保持する。
- category未指定ならquery parameterを付けない。
- 1ページ目への生成リンクでは`page=1`を省略する。
- 直接`?page=1`アクセスは有効。
- JavaScriptは使用しない。
- Spring Data `Page`の情報を使用し、重複Model属性を追加しない。
- URL上は1始まり、`Page.number`は0始まり。

第10章で定める次の方針を維持する。

```text
page < 1 → 400
page範囲外 → 400
invalid category → 400
```

---

### S-09／S-10 作品名2行省略

作品カードの作品名はPC・スマートフォンとも最大2行とし、2行を超える部分をCSSで省略する。

Java、Service、DTO、Thymeleafで作品名自体を切り詰めない。

`InProgressWorkSummary.name`と`CompletedWorkSummary.name`は作品名全文を保持する。

Thymeleafでは`th:text="${work.name}"`等で全文を出力する。

S-09・S-10共通CSSクラスを使用し、基本は次の複数行クランプとする。

```css
display: -webkit-box;
-webkit-box-orient: vertical;
-webkit-line-clamp: 2;
overflow: hidden;
```

JavaScriptによる文字数計算・高さ計測・文字列切断、`substring()`、1行用`white-space: nowrap`、Thymeleafによる固定`…`追加は行わない。

カード全体からS-11へ移動する既存操作は変更しない。

---

### S-09／S-10 カテゴリー絞り込み

query parameterは既存どおり`category`とする。

送信値は`WorkCategory` enum定数名をそのまま使用する。

```text
HAT
TOP
BOTTOM
SCARF
BAG
TOY
SMALL_GOODS
OTHER
```

表示名：

```text
すべて
帽子
上衣
下衣
マフラー
バッグ
ぬいぐるみ
小物
その他
```

カテゴリー変更時は「絞り込む」ボタンを設けず、JavaScriptで通常GET遷移する。変更時に現在の`page`は引き継がず1ページ目へ移動する。

「すべて」は`category=`を付けず、query parameter自体を省略する。

共通JavaScript：

```text
src/main/resources/static/js/work-list.js
```

S-09・S-10の両画面から`defer`付きで読み込む。

JavaScriptは`window.location.pathname`を利用し、S-09／S-10を個別判定する処理を作らない。

選択状態には既存Model属性`selectedCategory`を使用する。

ページ移動時はcategoryを保持し、S-09／S-10間のタブ切替時にはcategory・pageを引き継がない。

---

### Thymeleaf共通Fragment

配置：

```text
src/main/resources/templates/fragments/
├─ header.html
└─ footer.html
```

`header.html`には、未ログイン用`guestHeader(...)`とログイン後用`authenticatedHeader(activeMenu)`を定義する。

ログイン後のハンバーガーメニューは`authenticatedHeader`内に含め、navigation専用Fragment等へ過剰分割しない。

`activeMenu`の基本値：

```text
home
work-new
in-progress
completed
gauge-adjustment
yarn-combination
counter
yarn-guide
```

現在地判定専用Java enum・DTOは作成しない。

S-11では`from`または現在の作品ステータスに応じて`in-progress`／`completed`を使用する。

`footer.html`には全画面共通の`© Knitting`を持つ`footer` Fragmentを定義する。

- 共通HTMLを各画面へコピーしない。
- 標準`th:fragment`・`th:replace`を使用する。
- Thymeleaf Layout Dialect等は追加しない。
- 巨大な`layout.html`によるテンプレート継承は初期版では採用しない。
- Fragment内へ大量のCSS・JSをインライン記述しない。
- `authenticatedHeader`内に`POST /logout`用Formを共通配置する。

---

### 公開画面での認証状態判定とヘッダー切替

共通Model属性名：

```text
isAuthenticated
```

型：`boolean`

```text
未ログイン → false
通常ログイン → true
Remember-Meログイン → true
```

各Controllerで個別設定せず、次を新設する。

```text
<base-package>.controller.CommonModelAttributeAdvice
```

`@ControllerAdvice` + `@ModelAttribute("isAuthenticated")`相当で共通提供する。

認証済み判定は、Authenticationが存在し、認証済みであり、principalが`KnittingUserPrincipal`の場合とする。Spring Security標準anonymous authenticationを誤判定しない。

画面別：

```text
S-01・S-04・S-05・S-06・S-07
→ isAuthenticatedでguest/authenticatedを切替

S-02
→ guest固定

S-03
→ guest固定

S-08～S-11
→ authenticated固定
```

未ログイン用は`guestHeader(pageType)`とする。

```text
default → ログイン＋ユーザー登録
register → ログインのみ
login → ユーザー登録のみ
```

利用：

```text
S-01未ログイン → guestHeader('default')
S-04～S-07未ログイン → guestHeader('default')
S-02 → guestHeader('register')
S-03 → guestHeader('login')
```

初期版ではThymeleaf Security Extras、`sec:authorize`、JavaScript認証判定、認証確認API、Cookieからの認証推測は使用しない。

`isAuthenticated`は表示制御専用であり、認可はSpring Security・Service・Repository側で保証する。

---

### ログイン後ハンバーガーメニュー

JavaScript：

```text
src/main/resources/static/js/navigation.js
```

基本ID：

```text
global-menu-button
global-menu
```

初期表示は常に閉じた状態とし、`hidden=true`、`aria-expanded=false`とする。

ハンバーガーボタン押下で開閉を切り替える。開閉状態と`aria-expanded`を同期する。

メニューが開いている状態で、メニュー本体・ハンバーガーボタン以外をクリックした場合は閉じる。

Escキーで閉じ、ハンバーガーボタンへフォーカスを戻す。

通常メニュー項目は通常の`<a>`リンクとし、Ajax／SPAナビゲーションへ変更しない。

リンク選択時はメニューを閉じ、そのまま通常GET遷移する。

開閉状態はlocalStorage、sessionStorage、Cookie、HttpSession、URLへ保存しない。

PC・スマートフォンで同じ`navigation.js`を使用する。レスポンシブ差はCSSを基本とし、初期版では`resize`イベントによるJS制御を設けない。

以下は詳細デザインへ残す。

- ドロワー／ポップオーバー形式
- 幅・方向・背景・アニメーション
- フォーカストラップ
- 本文スクロール固定
- 具体的アイコン

---

### ログアウト確認とS-11未保存基本情報

ログアウト確認は`window.confirm()`ではなくHTML標準`<dialog>`を使用し、`authenticatedHeader`内へ共通配置する。

通常時：

```text
ログアウトしますか？
```

操作：

```text
キャンセル
ログアウト
```

S-11で作品基本情報に未保存変更がある場合は、別確認を追加せず同じdialogへ次を追加する。

```text
保存されていない変更内容は失われます。
```

未保存判定対象：

- 作品写真
- 作品名
- カテゴリー

対象外：

- 作品専用目数カウンター
- 作品専用段数カウンター
- 制作メモ
- 制作ステータス
- 完成日

編集モードに入っただけではdirtyにしない。編集開始時の値と比較し、3項目のいずれかが変わった場合だけ`basicInfoDirty=true`とし、全て元へ戻ればfalseへ戻す。

写真の新規選択・削除指定もdirty対象とする。

S-11固有dirty状態は`work-detail.js`で管理し、共通`navigation.js`との連携には`document.body.dataset.basicInfoDirty`を使用する。

ログアウトボタン押下時点ではPOSTせず、dialogで「ログアウト」を選択した場合だけ通常HTML Formで`POST /logout`を送信する。Spring Security標準`LogoutFilter`を使用し、独自Logout Controller、Ajax logout、`fetch()` logoutは作成しない。

CSRFはThymeleaf／Spring Security標準Form連携を使用する。

キャンセル時はdialogのみ閉じ、編集内容・dirty状態・自動保存状態を変更しない。

#### allowPageLeave統合

S-11で未保存基本情報がある状態からログアウトする場合、ログアウトdialog後に`beforeunload`確認を二重表示しない。

S-11では画面離脱許可状態として次を管理する。

```text
data-allow-page-leave="false"
```

ログアウトFormのsubmit成立時に`true`へ変更してから`POST /logout`を実行する。

`basicInfoDirty`自体はfalseへ書き換えない。

ログアウトでは作品自体は残るため、既存S-11自動保存の`visibilitychange`／`pagehide`によるbest-effort離脱flushは停止しない。

---

### S-11未保存離脱警告

作品基本情報に未保存変更がある状態でKnitting内の通常リンクから別画面へ移動しようとした場合、S-11固有`<dialog>`を表示する。

確認文言：

```text
変更内容が保存されていません。画面を移動しますか？
```

操作：

```text
キャンセル
移動する
```

未保存離脱dialogは`works/detail.html`へ配置する。

未保存なしなら通常遷移する。未保存ありならリンク遷移を一旦中止し、遷移先URLをページ内JavaScript変数に保持してdialogを表示する。

キャンセル時はS-11へ残り、入力内容・dirty状態を変更しない。

「移動する」時は`allowPageLeave=true`相当へしてから保持URLへ通常遷移する。

ブラウザ戻る／進む、再読み込み、タブ・ブラウザ終了、URL直接変更等は、以下の条件で`beforeunload`を使用する。

```text
basicInfoDirty == true
AND
allowPageLeave != true
```

`beforeunload`の具体的表示文言はブラウザ標準に任せる。

基本情報Formの正常submit時は`allowPageLeave=true`相当にしてからPOSTする。設定タイミングは保存ボタンclickではなくForm submit成立時を基本とする。

基本情報編集の「キャンセル」は画面遷移ではないため、編集開始時の内容へ戻しdirtyを解除して閲覧モードへ戻る。離脱確認は表示しない。

ログアウトは16章、作品削除は18章の専用確認を使い、汎用離脱dialogと二重確認しない。

`history.pushState()`や`popstate`による独自戻る制御は行わない。

---

### S-11作品削除確認

作品削除はS-11固有のHTML標準`<dialog>`を使用し、`works/detail.html`へ配置する。

通常確認文言：

```text
この作品を削除しますか？登録した写真、目数・段数、制作メモなどの関連情報も削除され、元に戻せません。
```

操作：

```text
キャンセル
削除する
```

作品名・写真・カテゴリーに未保存変更がある場合は、通常離脱確認を出さず、削除確認内へ次を追加する。

```text
保存されていない作品名・写真・カテゴリーの変更内容も失われます。
```

キャンセル時は削除POSTを送信せず、編集内容・dirty・編集モード・自動保存状態を変更しない。

「削除する」の場合だけ通常HTML Formで次を送信する。

```text
POST /works/{id}/delete
```

既存の`WorkController.deleteWork()`・`WorkService.deleteWork()`を使用する。成功後は`redirect:/`。

削除Formのsubmit成立時に`allowPageLeave=true`相当にして、`beforeunload`二重確認を防ぐ。`basicInfoDirty`自体はfalseへ変更しない。

削除確定時は`isDeleting=true`相当へ移行し、それ以降は新たな自動保存を開始しない。

- 未送信debounce timerを解除する。
- `visibilitychange`／`pagehide`による離脱flushを行わない。
- 削除確定前に開始済みの自動保存HTTP通信は強制中断しない。
- 削除のためだけに`AbortController`等を追加しない。

削除はAjax化せず、CSRF保護された通常HTML Formを使用する。本人所有作品確認はサーバー側で行う。

---

### S-01ログイン後トップ 0件表示

ログイン後トップで制作中作品が0件の場合でも「制作中の作品」セクションは表示する。

正式文言：

```text
制作中の作品はありません。
```

S-09の0件表示と統一する。

0件メッセージ付近に「作品を登録する」導線を表示し、`GET /works/new`へ遷移する。

見出し付近の「＋ 作品を登録」も0件時に残す。

「制作中一覧を見る」も表示し続け、`GET /works/in-progress`へ遷移する。

ダミー作品カードや空カードを表示せず、1つのempty-state領域としてメッセージと登録導線を表示する。共通初期画像を0件用ダミーカードには使わない。

JavaScriptで0件判定せず、既存Model属性`inProgressWorks`をThymeleafで判定する。

次の専用Model属性は追加しない。

```text
hasInProgressWorks
inProgressWorkCount
showEmptyState
```

未ログイン時はWorkServiceを呼ばず、制作中作品セクション自体を表示しない。

制作中作品0件でも、ログイン後トップの公開機能4種は通常どおり表示する。

---

### S-02 メールアドレス・ユーザー名重複確認

初期版ではリアルタイムAjax重複確認を行わない。

正式確認：

```text
POST /register
→ UserRegistrationService
→ Repository / DB
```

重複確認専用Controller、REST API、Endpoint、input/change/blurを契機としたHTTP通信は作成しない。

usernameは既存仕様どおりcase-insensitive、emailは既存正規化後の値でService／Repository／DBにより確認する。

重複時は`RegistrationValidationException`をControllerでfield errorへ変換し、S-02をHTTP 200で再表示する。

正式文言：

```text
このメールアドレスはすでに登録されています。
```

```text
このユーザー名はすでに使用されています。
```

再表示時はメールアドレス・ユーザー名を保持し、パスワード・確認用パスワードはHTMLへ再出力しない。

初回入力中はDB重複状態を知らないため、「未確認」と「重複」を区別する。重複未確認だけを理由に登録ボタンを無効化しない。

一度サーバーから重複エラーとして返された値は、その値が変更されるまで既知の重複状態として扱い、登録ボタンを無効化してよい。値を変更した時点で重複エラー状態を解除するが、新しい値を「重複なし」とはみなさず、次回POST時に再確認する。

S-02固有JavaScript：

```text
src/main/resources/static/js/register.js
```

JavaScriptによる登録ボタン制御はUX補助であり、正式Validation・重複保証はForm・Service・DBで行う。

---

### S-02／S-03 パスワード表示・非表示

S-02の「パスワード」「パスワード（確認）」、S-03の「パスワード」にそれぞれ個別の表示／非表示切替ボタンを設ける。

S-02の2欄は独立して切替可能とする。

初期状態はすべて`type="password"`。

非表示時の操作表示は「表示」、表示時は「非表示」とする。具体的な目アイコン等は詳細デザイン事項とする。

切替操作は`<button type="button">`を使用し、表示切替だけでForm submitしない。

共通JavaScript：

```text
src/main/resources/static/js/password-toggle.js
```

S-02では`register.js`と`password-toggle.js`を使用する。S-03で表示切替以外の専用処理が不要なら、表示切替だけのために`login.js`を作成しない。

対象ボタンへ`data-password-toggle`等を付与し、`aria-controls`から対象inputを特定する。

表示切替時はinputの`type`だけを変更し、パスワード値を加工・再設定・別変数へ複製・Storage保存しない。

既存autocompleteを維持する。

```text
S-02 password → new-password
S-02 passwordConfirmation → new-password
S-03 password → current-password
```

表示／非表示操作だけではValidation、エラー表示、登録ボタン・ログインボタン状態を変更しない。

画面再表示・再読み込み時は非表示状態へ戻し、表示状態をlocalStorage、sessionStorage、Cookie、HttpSessionへ保存しない。

JavaScriptが利用できなくても通常の`type="password"`として登録・ログイン可能とする。

---

### S-05 2本取り／引き揃えモード切替

HTML内部では`YarnCombinationMode`に対応するradio buttonを使用し、CSSでタブ風に表示する。

値：

```text
DOUBLE_STRAND
COMBINATION
```

ARIA tab widgetを独自実装しない。

初回`GET /yarn-combination`では`DOUBLE_STRAND`を選択し、2本取り入力領域を表示、引き揃え入力領域を非表示とする。

計算後・Validationエラー後は`YarnCombinationForm.mode`で選択状態・表示入力領域を復元する。

モード切替時はURL変更、ページ再読み込み、GET、POST、fetch、Ajaxを行わず、同一画面内でJavaScriptにより表示を切り替える。

入力領域は`hidden`等で表示／非表示だけを切り替え、inputをDOMから削除・再生成しない。

非選択モードのinputを`disabled`にせず、画面を離れていない間は両モードの入力値を保持する。GET Form送信時にも両モードの値を含められる状態とする。

Service・業務Validationでは既存どおり選択中モードだけを使用する。

#### 非選択モードのBinding error例外

非選択モードの値は業務Validation対象外だが、非選択モードの値であっても`BigDecimal`へBindingできない不正文字列が送信された場合は、第9章どおりBinding errorとする。

したがって「非選択モードは計算条件に影響しない」とは、**正常にBinding可能な値または空欄について、選択中モードの業務Validation対象にしない**という意味とする。

S-05専用JavaScript：

```text
src/main/resources/static/js/yarn-combination.js
```

責務：

- モード切替
- 入力領域切替
- 計算ボタン状態
- 計算後の入力変更案内

計算ロジック自体はJavaScriptへ実装しない。

計算済みモードから別モードへ切り替えた場合、その結果は非表示にする。ページ再読込なしで元の計算済みモードへ戻り、計算時入力値が変わっていなければ直前結果を再表示してよい。

モード切替だけでは次の案内を表示しない。

```text
入力内容が変更されています。再計算してください。
```

計算済みモードの計算対象入力値を変更した場合は、直前結果を残したまま上記案内を表示する。再度正常に計算した後は案内を非表示にする。

「計算する」の有効／無効は選択中モードの必要項目だけを対象とする。

```text
DOUBLE_STRAND
- doubleStrandWeight
- doubleStrandLength

COMBINATION
- yarn1Weight
- yarn1Length
- yarn2Weight
- yarn2Length
```

既存入力条件：

```text
0.01～99999.99
小数第2位まで
```

JavaScript判定はUX補助であり、正式Validationは`YarnCombinationForm`で保証する。

計算は既存どおり通常GET Formで実行する。

```text
GET /yarn-combination
calculate=true
```

計算専用APIや`fetch()`は追加しない。

---

### 実装時の主なJavaScript責務

```text
counter.js
├─ S-06 localStorage
├─ 目数／段数
├─ 直接入力
├─ 全角数字変換
├─ リセット
└─ localStorage利用不可対応

navigation.js
├─ ハンバーガーメニュー
├─ 外側クリック
├─ Esc
├─ logout dialog
└─ S-11未保存状態との共通連携

work-list.js
└─ S-09／S-10 category変更GET遷移

work-detail.js
├─ S-11基本情報dirty判定
├─ 未保存離脱dialog
├─ beforeunload
├─ allowPageLeave
├─ 作品削除dialog
├─ isDeleting
└─ 既存自動保存処理

register.js
├─ S-02入力状態
├─ 登録ボタン状態
└─ サーバーから返った既知重複状態の解除

password-toggle.js
└─ S-02／S-03 password表示／非表示

yarn-combination.js
├─ S-05モード切替
├─ 入力領域表示
├─ 計算ボタン状態
└─ 再計算案内
```

---

---

## 13. 環境設定・application設定設計

### application設定ファイル形式

設定ファイルは `.properties` 形式を採用する。

使用するファイルは以下のみを基本とする。

```text
src/main/resources/
├─ application.properties
├─ application-dev.properties
└─ application-prod.properties

src/test/resources/
└─ application-test.properties
```

方針：

- `.yml` / `.yaml` は使用しない。
- 共通値は `application.properties` に置く。
- profileごとに異なる値だけをprofile別propertiesへ置く。
- 同じ設定をdev / test / prodへ重複記載しない。
- `application-default.properties` は作成しない。
- `application-security.properties`、`application-db.properties` 等へ過剰分割しない。
- `spring.profiles.active` をpropertiesへhard-codeしない。

---

### Spring Profile

使用するProfileは次の3つのみとする。

```text
dev
test
prod
```

用途：

| Profile | 用途 |
|---|---|
| `dev` | Eclipse / ローカル開発 |
| `test` | 自動テスト |
| `prod` | 本番公開環境 |

方針：

- 第4のKnitting用default profileは作らない。
- `default=dev` のfallbackは作らない。
- dev / test / prodのいずれか1つだけを有効にする。
- Profileは環境差だけを扱い、業務ルールを変更しない。
- staging profileは初期版では設けない。

共通設定：

```properties
spring.profiles.default=none
```

これにより、active profile未指定時にSpring Bootの`default` profileへ依存しない。

通常起動：

```text
SPRING_PROFILES_ACTIVE=dev
```

または、

```text
SPRING_PROFILES_ACTIVE=prod
```

を外部から明示する。

Spring Contextを使用する自動テストは原則、

```java
@ActiveProfiles("test")
```

でtest profileを明示する。

さらに起動時に、

```text
Active Profile数 = 1
かつ
dev / test / prod のいずれか
```

であることを検証する。

以下は起動失敗とする。

```text
profileなし
dev,prod
dev,test
foo
```

---

### MySQL DataSource

Spring Boot標準のDataSource Auto Configurationを使用する。

独自 `DataSource` Beanは作成しない。

MySQL Connector/JをMaven runtime dependencyとして使用する。

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

versionはSpring Boot dependency managementへ任せる。

以下は設定しない。

```properties
spring.datasource.driver-class-name=...
```

`com.mysql.cj.jdbc.Driver` をJavaコードへ直接指定しない。

共通設定：

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

環境変数：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

dev標準DB名：

```text
knitting
```

test標準DB名：

```text
knitting_test
```

prodのhost / port / database名は公開環境から与える。

JDBC URLには、

```text
connectionTimeZone=Asia/Tokyo
```

を含める。

例：

```text
jdbc:mysql://localhost:3306/knitting?connectionTimeZone=Asia/Tokyo
```

username / passwordをJDBC URLへ埋め込まない。

初期版では以下を使用しない。

```text
useUnicode=true
characterEncoding=UTF-8
createDatabaseIfNotExist=true
forceConnectionTimeZoneToSession=true
```

MySQLは事前にdatabaseを用意し、その内部schemaをFlywayで管理する。

---

### DB接続情報・Secret管理

DB接続情報は外部環境変数で受け取る。

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

実値を以下へ直接記載しない。

- Java source
- `application.properties`
- profile別properties
- Git / GitHub
- README

fallbackも作らない。

禁止例：

```properties
spring.datasource.password=${DB_PASSWORD:password}
```

MySQL root userを通常アプリケーション接続ユーザーとして使用しない。

アプリ専用非rootユーザーを使用する。

初期版では、

- `.env`
- dotenv library
- Jasypt
- Vault

を必須導入しない。

prodの具体的なSecret Manager製品はhosting決定後に選択する。

---

### Flyway

Flywayはdev / test / prodすべてで有効とする。

同じmigration setを全環境で使用し、環境ごとのschema差分は作らない。

migration配置：

```text
src/main/resources/db/migration/
```

初期migration：

```text
V1__create_initial_schema.sql
```

適用済みmigrationは書き換えない。

DB構造変更時は、

```text
V2__
V3__
...
```

を追加する。

共通設定：

```properties
spring.flyway.enabled=true
spring.flyway.validate-migration-naming=true
spring.sql.init.mode=never
```

以下は使用しない。

- `schema.sql`
- `data.sql` によるschema管理
- test専用schema migration
- 自動Flyway clean
- `baseline-on-migrate=true`

FlywayとJPAは同じDataSourceを使用する。

```text
spring.flyway.url
spring.flyway.user
spring.flyway.password
```

は設定しない。

初期版ではmigration専用DBユーザーとruntime専用DBユーザーを分離しない。

起動順の考え方：

```text
DataSource
↓
Flyway migrate
↓
Hibernate / JPA validate
↓
Application startup
```

migration失敗時はアプリ起動失敗とする。

---

### JPA / Hibernate

HibernateはSpring Bootが提供するものを使用する。

dev / test / prod共通：

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.show-sql=false
```

DB schemaの作成・変更はFlywayのみが担当する。

Hibernateはschemaを検証する。

以下は使用しない。

```text
update
create
create-drop
```

OSIVは無効化する。

```properties
spring.jpa.open-in-view=false
```

Entityのlazy access、必要な関連取得、Entity→DTO変換はServiceのtransaction内で完了させる。

MySQL Dialectは自動判定に任せる。

```properties
spring.jpa.database-platform=...
```

は指定しない。

初期版では以下を追加しない。

- custom naming strategy
- Hibernate second-level cache
- global bind parameter logging
- `hibernate.jdbc.batch_size`
- `hibernate.order_inserts`
- `hibernate.order_updates`
- `hibernate.generate_statistics`
- `hibernate.format_sql`
- `hibernate.jdbc.time_zone`

---

### 画像保存設定

論理property名：

```text
knitting.image.storage-root
```

この値はローカルファイル保存方式における画像保存ルートの親ディレクトリを表す。

最終保存先：

```text
<storage-root>/works/<UUID>.<ext>
```

DBにはstorage-rootを保存しない。

DBの`photo_reference`は、

```text
works/<UUID>.jpg
works/<UUID>.png
```

を維持する。

#### dev

`application-dev.properties`：

```properties
knitting.image.storage-root=./data/images
```

保存例：

```text
Knitting/
└─ data/
   └─ images/
      └─ works/
         └─ <UUID>.<ext>
```

`.gitignore`：

```gitignore
/data/
```

#### test

`application-test.properties`：

```properties
knitting.image.storage-root=${java.io.tmpdir}/knitting-test-images
```

画像I/Oテストでは必要に応じてtest / test run単位のtemporary directoryへoverrideする。

dev / prod storageをtestから使用しない。

#### prod

productionの物理保存方式は公開環境決定後に確定する。

persistent local filesystem採用時：

```properties
knitting.image.storage-root=${IMAGE_STORAGE_ROOT}
```

を使用する。

Object Storage採用時：

- `knitting.image.storage-root` をbucket名等へ流用しない。
- object storage専用設定を別途設計する。
- DBの`photo_reference`論理形式は変更しない。

#### ローカルファイル保存時の起動確認

ローカルファイル実装が有効な環境では、起動時に以下を確認する。

```text
storage-root未設定 / blank
→ 起動失敗

root不存在
→ 作成

works不存在
→ 作成

本来directoryの場所に通常file
→ 起動失敗

read / write不能
→ 起動失敗
```

起動順：

```text
storage-root / works確認・作成
↓
read/write確認
↓
24時間以上のstale .tmp清掃
↓
通常起動
```

Object Storage採用時は、このfilesystem確認を適用しない。

---

### Multipart設定

画像の業務上限は既存画像設計どおり、

```text
10 MiB
= 10,485,760 bytes
```

とする。

Servlet Multipart側は少し大きく設定し、10MiB付近の業務Validationをアプリケーション側まで到達させる。

共通設定：

```properties
spring.servlet.multipart.max-file-size=11MB
spring.servlet.multipart.max-request-size=12MB
spring.servlet.multipart.file-size-threshold=0B
```

意味：

```text
業務上限
→ 10MiB

Servlet単一file上限
→ 11MB

request全体上限
→ 12MB
```

`file-size-threshold=0B` によりMultipart fileをメモリへ全保持するのではなくcontainerのtemporary storageを利用する。

以下は設定しない。

```properties
spring.servlet.multipart.location=...
```

Knitting独自の元upload用temporary directoryは作らない。

`<storage-root>/temp` も作らない。

Servlet / container上限を超えたrequestはForm binding前に失敗する場合がある。

その場合は共通例外処理で、

```text
「画像は10MB以下のファイルを選択してください。」
```

の趣旨を案内する。

このinfrastructure error pathでは、

- 通常の`photo` field error化
- 他Form項目の入力保持

を保証しない。

内部exception、request body、画像bytes、元ファイル名等はユーザー表示・ログ出力しない。

reverse proxy側upload limitは公開環境決定後に調整する。

---

### HttpSession

標準 `HttpSession` を使用する。

Spring Session / Redis / DB Sessionは初期版では導入しない。

共通設定：

```properties
server.servlet.session.timeout=30m
server.servlet.session.persistent=false
server.servlet.session.tracking-modes=cookie
```

Session timeoutは最後のSession関連activityから30分の非アクティブ時間とする。

Sessionを再起動・再デプロイ後へ永続化しない。

URL rewriting：

```text
;jsessionid=
```

は使用せず、Cookie追跡のみとする。

Spring Security側は第5章どおり、

```text
SessionCreationPolicy.IF_REQUIRED
Session Fixation = changeSessionId
Concurrent login = 許可
```

とする。

`maximumSessions(1)` 等は設定しない。

timeout警告UIやSession維持専用heartbeatは作らない。

Remember-Me 30日とHttpSession 30分は別仕様とする。

---

### JSESSIONID Cookie

共通設定：

```properties
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax
```

Cookie名は標準の、

```text
JSESSIONID
```

を使用する。

Domainは設定しない。

PathはServlet context pathを使用する。

Max-Ageは設定せず、非永続Session Cookieとする。

Secure属性：

dev：

```properties
server.servlet.session.cookie.secure=false
```

test：

```properties
server.servlet.session.cookie.secure=false
```

prod：

```properties
server.servlet.session.cookie.secure=true
```

---

### Remember-Me

方式：

```text
token-based Remember-Me
```

Persistent Token Repository方式は使用しない。

Remember-Me DB tableは作らない。

有効期間：

```text
30日
= 2,592,000秒
```

SecurityConfigで、

```java
tokenValiditySeconds(2_592_000)
```

を明示する。

使用するたびに期限を延長するsliding方式にはしない。

#### Remember-Me key

logical property：

```text
knitting.security.remember-me-key
```

環境変数：

```text
REMEMBER_ME_KEY
```

共通設定：

```properties
knitting.security.remember-me-key=${REMEMBER_ME_KEY}
```

fallbackは作らない。

dev / test / prodで別々のkeyを使用する。

同一prod環境の複数instanceでは同じkeyを共有する。

keyは、

```text
cryptographically secure random
最低32bytes / 256bit程度
```

とする。

起動時に、

- 未設定
- blank
- 明らかに短すぎる

場合は失敗させる。

ただしアプリがentropyそのものを完全判定することはしない。

Secret本体をValidation message / exception / logへ出力しない。

同一環境では通常再起動・再デプロイ・version updateでkeyを維持する。

漏えいまたは疑い時にkeyを交換し、既存Remember-Me Cookieが無効になることを許容する。

#### Remember-Me Cookie

Cookie名：

```text
remember-me
```

HttpOnly：

```text
true
```

SameSite：

```text
Lax
```

Secure：

```text
dev  = false
test = false
prod = true
```

custom property：

```text
knitting.security.remember-me-secure-cookie
```

profile別設定：

```properties
# dev
knitting.security.remember-me-secure-cookie=false
```

```properties
# test
knitting.security.remember-me-secure-cookie=false
```

```properties
# prod
knitting.security.remember-me-secure-cookie=true
```

Java側ではSpring Securityの`useSecureCookie(...)`へ渡す。

SameSite=LaxはSpring Bootの`CookieSameSiteSupplier`等を利用して`remember-me` Cookieへ適用する。

---

### HTTPS

productionではHTTPSを必須とする。

ただし、hosting環境未決定のため現段階では、

```properties
server.ssl.*
```

を設定しない。

TLS終端は、

```text
PaaS
Reverse Proxy
Load Balancer
Spring Boot自身
```

のどこで行うかを公開環境決定後に確定する。

HTTP→HTTPS redirectの実装場所も公開環境決定後に確定する。

prodのSecure Cookie属性は、Proxy設定未完成を理由にfalseへ弱めない。

---

### Logging

Spring Boot標準Logging Systemを使用する。

初期版では、

- 独自Logging framework
- `logback.xml`
- `logback-spring.xml`
- custom appender

を必須導入しない。

共通：

```properties
logging.level.root=INFO
```

test：

```properties
logging.level.root=WARN
```

devも通常はINFOとする。

開発中の調査時のみ、対象loggerを限定してDEBUGへ上げる。

以下は使用しない。

```properties
debug=true
```

SQL常時出力もしない。

```properties
spring.jpa.show-sql=false
```

Hibernate bind parameter TRACEを恒久的に有効化しない。

Spring Security DEBUG / TRACEも常時有効化しない。

初期版はconsole loggingのみとする。

以下は現時点で設定しない。

```text
logging.file.name
logging.file.path
Logback rolling policy
custom console pattern
JSON structured logging
Tomcat access log
```

ログへ出さないもの：

- password
- password hash
- Remember-Me token
- Remember-Me key
- Session ID
- CSRF token
- Cookie全体
- Authorization header
- request body全文
- Form全体
- 画像bytes
- 元画像ファイル名
- 不要な個人情報

---

### Reverse Proxy / Forwarded Header

Forwarded Headerを無条件に信用しない。

共通初期設定：

```properties
server.forward-headers-strategy=none
```

dev / testは通常`NONE`。

現在のprodもhosting未決定のため`NONE`を継承する。

productionでReverse Proxy / Load Balancerを使用する場合は公開前に再確定する。

第一候補：

```properties
server.forward-headers-strategy=native
```

`NATIVE`を採用する場合は、実際のProxy networkに合わせてtrusted/internal proxy範囲を制限する。

全Proxyを信用する設定はprodで使用しない。

`NATIVE`で要件を満たせない場合のみ、

```properties
server.forward-headers-strategy=framework
```

を第二候補とする。

ただしedge proxyが外部からの、

```text
Forwarded
X-Forwarded-*
```

を除去・上書きすることを前提とする。

KnittingのJavaコードから、

```java
request.getHeader("X-Forwarded-For")
```

を直接読み取りclient IPとしない。

Login / Registration Rate Limit用のclient IPは、trusted proxy処理後のrequest情報を利用する。

---

### Security Headers / CSP

Spring Security標準Security Headersを維持する。

Security Headers全体を無効化しない。

X-Frame-Options：

```text
DENY
```

CSPでも、

```text
frame-ancestors 'none'
```

とする。

Knitting初期版のCSP基準policy：

```text
default-src 'self';
script-src 'self';
style-src 'self';
img-src 'self' data: blob:;
connect-src 'self';
font-src 'self';
object-src 'none';
base-uri 'self';
frame-ancestors 'none';
form-action 'self';
```

これは**基準・目標policy**とする。

dev / testでも可能な範囲で適用し、実装段階から違反を発見する。

production公開前に、

- S-01〜S-11
- JavaScript
- CSS
- 画像preview
- S-11 fetch
- login / registration
- navigation

を実ブラウザーで確認し、必要最小限の修正後に最終directiveを確定する。

以下を動作させるだけの理由で追加しない。

```text
'unsafe-inline'
'unsafe-eval'
*
```

外部CDN / script / font等を将来追加する場合は、必要なoriginだけを個別reviewする。

HSTSはSpring Security標準を維持し、production HTTPS環境で有効にする。

`preload`は現時点では追加しない。

Reverse Proxy / TLS構成確定後にHSTSの実responseをSecurity Integration Testで確認する。

---

### Error Response Security

内部情報をSpring Boot標準error responseへ露出させないことを明示する。

共通設定：

```properties
spring.web.error.include-message=never
spring.web.error.include-stacktrace=never
spring.web.error.include-binding-errors=never
spring.web.error.include-exception=false
```

これにより、

- exception message
- stack trace
- binding detail
- exception class

を標準error responseへ出さない。

既存のControllerAdvice / error template設計はそのまま維持する。

---

### custom `knitting.*` property設計

Spring Boot標準の、

```text
spring.*
server.*
logging.*
```

はSpring Bootへ直接設定する。

Knitting独自のpropertyは`@ConfigurationProperties`で管理する。

`@Value`や`Environment#getProperty()`を各クラスへ分散させない。

主なcustom property：

```text
knitting.image.storage-root
knitting.security.remember-me-key
knitting.security.remember-me-secure-cookie
```

#### Security properties

```java
@ConfigurationProperties("knitting.security")
@Validated
public record KnittingSecurityProperties(
        @NotBlank String rememberMeKey,
        @NotNull Boolean rememberMeSecureCookie
) {
}
```

`rememberMeSecureCookie`はprimitive `boolean`ではなく`Boolean + @NotNull`とし、未設定を暗黙`false`にしない。

Remember-Me keyは未設定 / blankをValidationで拒否する。

最低強度の追加確認はSecret値をerror messageへ出さない明示的なValidationで行う。

#### Image storage properties

Object Storageへの将来移行を妨げないため、`storageRoot`へglobal `@NotBlank`を付けない。

```java
@ConfigurationProperties("knitting.image")
public record ImageStorageProperties(
        String storageRoot
) {
}
```

ローカルファイル実装が有効な環境でのみ、

```text
null
blank
invalid path
directory作成不能
read/write不能
```

をstorage初期化時に起動失敗とする。

Object Storage採用時は`storageRoot`を必須としない。

#### 登録方法

main application class：

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class KnittingApplication {
}
```

ConfigurationProperties recordには`@Component`を付けない。

利用側へconstructor injectionする。

Security propertiesを不要なController / Serviceへ伝播させない。

property名はlowercase kebab-caseに統一する。

---

### Configuration Metadata

custom property metadata生成のため、

```text
spring-boot-configuration-processor
```

をannotation processorとして使用する。

runtime dependencyとはしない。

Properties recordには用途を説明する簡潔なJavadocを付与する。

Secret実値や具体的な本番SecretをJavadocへ書かない。

---

### 環境変数一覧

初期版の通常起動で使用する環境変数：

```text
SPRING_PROFILES_ACTIVE
DB_URL
DB_USERNAME
DB_PASSWORD
REMEMBER_ME_KEY
```

productionでpersistent local filesystemを採用した場合のみ：

```text
IMAGE_STORAGE_ROOT
```

を追加する。

一覧：

| 環境変数 | 用途 | dev | test | prod |
|---|---|---:|---:|---:|
| `SPRING_PROFILES_ACTIVE` | Profile選択 | 必須 | 原則`@ActiveProfiles` | 必須 |
| `DB_URL` | JDBC URL | 必須 | DB統合テストで必須 | 必須 |
| `DB_USERNAME` | DB username | 必須 | DB統合テストで必須 | 必須 |
| `DB_PASSWORD` | DB password | 必須 | DB統合テストで必須 | 必須 |
| `REMEMBER_ME_KEY` | Remember-Me key | 必須 | Security Context testで必要 | 必須 |
| `IMAGE_STORAGE_ROOT` | prod local image root | 不使用 | 不使用 | local FS採用時のみ |

現時点では以下を必須環境変数にしない。

```text
PORT
SERVER_PORT
TZ
Object Storage credentials
Proxy-specific values
```

hosting決定後に必要なものだけ追加する。

---

### dev環境の外部設定

Eclipse Run Configuration等から、

```text
SPRING_PROFILES_ACTIVE=dev
DB_URL=...
DB_USERNAME=...
DB_PASSWORD=...
REMEMBER_ME_KEY=...
```

を渡す。

Windows全体のsystem environment variableへ登録する必要はない。

Secret入りRun ConfigurationをGit共有しない。

---

### test環境

Spring Context test：

```java
@ActiveProfiles("test")
```

を使用する。

test DBはdev / prod DBから完全分離する。

```text
knitting_test
```

を標準論理名とする。

Security testでは本番Remember-Me keyを使用せず、test専用synthetic keyを与える。

画像保存先はtest専用temporary areaを使用する。

---

### prod環境

productionでは、

- HTTPS必須
- Secure Cookie必須
- production DB
- production専用Remember-Me key
- production hostingのSecret機能

を使用する。

公開環境決定後に追加確定するもの：

- TLS termination
- HTTP→HTTPS redirect
- Forwarded Header
- trusted proxy
- client IP取得
- Rate Limit物理実装
- production Secret Manager
- production image storage
- persistent disk / Object Storage
- backup / capacity
- object storage credentials
- reverse proxy upload limit
- access log取得場所
- dynamic port
- HSTS最終確認
- CSP最終directive
- production Security Integration Test

これらは詳細設計12の未完成を意味せず、インフラ依存事項として公開環境選定後に決定する。

---

### dev / test / prod差分

| 設定 | dev | test | prod |
|---|---|---|---|
| Profile | `dev` | `test` | `prod` |
| Profile指定 | env | `@ActiveProfiles`中心 | env |
| DB | `knitting` | `knitting_test` | production DB |
| Flyway | ON | ON | ON |
| Hibernate ddl | validate | validate | validate |
| OSIV | OFF | OFF | OFF |
| Session timeout | 30m | 30m | 30m |
| JSESSIONID Secure | false | false | true |
| Remember-Me Secure | false | false | true |
| Remember-Me key | dev専用 | test専用 | prod専用 |
| Logging root | INFO | WARN | INFO |
| Image storage | `./data/images` | temp | hosting依存 |
| Forwarded Header | NONE | NONE | hosting決定後 |
| HTTPS | local HTTP可 | test用途 | 必須 |
| CSP | 基準policy | 基準policy | 公開前最終確認 |
| HSTS | HTTPならなし | test条件依存 | HTTPSで確認 |

---

### `application.properties` 最終基準

```properties
# ==================================================
# Profile
# ==================================================
spring.profiles.default=none


# ==================================================
# DataSource
# ==================================================
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}


# ==================================================
# Flyway
# ==================================================
spring.flyway.enabled=true
spring.flyway.validate-migration-naming=true
spring.sql.init.mode=never


# ==================================================
# JPA / Hibernate
# ==================================================
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.show-sql=false


# ==================================================
# Multipart
# ==================================================
spring.servlet.multipart.max-file-size=11MB
spring.servlet.multipart.max-request-size=12MB
spring.servlet.multipart.file-size-threshold=0B


# ==================================================
# Session
# ==================================================
server.servlet.session.timeout=30m
server.servlet.session.persistent=false
server.servlet.session.tracking-modes=cookie

server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax


# ==================================================
# Reverse Proxy / Forwarded Headers
# ==================================================
server.forward-headers-strategy=none


# ==================================================
# Error Response Security
# ==================================================
spring.web.error.include-message=never
spring.web.error.include-stacktrace=never
spring.web.error.include-binding-errors=never
spring.web.error.include-exception=false


# ==================================================
# Logging
# ==================================================
logging.level.root=INFO


# ==================================================
# Knitting Security
# ==================================================
knitting.security.remember-me-key=${REMEMBER_ME_KEY}
```

---

### `application-dev.properties`

```properties
# ==================================================
# Image Storage
# ==================================================
knitting.image.storage-root=./data/images


# ==================================================
# Cookie
# ==================================================
server.servlet.session.cookie.secure=false
knitting.security.remember-me-secure-cookie=false
```

---

### `application-test.properties`

```properties
# ==================================================
# Image Storage
# ==================================================
knitting.image.storage-root=${java.io.tmpdir}/knitting-test-images


# ==================================================
# Cookie
# ==================================================
server.servlet.session.cookie.secure=false
knitting.security.remember-me-secure-cookie=false


# ==================================================
# Logging
# ==================================================
logging.level.root=WARN
```

---

### `application-prod.properties`

現時点で固定する設定：

```properties
# ==================================================
# Cookie
# ==================================================
server.servlet.session.cookie.secure=true
knitting.security.remember-me-secure-cookie=true
```

persistent local filesystem採用時のみ追加：

```properties
# ==================================================
# Image Storage
# ==================================================
knitting.image.storage-root=${IMAGE_STORAGE_ROOT}
```

Reverse Proxy採用時は公開環境決定後に、

```properties
server.forward-headers-strategy=native
```

を第一候補として追加する。

必要な場合のみ`FRAMEWORK`を選択する。

---

### Profileによって変更しない仕様

以下はprofile差分にしない。

- Form Validation
- 作品カテゴリー
- 作品ステータス
- カウンター0〜9999
- メモ最大1000文字
- 画像業務上限10MiB
- 画像最大8192px
- 画像resize長辺1920px
- Remember-Me 30日
- Session timeout 30分
- DB schema
- Flyway migration
- CSP基準policy
- Security認証・認可ルール
- Service業務ルール

Profileは環境設定だけを切り替える。

---

### Maven実装前確認

Jakarta Bean ValidationをFormおよびConfigurationProperties Validationで使用するため、MavenにValidation実装が存在することを確認する。

未導入の場合は、

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

を追加する。

既に導入済みなら追加変更不要。

また、FlywayについてはSpring Boot 4系の採用構成として、

```text
spring-boot-starter-flyway
org.flywaydb:flyway-mysql
```

を使用し、versionはSpring Boot dependency managementに任せる。

---

### 起動失敗とする主な設定不備

以下はfail fastとする。

```text
Active Profileが0件
Active Profileが2件以上
Active Profileがdev/test/prod以外

DB_URL未設定
DB_USERNAME未設定
DB_PASSWORD未設定

REMEMBER_ME_KEY未設定
REMEMBER_ME_KEY blank
REMEMBER_ME_KEY明らかに短すぎる

remember-me-secure-cookie未設定

local image storage方式でstorage-root未設定
local image storage方式でstorage-root blank
local image storage方式でdirectory利用不能

Flyway migration失敗
Hibernate schema validation失敗
```

---

---

## 14. テスト設計

### 13-1 テスト全体構成・テストレベルごとの実行方式

#### テストレベル

Knittingでは、責務に応じて次のテストレベルを使い分ける。

```text
Form / Validator単体
Controller / MockMvc
Service単体
Repository / DB統合
画像・ファイル統合
Security
実ブラウザー受入確認
```

#### 基本方針

1. Form、Validator、計算ロジック等、Spring Contextを必要としないものは軽量な単体テストを基本とする。
2. ControllerおよびSpring MVCのWeb動作はMockMvcを中心に確認する。
3. Repository、JPA Mapping、Flyway、MySQL固有制約、PESSIMISTIC_WRITE等はmockのみで済ませず、実MySQLによる統合テストを行う。
4. 画像・ファイルI/Oは必要に応じて実ファイルを使用した統合テストを行う。
5. JavaScriptは初期版で専用フロントエンドテスト基盤を導入せず、実ブラウザー受入確認を使用する。
6. すべてのテストをfull ApplicationContextで実行しない。
7. テスト対象は、外部仕様、境界、Security、データ整合性、主要業務ルールを優先する。

---

### 13-2 テスト用Maven依存関係・テストライブラリ

#### 基本依存関係

Spring Boot 4.0.8の公式test starter群を使用する。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### 依存関係ルール

- すべて`test` scopeとする。
- JUnit Jupiterを使用する。
- JUnit 4 / Vintageは使用しない。
- JUnit Jupiter、Mockito、AssertJ等を個別に重複指定しない。
- Spring Bootのdependency management対象は個別versionを固定しない。
- H2は初期版で追加しない。
- Selenium、Playwright、Cypress、REST Assured、WireMock、JaCoCo等は初期版で追加しない。
- 「将来使うかもしれない」という理由だけでtest libraryを追加しない。

---

### 13-3 DB統合テスト方式・MySQLテスト環境

#### 採用方式

DB統合テストには、実際のMySQL Community 8.4 LTS系列を使用する。

```text
test DB標準論理名：
knitting_test
```

#### 方針

1. H2等の代替インメモリDBを使用しない。
2. 初期版ではTestcontainersを導入しない。
3. ローカルMySQL上に`knitting_test`を用意する。
4. `knitting_test`はdev用`knitting`およびproduction DBから完全に分離する。
5. DB統合テストは`test` Profileで実行する。
6. DB接続情報は外部から与える。

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

7. JDBC URLには既存仕様どおり`connectionTimeZone=Asia/Tokyo`を含める。
8. DB schemaはFlywayで管理する。
9. Hibernateは`ddl-auto=validate`を維持する。
10. Repository、Entity Mapping、Flyway、UNIQUE / CHECK / FK、owner条件付き取得、PESSIMISTIC_WRITE等を実MySQLで確認する。
11. H2への自動置換を前提としたRepositoryテスト構成にしない。
12. Testcontainersは、将来CIや再現性向上が必要になった場合の候補とする。

---

### 13-4 test Profile・Flyway・JPA・DB初期化方針

#### test Profile

Spring Contextを使用するテストでは原則として次を使用する。

```java
@ActiveProfiles("test")
```

Spring Contextを使用しない純粋な単体テストでは、test Profileを必須としない。

#### DB初期化

1. `knitting_test` database自体は事前に用意する。
2. database内部のテーブル、制約、インデックス等はFlywayが管理する。
3. testでもFlywayを有効にする。
4. dev / test / prodで同じ本番用migrationを使用する。
5. `src/main/resources/db/migration`をDB schemaの唯一の正とする。
6. test専用の別schema定義を作らない。
7. Hibernateによるschema生成・更新は行わない。
8. testでも`spring.jpa.hibernate.ddl-auto=validate`を維持する。
9. `schema.sql`は使用しない。
10. `data.sql`による一括fixture投入も使用しない。
11. `spring.sql.init.mode=never`を維持する。
12. test専用Flyway migrationをfixture投入目的で作成しない。
13. 通常テスト実行前に毎回`Flyway clean`する方式は採用しない。
14. Flyway migration失敗時はApplicationContext起動失敗とする。
15. `@DataJpaTest`を使用する場合もembedded DBへ置換せず、`knitting_test`を使用する。
16. その場合は`@AutoConfigureTestDatabase(replace = Replace.NONE)`相当の設定を使用する。
17. テストデータの投入・rollback・cleanup方法は13-7の方針に従う。

---

### 13-5 Controller / MockMvc / Securityテストの構成

#### 2段構成

Web層は次の2段構成とする。

```text
Controller / Web slice
→ @WebMvcTest + MockMvc

Security横断統合
→ @SpringBootTest
   + @AutoConfigureMockMvc
   + @ActiveProfiles("test")
```

#### Controller / Web slice

主に次を確認する。

- URL Mapping
- Form Binding
- Bean Validation
- typeMismatch
- Model
- view
- redirect
- Flash Attribute
- 入力保持
- Service呼出 / 未呼出
- JSON / HTMLレスポンス変換

Controller依存のService等はmock化し、Service業務ロジックをControllerテストで再テストしない。

通常HTML Formでは、

```text
正常入力
→ Service呼出
→ redirect

Validation / typeMismatch
→ 同画面再表示
→ field error
→ Service未呼出
```

を基本とする。

S-11非同期Controllerでは、200 / 400 / 409等のController・AdviceによるHTTP変換を確認する。

#### Security統合テスト

実際のSecurityFilterChainを使用して最低限次を確認する。

- 公開URL
- 保護URL
- Login成功 / 失敗
- CSRF正常 / 不正
- S-11未認証401
- S-11 CSRF不正403
- owner作品操作
- 他人作品操作拒否
- Logout
- Remember-Me ON / OFF
- Principalの安全性
- `/error`とSecurityの統合

Form Loginそのものを`@WithMockUser`だけで代替しない。

既認証状態だけが前提のテストでは、Spring Security Testによるmock authenticationを使用してよい。

KnittingUserPrincipalからuserIdを取得する処理が関係する代表テストでは、KnittingUserPrincipal相当のPrincipalを使用する。

test fixture helper名やcustom SecurityContext生成方法は実装時に決定してよい。

---

### 13-6 Service単体テスト・mock使用方針

#### 基本構成

Service業務ロジックは、JUnit Jupiter + Mockitoを基本とした軽量な単体テストで確認する。

- Spring ApplicationContextは原則起動しない。
- MySQL、Flyway、test Profileを必須としない。
- テスト対象Service自身は実インスタンスを使用する。
- Repository等の外部依存を必要に応じてmock化する。

主なmock対象候補：

```text
UserRepository
WorkRepository
PasswordEncoder
画像処理・storage担当component
SubmissionTokenManager
```

#### mock方針

- Entity、DTO、Enum、値オブジェクトは原則mock化しない。
- 日時依存テストは`Clock.fixed()`を使用する。
- deep stubは原則使用しない。
- Repositoryが返すEntityまで機械的にmock化しない。
- privateメソッドをreflection等で直接テストしない。

#### 主な確認対象

- 登録重複
- Password Blocklist
- createWork初期値
- owner判定
- DTO変換
- status / completionDate連動
- expected値比較
- 同値再送
- lastUpdatedAt更新判断
- snapshot競合
- submissionToken処理
- Optional処理
- 画像DB失敗時の補償処理
- commit後画像削除の業務オーケストレーション
- 固定Clockによる日時

#### DBとの境界

Mockito単体テストによって、次を保証したとは扱わない。

- 実DB transaction commit / rollback
- transaction isolation
- PESSIMISTIC_WRITEの実ロック
- DB制約
- Repository query
- JPA Mapping
- Flyway

これらは実MySQL統合テストで保証する。

---

### 13-7 Repository・DB統合テストのデータ作成・分離・後処理

#### テストデータ

1. 各テストが必要なデータだけを作成する。
2. `knitting_test`へ固定の共通テストデータを常駐させない。
3. テスト同士で作成済みUser / Workを共有しない。
4. 通常のfixture作成は実Entity + 実Repositoryを基本とする。
5. 小さなJava Test Fixture / Test Data Factoryを使用してよい。
6. 巨大な共通fixtureは作らない。
7. 通常fixture作成のために大量の`@Sql`やINSERT SQLを標準方式にしない。
8. Entityでは表現困難なDB制約確認のみ、JdbcTemplate / native SQL等を必要最小限使用してよい。

#### rollback

通常の`@DataJpaTest`系Repositoryテストでは、transactional rollbackを基本とする。

通常テストの後処理として手動DELETEを標準方式にしない。

#### ID

- IDはAUTO_INCREMENTへ任せる。
- 固定IDを前提としない。
- 保存後に生成されたIDを取得してassertや検索に使用する。
- AUTO_INCREMENT値の連続性をテスト条件にしない。
- cleanup時にもAUTO_INCREMENTを原則リセットしない。

#### UNIQUE項目

username / email等は、同一テスト内で衝突しない明確な値を使用する。

不要なランダムUUID依存を避け、失敗時に理解しやすい決定的なテスト値を基本とする。

#### flush

DB制約をその場で確認する場合は、`saveAndFlush()`または明示`flush()`を使用する。

#### PESSIMISTIC_WRITE等

PESSIMISTIC_WRITE競合は独立した複数の実DB transactionを使用する。

```text
Transaction A
Transaction B
```

を実際に分離して確認する。

commit後状態、rollbackそのもの、複数transaction競合等の特殊テストでは、通常の自動rollbackだけに依存しない。

必要に応じてcommitして検証し、テスト後に明示cleanupする。

明示cleanup時は外部キー関係に従い、

```text
works
↓
users
```

の順で削除する。

schema、table、database自体は削除しない。

DB統合テストは初期版で並列実行を前提としない。

---

### 13-8 画像・ファイル統合テスト方式

#### 基本方針

画像・ファイル機能はmockのみで済ませず、実ファイルI/Oを用いるテストを必須とする。

dev / prod storage-rootは使用しない。

testではtemporary storage-rootを使用する。

`application-test.properties`の基準値：

```properties
knitting.image.storage-root=${java.io.tmpdir}/knitting-test-images
```

実画像I/Oテストでは必要に応じてテストクラスまたはtest run単位のtemporary directoryへoverrideする。

JUnit Jupiterの`@TempDir`等を第一候補とし、新しい仮想filesystem libraryは追加しない。

#### fixture

配置：

```text
src/test/resources/images/fixtures/
```

最低限：

- real JPEG
- real PNG
- Orientation JPEG
- metadata-bearing JPEG
- corrupt image

実ユーザー画像、個人情報、不要なGPS/EXIF情報をfixtureへ使用しない。

小さく再現可能なfixtureはGit管理してよい。

10MiB、8192px等の巨大境界データは原則テスト時生成する。

#### 主な確認対象

- JPEG / PNG
- extension / MIME / actual format
- corrupt image
- 10MiB境界
- 8192px境界
- resize
- Orientation補正
- metadata除去
- `.tmp` → final切替
- move失敗
- 起動時tmp cleanup
- path traversal
- 差し替え
- 削除
- DB失敗時candidate補償削除
- DB photo_referenceと実ファイルの整合
- default画像の保護

保存後画像は必要に応じて再読込し、実形式、dimensions、Orientation、metadata等を確認する。

ライブラリ内部実装そのものではなく、Knittingから観測可能な結果をテストする。

#### DB rollbackとの違い

DB transactionがrollbackしてもfilesystemはrollbackされない。

そのため画像テストでは、temporary directory破棄または明示cleanupによりファイル側も分離する。

---

### 13-9 Thymeleaf・JavaScript・実ブラウザー受入確認

#### 基本方針

初期版では以下を新規導入しない。

```text
Selenium
Playwright
Cypress
Jest
Vitest
jsdom
```

サーバー側で確認可能なThymeleaf仕様はMockMvc等で自動テストする。

DOM操作、Browser API、localStorage、dialog、beforeunload、focus、debounce、fetch等は実ブラウザー受入確認を行う。

#### 主ブラウザー

開発時点の最新安定版Google Chromeを主な確認環境とする。

特定version番号へ設計を固定しない。

Chrome DevToolsの以下を使用してよい。

```text
Network
Application
Console
responsive viewport
```

初期版では全主要ブラウザーを対象とした大規模クロスブラウザーテスト表を必須としない。

#### 主な受入確認

##### S-06 簡易カウンター

- localStorage保存・読込
- 再読み込み後の復元
- 直接入力
- 全角数字処理
- 上下限
- 個別reset
- 全reset
- 不正保存値復旧
- localStorage利用不可時

##### S-05

- モード切替
- 入力値維持
- hidden切替
- 再計算案内

##### 共通ナビゲーション

- hamburger初期close
- open / close
- 外側click
- Esc
- aria-expanded
- focus復帰

##### Logout

- dialog表示
- キャンセル時POSTなし
- 確定時POST `/logout`
- S-11未保存警告統合
- beforeunloadとの二重確認防止

##### S-11未保存状態

- 実変更時のみdirty
- 元値へ戻した場合dirty解除
- 通常リンク離脱
- 再読み込み
- 戻る / 進む
- タブ / ブラウザー終了
- browser標準beforeunload
- 作品削除dialog
- logout / delete /通常離脱の二重確認防止

##### Password toggle

- `password` / `text`切替
- 値維持
- submitされない
- 再表示時に非表示へ戻る
- Storageへ保存しない

##### S-11自動保存

- 初期表示時に不要POSTなし
- counter 300ms debounce
- memo 1秒debounce
- 中間値大量送信なし
- 同一チャンネル並列POSTなし
- 最新pending値送信
- 古いresponseで最新値を上書きしない
- 「保存しました」
- 約2秒後表示消去
- 5xx
- network failure
- 手動retry
- 400
- 401
- 403
- 409
- visibilitychange
- pagehide
- 二重送信防止

必要に応じてDevTools NetworkでPOST回数、送信タイミング、status等を確認する。

主要画面操作時にKnitting自身に起因する予期しないJavaScript errorがConsoleへ出ていないことも確認する。

productionコードへテスト専用endpointや隠し機能を追加しない。

---

### 13-10 テスト実行単位・Mavenからの実行・全体完了基準

#### Maven lifecycle

軽量テストとintegration testをMaven上でも分離する。

```text
軽量テスト
→ Maven Surefire Plugin
→ *Test

統合テスト
→ Maven Failsafe Plugin
→ *IT
```

JUnit Jupiterを共通のtest frameworkとして使用する。

Failsafeはintegration test実行用Maven Pluginとして導入する。

#### *Test

主に次を配置する。

- Form / Validator
- 計算ロジック
- Service単体
- Controller / WebMvcTest
- 小さい画像・file処理テスト

原則として外部MySQLを必要としない。

#### *IT

主に次を配置する。

- Repository / MySQL
- Flyway
- JPA Mapping
- DB制約
- PESSIMISTIC_WRITE
- 実transaction
- Security横断統合
- DB + file consistency
- `/error` + Security統合

#### 実行コマンド

普段の高速確認：

```bash
mvn test
```

全自動テストの正式確認：

```bash
mvn verify
```

大きな節目：

```bash
mvn clean verify
```

実装中はEclipseやMavenからtest method / class単位で実行してよい。

#### 実行タイミング

```text
実装中
→ 関連Testのみ

1機能完成
→ 関連Test + 関連IT

通常の一区切り
→ mvn test

Phase / 機能群完成
→ mvn verify

最終提出・公開準備
→ mvn clean verify
   + 実ブラウザー受入確認
```

#### 実ブラウザーチェックリスト

実ブラウザー受入項目はGit管理するMarkdownチェックリストとする。

厳密な保存pathはrepository内の既存documentation構成に合わせてよい。

通常開発中は変更箇所に関連する項目を確認し、実装全体完了・総合テスト・公開前には一通り実施する。

#### 完了条件

機能完成条件として次を求める。

1. 必要な`*Test`が成功する。
2. integration対象がある場合は必要な`*IT`が成功する。
3. 自動テスト全体として`mvn verify`が成功する。
4. JavaScript・実画面動作を含む対象では必要な実ブラウザー受入確認が成功する。
5. 必須仕様テストを`@Disabled`のままにしない。
6. test failureを隠すためにassertを弱めたり、必須testを削除したりしない。

初期版ではline coverage / branch coverageの数値目標を設定しない。

JaCoCo等のcoverage toolも初期版の必須構成には追加しない。

テスト数やcoverage率よりも、外部仕様、Security boundary、DB制約、データ整合性、主要業務ルールが確認できていることを重視する。

GitHub Actions等のCIは詳細設計13の初期範囲には含めない。

将来CIを導入する場合でも、`mvn verify`を全自動テストの基本entry pointとして再利用できる構成とする。

---

### 詳細設計01〜12との最終対応

#### 対応方針

本章は、詳細設計01〜12で確定済みのテストケースを別仕様へ変更せず、テストレベル・実行方式・環境を統一する。

対応は次のとおり。

| 詳細設計 | 主な確認対象 | 詳細設計13での主な対応 |
|---|---|---|
| 01 全体方針・技術構成 | レイヤ責務、共通技術方針 | 13-1、13-2、13-10 |
| 02 URL・ルーティング | URL、GET/POST、redirect、公開/保護URL | 13-5、13-9 |
| 03 DB・JPA | Entity、Repository、Flyway、制約、Mapping | 13-3、13-4、13-7 |
| 04 認証・認可 | Login、Logout、CSRF、Remember-Me、owner | 13-5 |
| 05 画像保存 | JPEG/PNG、resize、path、file/DB整合 | 13-8 |
| 06 自動保存・非同期更新 | debounce、競合、DB lock、pending | 13-6、13-7、13-9 |
| 07 共通例外・エラー | 400/401/403/404/409/500、Advice、/error | 13-5、13-6、13-7、13-9 |
| 08 Form・Validation | Bean Validation、Binding、入力保持 | 13-5、13-6、13-9 |
| 09 Controller・Service | Controller、Service、transaction、Clock | 13-5、13-6、13-7、13-8 |
| 10 計算ロジック | BigDecimal、HALF_UP、境界値 | 13-1、13-6、13-10 |
| 11 Thymeleaf・JavaScript | localStorage、dialog、dirty、画面動作 | 13-5、13-9 |
| 12 環境設定 | test Profile、DB分離、storage分離、fail-fast | 13-3、13-4、13-8、13-10、13-11 |

#### 詳細設計12 fail-fastテスト補完

詳細設計12で確定済みのfail-fast仕様を正式なテスト対象とする。

代表対象：

```text
Active Profile未指定
Active Profile複数
不正Profile

DB_URL不足
DB_USERNAME不足
DB_PASSWORD不足

REMEMBER_ME_KEY不足
REMEMBER_ME_KEY blank
REMEMBER_ME_KEY短すぎ

remember-me secure cookie設定不備

local image storage-root不足
storage-root blank
storage directory利用不能

Flyway migration失敗
Hibernate schema validation失敗
```

Knitting独自の設定Validationについて、

```text
正常設定
→ 起動成功

代表的な不正・不足設定
→ fail fast
```

を自動テストする。

全設定値の組み合わせを総当たりしない。

Flyway / Hibernateについてはproduction migrationを故意に壊すfixtureを作らず、実MySQL統合テストで正常起動・整合を確認する。

#### テスト重複の扱い

同じ業務仕様をすべての層で無意味に全面重複テストしない。

例：

```text
Service
→ 業務判断

Repository
→ DB query / constraint

Web / Security
→ HTTP境界

Browser
→ 実画面・JavaScript挙動
```

という責務分担を維持する。

---

### S-11の`from`による遷移元保持テスト

Controller / MockMvcテストおよび実ブラウザー受入確認では、次を確認する。

| ケース | 確認内容 |
|---|---|
| S-09 → S-11 | `from=in-progress`を付け、戻り先と現在地が制作中一覧になる |
| S-10 → S-11 | `from=completed`を付け、戻り先と現在地が完成一覧になる |
| status変更後 | 有効な`from`があれば、変更後statusにかかわらず遷移元を維持する |
| `from`なし | 現在の作品statusから戻り先・現在地を判定する |
| 不正`from` | エラーにせず`from`なしとして扱う |
| 基本情報保存後 | PRG後のGETへ有効な`from`を引き継ぐ |
| 自動保存 | 自動保存APIへ`from`を送信しない |

### Rate Limitの公開前検証

Rate Limitの物理実装場所は公開環境決定後に確定する。方式決定後、実装場所に応じたSecurity確認・自動テストまたはstagingでのintegration / scripted verificationを追加し、少なくとも次を確認する。

- Login username bucket：5回 / 5分
- Login IP bucket：20回 / 1分
- Register IP bucket：5回 / 1時間
- 成功試行もcountし、成功でresetしない
- CSRF不正試行はcountしない
- 429拒否はcountせず、windowを延長しない
- Login / Register bucketを分離する
- 形式不正usernameはshared invalid-username bucketへ入れる

---

## 15. 実装前・実装時・公開前の残事項

本章の残事項は、確定済みの機能仕様を変更するものではない。A級残件はなく、Knitting全体の実装開始を妨げる事項はない。

### A：実装開始前に全体として決定必須

なし。

### B：該当機能の実装前までに決定

なし。

### C：実装時に調整可能

確定仕様を変えない範囲で、次を実装時に調整できる。

- CSSの具体値、色、余白、px値、レスポンシブの具体的ブレークポイント
- HTMLタグの細かな構成、画像プレビューに使用するブラウザーAPI、具体的なアイコンや装飾
- private helper名、private methodの細かな分割、実装仕様を変えない内部クラス名
- submission tokenのSession内における具体的なデータ構造
- Custom Validatorの厳密なクラス名、各種result型・内部enumの細かな命名
- ImageIO / Thumbnailator / Java NIOの具体的API選択、multipart threshold等の細かな設定
- logger配置およびmessageの細かな文言
- test class名、test method名、fixture名、helper名、個別assertの記述方法

### D：公開環境決定後に決定

- production hosting
- TLS terminationおよびHTTP→HTTPS redirectの実装場所
- Forwarded Header、trusted proxy、client IP取得方式
- Rate Limitの物理実装場所および複数instance時の共有state
- production Secret ManagerおよびRemember-Me keyの保管方法
- production image storage、persistent disk / Object Storage、object storage credentials
- backup、capacity、object storage lifecycle、reverse proxy upload limit
- access log取得場所、dynamic port
- Secure Cookie、HSTSの最終確認、CSP最終directive
- production Security Integration Test

### E：将来拡張時に検討

- CI / GitHub ActionsおよびCI上のMySQL service構成
- Testcontainers
- Playwright等によるE2E自動化
- JaCoCo等によるcoverage管理
- 作品画像のクラウドストレージ移行・CDN利用
- 初期仕様にない追加API
- ユーザー退会、password変更、password reset、email verification、管理者role等の初期仕様外機能
- API / SPA / 別origin frontendを導入する場合の認証・CORS・SecurityFilterChain再設計

---

本書に記載する詳細設計01〜13は統合時点の確定仕様であり、A級残件はない。B〜Eは記載した時点で扱い、初期版の詳細設計未完了とはみなさない。
