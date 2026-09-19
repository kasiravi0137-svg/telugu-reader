"""
Patches the AndroidManifest.xml that `flutter create` generates so it has:
- SYSTEM_ALERT_WINDOW / FOREGROUND_SERVICE / POST_NOTIFICATIONS permissions
- the TeluguReaderAccessibilityService <service> declaration
"""
import re

MANIFEST_PATH = "android/app/src/main/AndroidManifest.xml"
STRINGS_PATH = "android/app/src/main/res/values/strings.xml"

PERMISSIONS = """    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
"""

SERVICE_BLOCK = """
        <service
            android:name=".TeluguReaderAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true"
            android:label="Telugu Reader">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
"""

def patch_manifest():
    with open(MANIFEST_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    if "SYSTEM_ALERT_WINDOW" not in content:
        content = re.sub(
            r"(<manifest[^>]*>)",
            r"\1\n" + PERMISSIONS,
            content,
            count=1,
        )

    if "TeluguReaderAccessibilityService" not in content:
        content = content.replace("</application>", SERVICE_BLOCK + "    </application>")

    with open(MANIFEST_PATH, "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched AndroidManifest.xml")

def patch_strings():
    desc = '    <string name="accessibility_service_description">Screen text chadavadaniki, floating bubble chupinchadaniki ee permission vaadutundi.</string>\n'
    try:
        with open(STRINGS_PATH, "r", encoding="utf-8") as f:
            content = f.read()
    except FileNotFoundError:
        content = "<resources>\n</resources>\n"

    if "accessibility_service_description" not in content:
        content = content.replace("</resources>", desc + "</resources>")

    with open(STRINGS_PATH, "w", encoding="utf-8") as f:
        f.write(content)
    print("Patched strings.xml")

if __name__ == "__main__":
    patch_manifest()
    patch_strings()
