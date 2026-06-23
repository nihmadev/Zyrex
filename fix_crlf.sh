#!/bin/bash
set -euo pipefail

DRY_RUN=false
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=true

CONVERTED=0

echo "=== CRLF → LF конвертация (Zyrex) ==="
[[ "$DRY_RUN" == true ]] && echo "[DRY-RUN] Файлы не будут изменены"
echo ""

# Находим все файлы с нужными расширениями и проверяем каждый
find . \
  -type f \
  \( \
    -name "*.java" -o \
    -name "*.kt" -o \
    -name "*.gradle" -o \
    -name "*.properties" -o \
    -name "*.xml" -o \
    -name "*.json" -o \
    -name "*.txt" -o \
    -name "*.sh" \
  \) \
  -not -path "./.git/*" \
  -not -path "./run/*" \
  -not -path "./build/*" \
  -not -path "./.gradle/*" \
  -print0 | while IFS= read -r -d '' file; do
  
    # Проверяем наличие CRLF с помощью cat -A
    if cat -A "$file" | grep -q '\^M'; then
        echo "Найден CRLF: $file"
        
        if [[ "$DRY_RUN" == false ]]; then
            # Конвертируем
            tr -d '\r' < "$file" > "$file.tmp" && mv "$file.tmp" "$file"
            echo "  -> Конвертирован"
            ((CONVERTED++))
        else
            echo "  -> Будет конвертирован (dry-run)"
            ((CONVERTED++))
        fi
    fi
done

echo ""
echo "=== Готово ==="
echo "Обработано файлов с CRLF: $CONVERTED"