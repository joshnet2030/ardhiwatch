# Ardhi (Dokezo) Project Rules & Conventions

## 1. Architecture & Domain Models
- **Base Entity**: All JPA entities must extend `com.josh.ardhi.models.base.Auditable` and include all `Auditable` columns (`id`, `created_by_app_session_id`, `created_at`, `last_modified_by_app_session_id`, `last_modified_at`, `is_active`, `deactivation_memo`) in Flyway migrations (`src/main/resources/db/migration/`).
- **File Uploads**: All uploaded files must be represented by `com.josh.ardhi.models.SystemFile` (via `SystemFileService`) and stored on disk inside the directory configured by `${upload.dir}` in `application.properties`.

## 2. Error Handling (`AppException`)
- Use `com.josh.ardhi.exception.AppException` (checked exception) for domain and service-layer errors.
- Service interface and implementation methods that can fail must declare `throws AppException`.
- Annotate transactional service methods that throw `AppException` with `@Transactional(rollbackOn = AppException.class)`.
- In controllers, wrap service calls in `try / catch (AppException e)`, attach a user-friendly error message to the `Model` (e.g., `uploadFailure` or `message`), and return the HTMX fragment to display an inline error alert.

## 3. Forms, HTMX & Security
- **HTMX Form Handling**: Form processing endpoints use `@HxRequest` and `@PostMapping`, validate a `*Form` class (`@Valid @ModelAttribute` + `BindingResult`), and return a Thymeleaf fragment (`"path/to/template :: fragmentName"`).
- **CSRF Token**: Every `<form>` (both standard and HTMX) must include `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>`.
- **Security Config**: Whenever adding an unauthenticated GET or POST route, add the route pattern to `.permitAll()` in `com.josh.ardhi.security.SecurityConfig`.

## 4. UI & Styling Boundaries
- **Public, Legal, Auth & Client Portal** (`templates/layouts/public.html`, `templates/layouts/auth.html`, `templates/layouts/client.html`, `templates/auth/**`, `templates/client/**`): Use the custom dark-navy and emerald `dk-*` design system defined in `src/main/resources/static/css/styles.css`. Do not use Bootstrap classes on these pages.
- **Admin Portal** (`templates/layouts/admin.html`, `templates/admin/**`): Use Bootstrap 5 (`bootstrap.min.css`) components and utilities.
