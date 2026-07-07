#!/bin/bash
sed -i 's/<string name="err_backup_restricted">.*<\/string>/<string name="err_backup_restricted">لا يمكن استخراج حزمة هذا التطبيق بسبب قيود نظام أندرويد على هذا الجهاز.<\/string>/' app/src/main/res/values-ar/strings.xml
sed -i 's/<string name="pkg_copied">.*<\/string>/<string name="pkg_copied">تم نسخ اسم الحزمة!<\/string>/' app/src/main/res/values-ar/strings.xml

sed -i 's/<string name="err_backup_restricted">.*<\/string>/<string name="err_backup_restricted">Le package de cette application ne peut pas être extrait en raison des restrictions du système Android sur cet appareil.<\/string>/' app/src/main/res/values-fr/strings.xml
sed -i 's/<string name="pkg_copied">.*<\/string>/<string name="pkg_copied">Nom du package copié !<\/string>/' app/src/main/res/values-fr/strings.xml
