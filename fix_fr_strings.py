import xml.etree.ElementTree as ET

tree_base = ET.parse('app/src/main/res/values/strings.xml')
tree_fr = ET.parse('app/src/main/res/values-fr/strings.xml')

root_base = tree_base.getroot()
root_fr = tree_fr.getroot()

fr_keys = set([item.attrib['name'] for item in root_fr.findall('string')])

for item in root_base.findall('string'):
    key = item.attrib['name']
    if key not in fr_keys:
        new_item = ET.Element('string', {'name': key})
        # Simple translation strategy: just use English for now or basic translation
        text = item.text
        if text == "Yes": new_item.text = "Oui"
        elif text == "No": new_item.text = "Non"
        elif text == "System": new_item.text = "Système"
        elif text == "OS": new_item.text = "OS"
        elif text == "Free": new_item.text = "Libre"
        elif text == "Used": new_item.text = "Utilisé"
        else: new_item.text = text
        
        new_item.tail = "\n    "
        root_fr.append(new_item)

tree_fr.write('app/src/main/res/values-fr/strings.xml', encoding='utf-8', xml_declaration=True)
