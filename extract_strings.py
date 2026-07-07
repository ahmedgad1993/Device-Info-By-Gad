import os
import re
import xml.etree.ElementTree as ET

kt_files = []
for root_dir, dirs, files in os.walk('app/src/main/java/com/deviceinfo/gad'):
    for file in files:
        if file.endswith('.kt'):
            kt_files.append(os.path.join(root_dir, file))

# Pattern to find Text("...") or Text(text = "...")
# Will only match simple strings, not templates with $
pattern = re.compile(r'Text\(\s*(?:text\s*=\s*)?"([^"$\\]+)"')

strings_to_add = {}

def make_key(text):
    s = re.sub(r'[^a-zA-Z0-9]', '_', text.lower())
    s = re.sub(r'_+', '_', s).strip('_')
    return 'ui_' + s[:20]

for kt_file in kt_files:
    with open(kt_file, 'r') as f:
        content = f.read()
    
    matches = pattern.findall(content)
    for text in matches:
        if len(text.strip()) > 0:
            key = make_key(text)
            strings_to_add[key] = text
            content = content.replace(f'Text("{text}"', f'Text(stringResource(R.string.{key})')
            content = content.replace(f'Text(text = "{text}"', f'Text(text = stringResource(R.string.{key})')
    
    with open(kt_file, 'w') as f:
        f.write(content)

# Add to strings.xml
tree = ET.parse('app/src/main/res/values/strings.xml')
root = tree.getroot()
existing_keys = set([item.attrib['name'] for item in root.findall('string')])

for key, val in strings_to_add.items():
    if key not in existing_keys:
        new_item = ET.Element('string', {'name': key})
        new_item.text = val
        new_item.tail = "\n    "
        root.append(new_item)
        existing_keys.add(key)

tree.write('app/src/main/res/values/strings.xml', encoding='utf-8', xml_declaration=True)

print(f"Extracted {len(strings_to_add)} strings.")
