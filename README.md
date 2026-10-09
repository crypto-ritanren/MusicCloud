# ☁️ MusicCloud — Android Google Drive Audio Player

<div align="center">

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?style=for-the-badge&logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack_Compose-2024.10.01-4285F4?style=for-the-badge&logo=jetpackcompose)
![Media3](https://img.shields.io/badge/AndroidX_Media3-1.5.1-red?style=for-the-badge&logo=google)
![Room](https://img.shields.io/badge/Room_DB-2.6.1-green?style=for-the-badge&logo=sqlite)
![Target SDK](https://img.shields.io/badge/Target_SDK-35-blue?style=for-the-badge&logo=android)
![Architecture](https://img.shields.io/badge/Concept-BYOK-orange?style=for-the-badge)

**Стриминг личной музыкальной коллекции прямо с Google Диска без посредников, серверов и подписок.**

[Быстрый старт](#-быстрый-старт) • [Архитектура и фичи](#-ключевые-особенности) • [Полный Walkthrough](WALKTHROUGH.md) • [Стек технологий](#-стек-технологий)

</div>

---

## 🎵 О проекте

**MusicCloud** — это легковесный, быстрый и стильный аудиоплеер для Android, спроектированный по модели **BYOK (Bring Your Own Key)**. Приложение подключается к вашему личному Google Диску через официальный Google Drive API v3, загружает аудиофайлы из заданной папки (по умолчанию `User Music`), парсит встроенные ID3-теги и обложки альбомов и воспроизводит музыку с локальным кэшированием.

Никакие логины, пароли или файлы не проходят через сторонние серверы — только прямое защищённое соединение между вашим устройством и инфраструктурой Google.

---

## 🌟 Ключевые особенности

- 🔑 **Bring Your Own Key (BYOK)**: Вы используете собственный OAuth Client ID из Google Cloud Console. Полный контроль над доступом и квотами.
- 📁 **Синхронизация выбранной папки (`User Music`)**: Сканирует целевую папку и рекурсивно все её поддиректории. Системные звуки, диктофонные записи и мелкий аудиомусор (`< 150 КБ`) автоматически отсекаются.
- 🏷️ **Умный парсинг ID3-тегов и обложек**: Извлекает метаданные (название, артист, альбом) и оригинальные JPEG/PNG обложки через легковесные HTTP Range-запросы первых 384 КБ без скачивания всего трека.
- 🧹 **Автоочистка от пиратских водяных знаков**: На лету удаляет метки, трек-номера и служебный шум.
- ⚡ **Resilient DNS**: Встроенный fallback на DNS Cloudflare (`1.1.1.1`) и Google (`8.8.8.8`) для обхода блокировок и сетевых сбоев интернет-провайдеров.
- 💿 **Стильный виниловый плеер**: Анимированная пластинка с текстурой дорожек, вращающаяся во время воспроизведения, индикатор буферизации, управление порядком треков и избранное.
- 📴 **Локальное кэширование (ExoPlayer SimpleCache)**: Прослушанные треки сохраняются в кэше устройства (до 256 МБ). Повторный запуск не расходует интернет-трафик.
- 🔔 **AndroidX Media3 Notification**: Полноценное управление из шторки уведомлений и экрана блокировки с прогресс-баром.

---

## 🛠️ Стек технологий

| Компонент | Технология |
|---|---|
| **Язык** | Kotlin 2.0.21, Coroutines & Flow |
| **UI** | Jetpack Compose + Material 3, Edge-to-Edge |
| **Аудио движок** | AndroidX Media3 1.5.1 (ExoPlayer + MediaSessionService + CacheDataSource) |
| **База данных** | Room 2.6.1 (SQLite) с миграцией схемы |
| **Сеть** | OkHttp 4.12, Retrofit 2.11, Gson |
| **Авторизация** | OAuth 2.0 PKCE + Chrome Custom Tabs + встроенный локальный сокет-сервер |
| **Изображения** | Coil 2.7.0 |
| **Минимальная версия** | Android 8.0 (API 26), Target: Android 15 (API 35) |

---

## 🚀 Быстрый старт

### 1. Подготовка Google Cloud
1. Перейдите в [Google Cloud Console](https://console.cloud.google.com/), залогиньтесь под своим аккаунтом и создайте проект с любым названием, например `MusicCloud`, нижние два поля оставьте как "No organization".

2. В верхней строке поиска найдите **Google Drive API** и включите его. Далее - найдите **OAuth consent screen** в левом меню или найдите через поиск - это панель управления доступом к вашим данным.

3. Настройте **OAuth consent screen**:
   - **App information**:
     - *App name*: любое (например, `MusicCloud`)
     - *User support email*: почта вашего аккаунта
   - **Audience**: выберите *External*
   - **Contact information**: почта вашего аккаунта
   - Нажмите **Save and Continue** (или **Create**).

4. Перейдите во вкладку **Data Access** (Scopes) и нажмите **Add or remove scopes**:
   - Найдите и отметьте галочкой: `https://www.googleapis.com/auth/drive.readonly`
   - Нажмите **Update** / **Save**.

5. Перейдите во вкладку **Audience** (Test users):
   - В блоке *Test users* нажмите **Add users** и укажите почту своего Google-аккаунта.

6. Перейдите во вкладку **Clients** (Credentials) и нажмите **Create client**:
   - Application type: **Web application**
   - В блоке **Authorized redirect URIs** нажмите *Add URI* и введите:
     ```text
     http://localhost:8888/callback
     ```
   - Нажмите **Create** / **Save**. Google отобразит 2 ключа:
     - **Client ID**
     - **Client Secret**

### 2. Подготовка Google Диска
Создайте в корне вашего Google Диска папку с именем **`User Music`** (или любым другим, по умолчанию в приложении стоит `User Music`) и загрузите туда свои аудиофайлы (`.mp3`, `.flac`, `.m4a`). Внутри можно свободно создавать вложенные подпапки исполнителей и альбомов.

### 3. Запуск приложения
1. Соберите APK через Android Studio или скачайте готовый `.apk` из вкладки **Releases**.
2. При первом запуске нажмите на иконку настроек (шестерёнка в правом верхнем углу).
3. Вставьте свои **Client ID** и **Client Secret**.
4. Нажмите **Сохранить**, затем **Войти через Google** и разрешите доступ к чтению аудиофайлов.
5. Нажмите кнопку **Синхронизировать** (облачко со стрелками) — приложение загрузит ваши треки, подтянет обложки и начнёт играть!

---

## 📖 Полное руководство

Подробный разбор архитектуры, решение проблемы 302-редиректов Google Drive, устройство резервного ID3v2-парсера и внутренняя структура описаны в [WALKTHROUGH.md](WALKTHROUGH.md).

---

## 📄 Лицензия

Проект распространяется под открытой лицензией [MIT License](LICENSE).
Вы можете свободно использовать, модифицировать и распространять код при сохранении авторства.

