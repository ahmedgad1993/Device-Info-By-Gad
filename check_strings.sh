grep -hro 'R.string.[a-zA-Z0-9_]*' app/src/main/java | sort | uniq | sed 's/R.string.//' > used_strings.txt
for s in $(cat used_strings.txt); do 
  grep -q "\"$s\"" app/src/main/res/values/strings.xml || echo "Missing in en: $s"
  grep -q "\"$s\"" app/src/main/res/values-ar/strings.xml || echo "Missing in ar: $s"
done
