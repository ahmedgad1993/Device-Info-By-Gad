#!/bin/sh
grep -ho 'R\.string\.[a-zA-Z0-9_]*' app/src/main/java/com/deviceinfo/gad/*.kt | sort | uniq | sed 's/R.string.//' > expected.txt
while read -r val; do
  if ! grep -q "\"$val\"" app/src/main/res/values/strings.xml; then
    echo "English MISSING: $val"
  fi
  if ! grep -q "\"$val\"" app/src/main/res/values-ar/strings.xml; then
    echo "Arabic MISSING: $val"
  fi
done < expected.txt
