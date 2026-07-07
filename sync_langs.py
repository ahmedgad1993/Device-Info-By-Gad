import xml.etree.ElementTree as ET

base_tree = ET.parse('app/src/main/res/values/strings.xml')
base_root = base_tree.getroot()

for lang in ['ar', 'fr']:
    path = f'app/src/main/res/values-{lang}/strings.xml'
    tree = ET.parse(path)
    root = tree.getroot()
    
    existing_keys = set([item.attrib['name'] for item in root.findall('string')])
    
    for base_item in base_root.findall('string'):
        key = base_item.attrib['name']
        if key not in existing_keys:
            new_item = ET.Element('string', {'name': key})
            new_item.text = base_item.text
            new_item.tail = "\n    "
            root.append(new_item)
            
    tree.write(path, encoding='utf-8', xml_declaration=True)

print("Synchronized translation files.")
