#!/bin/bash
for file in app/src/main/res/values*/strings.xml; do
  sed -i '/<\/resources>/d' "$file"
  echo '    <string name="err_backup_restricted">This app'"'"'s package cannot be extracted due to Android system restrictions on this device.</string>' >> "$file"
  echo '    <string name="pkg_copied">Package name copied!</string>' >> "$file"
  echo '</resources>' >> "$file"
done
