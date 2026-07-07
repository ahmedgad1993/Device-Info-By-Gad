import xml.etree.ElementTree as ET

def get_keys(path):
    tree = ET.parse(path)
    return set([item.attrib['name'] for item in tree.getroot().findall('string')])

base_keys = get_keys('app/src/main/res/values/strings.xml')
ar_keys = get_keys('app/src/main/res/values-ar/strings.xml')
fr_keys = get_keys('app/src/main/res/values-fr/strings.xml')

missing_in_ar = base_keys - ar_keys
missing_in_fr = base_keys - fr_keys

print(f"Missing in Arabic: {missing_in_ar}")
print(f"Missing in French: {missing_in_fr}")
